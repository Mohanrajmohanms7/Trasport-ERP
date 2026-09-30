#!/usr/bin/env bash
# End-to-end API check of booking -> trip -> invoice -> approval rules. Runs in CI only.
set -u
API=http://localhost:8080/api/v1
export PGPASSWORD=ci_password
PSQL="psql -h localhost -U transport_admin -d transport_erp -tAc"
FAILS=0
pass() { echo "PASS $1"; }
fail() { echo "FAIL $1 :: $2"; FAILS=$((FAILS+1)); }
j() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$1"; }

for i in $(seq 1 15); do
  LR=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin@123"}')
  echo "$LR" | grep -q '"token"' && break; sleep 3
done
echo "login: $LR" | cut -c1-600
TOKEN=$(echo "$LR" | j "d['data']['token']")
[ -n "$TOKEN" ] && pass login || { fail login "no token"; grep -E "ERROR|Exception" -A3 /tmp/app.log | head -60; exit 1; }
H=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')
post() { curl -s -X POST "${H[@]}" "$API$1" -d "${2:-{\}}"; }
put()  { curl -s -X PUT  "${H[@]}" "$API$1" -d "$2"; }

$PSQL "UPDATE companies SET gst_number='33AAAAA0000A1Z5'; UPDATE branches SET gst_number=NULL;" >/dev/null

R=$(post /customers '{"code":"C-KA","name":"Karnataka Builders","gstNumber":"29BBBBB0000B1Z5","status":"ACTIVE"}'); echo "customer: $R" | cut -c1-300
CUST=$(echo "$R" | j "d['data']['id']")
R=$(post /customers '{"code":"C-TN","name":"Chennai Builders","gstNumber":"33CCCCC0000C1Z5","status":"ACTIVE"}')
CUST_TN=$(echo "$R" | j "d['data']['id']")
R=$(post /materials '{"code":"MSAND","name":"M-Sand","status":"ACTIVE"}'); echo "material: $R" | cut -c1-300
MAT=$(echo "$R" | j "d['data']['id']")

BK='{"customer":{"id":'$CUST'},"details":[{"material":{"id":'$MAT'},"quantity":10,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'
R=$(post /bookings "$BK"); echo "booking: $R" | cut -c1-300
BOOK=$(echo "$R" | j "d['data']['id']")
BNO=$(echo "$R" | j "d['data']['bookingNumber']")
[[ "$BNO" =~ ^BKG-[0-9]{4}/0000[0-9]$ ]] && pass "booking number $BNO" || fail "booking number" "$BNO"

R=$(post /trips '{"booking":{"id":'$BOOK'},"details":[{"material":{"id":'$MAT'},"quantity":6}]}')
echo "$R" | grep -q "Booking Not Approved" && pass "trip blocked on unapproved booking" || fail "trip on unapproved booking" "$(echo $R | cut -c1-300)"

post /bookings/$BOOK/approve >/dev/null
R=$(post /trips '{"booking":{"id":'$BOOK'},"details":[{"material":{"id":'$MAT'},"quantity":6}]}'); echo "trip: $R" | cut -c1-400
TRIP=$(echo "$R" | j "d['data']['id']")
TNO=$(echo "$R" | j "d['data']['tripNumber']")
RATE=$(echo "$R" | j "d['data']['details'][0]['rate']")
[[ "$TNO" =~ ^TR[A-Z]*-[0-9]{4}/00001$ ]] && pass "trip number $TNO" || fail "trip number" "$TNO"
[ "$RATE" = "900" ] || [ "$RATE" = "900.0" ] || [ "$RATE" = "900.00" ] && pass "trip rate copied from booking" || fail "trip rate" "$RATE"

R=$(post /trips '{"booking":{"id":'$BOOK'},"details":[{"material":{"id":'$MAT'},"quantity":6}]}')
echo "$R" | grep -q "Booking Quantity Exceeded" && pass "over-booking blocked" || fail "over-booking" "$(echo $R | cut -c1-300)"

R=$(post /trips/$TRIP/complete)
echo "$R" | grep -q "Invalid Trip Status" && pass "cannot complete a PLANNED trip" || fail "status transition" "$(echo $R | cut -c1-300)"
R=$(post /trips/$TRIP/dispatch)
echo "$R" | grep -q "Vehicle And Driver Required" && pass "dispatch needs vehicle+driver" || fail "dispatch unassigned" "$(echo $R | cut -c1-300)"

# Skip fleet setup: move the trip to COMPLETED directly, then record weighbridge values.
$PSQL "UPDATE trips SET status='COMPLETED' WHERE id=$TRIP" >/dev/null
R=$(put /trips/$TRIP '{"booking":{"id":'$BOOK'},"details":[{"material":{"id":'$MAT'},"quantity":6,"loadedQuantity":6,"deliveredQuantity":5.8}]}')
SHORT=$(echo "$R" | j "d['data']['details'][0]['shortageQuantity']")
[ "$SHORT" = "0.2" ] || [ "$SHORT" = "0.20" ] && pass "weighbridge shortage 0.2" || fail "weighbridge" "$(echo $R | cut -c1-300)"
R=$(put /trips/$TRIP '{"booking":{"id":'$BOOK'},"details":[{"material":{"id":'$MAT'},"quantity":6,"loadedQuantity":5,"deliveredQuantity":5.8}]}')
echo "$R" | grep -q "Delivered More Than Loaded" && pass "delivered > loaded blocked" || fail "delivered>loaded" "$(echo $R | cut -c1-300)"

R=$(post /invoices/from-trip/$TRIP); echo "invoice: $R" | cut -c1-600
INV=$(echo "$R" | j "d['data']['id']")
INO=$(echo "$R" | j "d['data']['invoiceNumber']")
NET=$(echo "$R" | j "d['data']['netAmount']")
TAX=$(echo "$R" | j "d['data']['taxAmount']")
ST=$(echo "$R" | j "d['data']['supplyType']")
IG=$(echo "$R" | j "d['data']['details'][0]['igst']")
[[ "$INO" =~ ^INV-[0-9]{4}/00001$ ]] && pass "invoice number $INO" || fail "invoice number" "$INO"
# 5.8 t x (900 + 100) = 5800 taxable; IGST 5% = 290; total 6090
python3 -c "import sys; sys.exit(0 if abs(float('$NET')-6090)<0.001 and abs(float('$TAX')-290)<0.001 else 1)" && pass "delivered qty billed, total 6090" || fail "invoice total" "net=$NET tax=$TAX"
[ "$ST" = "INTER_STATE" ] && python3 -c "import sys; sys.exit(0 if abs(float('$IG')-290)<0.001 else 1)" && pass "IGST for inter-state" || fail "IGST" "type=$ST igst=$IG"

BS=$(curl -s "${H[@]}" $API/trips/$TRIP | j "d['data'].get('billingStatus')")
[ "$BS" = "INVOICE_DRAFT" ] && pass "trip billingStatus reaches API ($BS)" || fail "billingStatus" "$BS"
R=$(put /trips/$TRIP '{"booking":{"id":'$BOOK'},"details":[{"material":{"id":'$MAT'},"quantity":6}]}')
echo "$R" | grep -q "Trip Already Invoiced" && pass "invoiced trip locked" || fail "invoiced trip edit" "$(echo $R | cut -c1-300)"

R=$(post /invoices '{"customer":{"id":'$CUST'},"paymentTerms":"NET_30","discount":0,"details":[{"trip":{"id":'$TRIP'},"material":{"id":'$MAT'},"quantity":1,"rate":1,"freightCharges":0,"loadingCharges":0,"royalty":0,"gstPercentage":5}]}')
echo "$R" | grep -q "Trip Already Invoiced" && pass "double billing blocked" || fail "double billing" "$(echo $R | cut -c1-300)"

R=$(post /invoices/$INV/approve); echo "approve: $R" | cut -c1-300
INC=$($PSQL "SELECT COALESCE(SUM(amount),0) FROM journal_vouchers WHERE reference_number='$INO' AND description LIKE '%revenue%'")
GST=$($PSQL "SELECT COALESCE(SUM(amount),0) FROM journal_vouchers WHERE reference_number='$INO' AND description LIKE '%GST%'")
python3 -c "import sys; sys.exit(0 if abs(float('$INC')-5800)<0.001 and abs(float('$GST')-290)<0.001 else 1)" && pass "GL: income 5800, GST liability 290" || fail "GL posting" "income=$INC gst=$GST"
JVN=$($PSQL "SELECT string_agg(voucher_number, ',') FROM journal_vouchers WHERE reference_number='$INO'")
[[ "$JVN" =~ JV-[0-9]{4}/0000[0-9],JV-[0-9]{4}/0000[0-9] ]] && pass "JV numbers $JVN" || fail "JV numbers" "$JVN"

# Manual intra-state invoice with discount: 1 x 1000 = 1000, discount 100 -> 900 taxable, GST 18% = 162
R=$(post /invoices '{"customer":{"id":'$CUST_TN'},"paymentTerms":"NET_30","discount":100,"details":[{"material":{"id":'$MAT'},"quantity":1,"rate":1000,"freightCharges":0,"loadingCharges":0,"royalty":0,"gstPercentage":18}]}'); echo "manual: $R" | cut -c1-400
NET=$(echo "$R" | j "d['data']['netAmount']"); CG=$(echo "$R" | j "d['data']['details'][0]['cgst']"); INO2=$(echo "$R" | j "d['data']['invoiceNumber']")
python3 -c "import sys; sys.exit(0 if abs(float('$NET')-1062)<0.001 and abs(float('$CG')-81)<0.001 else 1)" && pass "discount before GST, CGST 81" || fail "manual discount" "net=$NET cgst=$CG"
[[ "$INO2" =~ /00002$ ]] && pass "invoice sequence continues $INO2" || fail "sequence" "$INO2"

FUT=$(date -d '+5 days' +%F)
R=$(post /invoices '{"customer":{"id":'$CUST_TN'},"invoiceDate":"'$FUT'","paymentTerms":"NET_30","discount":0,"details":[{"material":{"id":'$MAT'},"quantity":1,"rate":1,"freightCharges":0,"loadingCharges":0,"royalty":0,"gstPercentage":5}]}')
echo "$R" | grep -q "Date In Future" && pass "future invoice date blocked" || fail "future date" "$(echo $R | cut -c1-300)"


# ---------------- Driver daily slab payroll ----------------
PREV=$(date -d "$(date +%Y-%m-01) -1 day" +%Y-%m)
PY=${PREV%-*}; PM=$((10#${PREV#*-}))
R=$(post /drivers '{"code":"D001","name":"Kumar","licenseNumber":"TN0120260001","phoneNumber":"9000000001","status":"ACTIVE"}'); echo "driver: $R" | cut -c1-250
DRV=$(echo "$R" | j "d['data']['id']")
$PSQL "INSERT INTO trips (trip_number, trip_date, booking_id, driver_id, status, code, name, company_id, branch_id, created_date, updated_date, is_deleted, version) SELECT 'PT-'||g.n||'-'||g.k, to_date('$PREV-0'||g.n,'YYYY-MM-DD'), $BOOK, $DRV, 'COMPLETED', 'PT-'||g.n||'-'||g.k, 'pt', 1, 1, now(), now(), false, 0 FROM (VALUES (1,1),(2,1),(2,2),(3,1),(3,2),(3,3),(3,4)) AS g(n,k);
INSERT INTO trips (trip_number, trip_date, booking_id, driver_id, status, code, name, company_id, branch_id, created_date, updated_date, is_deleted, version) VALUES ('PT-X', to_date('$PREV-04','YYYY-MM-DD'), $BOOK, $DRV, 'CANCELLED', 'PT-X', 'pt', 1, 1, now(), now(), false, 0);" >/dev/null
R=$(put /driver-pay-slabs '[{"tripsFrom":1,"tripsTo":1,"dailyAmount":500},{"tripsFrom":2,"tripsTo":null,"dailyAmount":1000}]'); echo "slabs: $R" | cut -c1-200
echo "$R" | grep -q '"success":true' && pass "slabs saved" || fail "slabs" "$R"
R=$(put /driver-pay-slabs '[{"tripsFrom":1,"tripsTo":2,"dailyAmount":500},{"tripsFrom":2,"tripsTo":null,"dailyAmount":1000}]')
echo "$R" | grep -q "Invalid Pay Slab" && pass "overlapping slabs rejected" || fail "slab overlap" "$(echo $R | cut -c1-200)"
R=$(post /driver-advances '{"driver":{"id":'$DRV'},"amount":500,"paymentMethod":"CASH"}'); echo "advance: $R" | cut -c1-250
ANO=$(echo "$R" | j "d['data']['advanceNumber']")
[[ "$ANO" =~ ^ADV-[0-9]{4}/00001$ ]] && pass "advance issued $ANO" || fail "advance" "$ANO"
R=$(post /driver-payrolls/generate '{"driverId":'$DRV',"payYear":'$PY',"payMonth":'$PM',"advanceAdjustment":500,"basicSalary":99999}'); echo "payroll: $R" | cut -c1-500
PR=$(echo "$R" | j "d['data']['id']")
G=$(echo "$R" | j "d['data']['grossAmount']"); N=$(echo "$R" | j "d['data']['netSalaryPayable']")
TT=$(echo "$R" | j "d['data']['totalTrips']"); TD=$(echo "$R" | j "d['data']['tripDays']")
PNO=$(echo "$R" | j "d['data']['payrollNumber']")
python3 -c "import sys; sys.exit(0 if abs(float('$G')-2500)<0.001 and abs(float('$N')-2000)<0.001 else 1)" && pass "spec scenario gross 2500 net 2000 (trips $TT on $TD days)" || fail "payroll amounts" "gross=$G net=$N trips=$TT days=$TD"
[ "$TT" = "7" ] && [ "$TD" = "3" ] && pass "cancelled trip ignored, 7 trips / 3 days" || fail "trip counts" "$TT/$TD"
[[ "$PNO" =~ ^PAY-[0-9]{4}/00001$ ]] && pass "payroll number $PNO" || fail "payroll number" "$PNO"
R=$(post /driver-payrolls/generate '{"driverId":'$DRV',"payYear":'$PY',"payMonth":'$PM'}')
echo "$R" | grep -q "Duplicate Pay Period" && pass "duplicate period blocked" || fail "duplicate period" "$(echo $R | cut -c1-200)"
R=$(post /driver-payrolls/$PR/post)
echo "$R" | grep -q "Invalid Payroll Status" && pass "cannot post a DRAFT" || fail "post draft" "$(echo $R | cut -c1-200)"
post /driver-payrolls/$PR/approve >/dev/null
R=$(post /driver-payrolls/$PR/post); echo "post: $R" | cut -c1-200
post /driver-payrolls/$PR/post >/dev/null
EXP=$($PSQL "SELECT count(*)||':'||COALESCE(sum(amount),0) FROM journal_vouchers j JOIN chart_of_accounts d ON d.id=j.debit_account_id JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE j.reference_number LIKE 'SAL-%-$PR' AND d.account_code='5150' AND c.account_code='2050'")
REC=$($PSQL "SELECT count(*)||':'||COALESCE(sum(amount),0) FROM journal_vouchers j JOIN chart_of_accounts d ON d.id=j.debit_account_id JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE j.reference_number LIKE 'SAL-%-$PR' AND d.account_code='2050' AND c.account_code='1150'")
[ "$EXP" = "1:2500.00" ] && pass "one salary expense JV 2500 (after double post)" || fail "expense JV" "$EXP"
[ "$REC" = "1:500.00" ] && pass "one advance recovery JV 500" || fail "recovery JV" "$REC"
OUT=$(curl -s "${H[@]}" $API/driver-advances/driver/$DRV/outstanding | j "d['data']")
python3 -c "import sys; sys.exit(0 if abs(float('$OUT'))<0.001 else 1)" && pass "advance fully recovered" || fail "advance outstanding" "$OUT"
R=$(post /driver-payrolls/$PR/pay '{"paymentMethod":"BANK_TRANSFER","paymentReference":"UTR123"}'); echo "pay: $R" | cut -c1-200
post /driver-payrolls/$PR/pay '{"paymentMethod":"CASH"}' >/dev/null
PAYJ=$($PSQL "SELECT count(*)||':'||COALESCE(sum(amount),0) FROM journal_vouchers j JOIN chart_of_accounts d ON d.id=j.debit_account_id WHERE j.reference_number LIKE 'SAL-%-$PR' AND d.account_code='2050' AND j.reference_number LIKE 'SAL-PAY-%'")
EXP2=$($PSQL "SELECT count(*) FROM journal_vouchers j JOIN chart_of_accounts d ON d.id=j.debit_account_id WHERE j.reference_number LIKE 'SAL-%-$PR' AND d.account_code='5150'")
[ "$PAYJ" = "1:2000.00" ] && [ "$EXP2" = "1" ] && pass "one payment JV 2000, no second expense (after double pay)" || fail "payment JV" "pay=$PAYJ expense=$EXP2"
PAYABLE=$($PSQL "SELECT COALESCE(sum(CASE WHEN c.account_code='2050' THEN j.amount ELSE 0 END),0) - COALESCE(sum(CASE WHEN d.account_code='2050' THEN j.amount ELSE 0 END),0) FROM journal_vouchers j JOIN chart_of_accounts d ON d.id=j.debit_account_id JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE j.reference_number LIKE 'SAL-%-$PR'")
[ "$PAYABLE" = "0.00" ] && pass "salary payable fully settled (balance 0)" || fail "payable balance" "$PAYABLE"
SLIP=$(curl -s "${H[@]}" $API/driver-payrolls/$PR/print | j "len(d['data']['days'])")
[ "$SLIP" = "3" ] && pass "salary slip has 3 daily rows" || fail "slip days" "$SLIP"
PDF=$(curl -s -o /tmp/slip.pdf -w "%{http_code}:%{content_type}" "${H[@]}" $API/driver-payrolls/$PR/pdf)
[[ "$PDF" == 200:application/pdf* ]] && pass "salary slip PDF" || fail "pdf" "$PDF"
R=$(post /driver-payrolls/$PR/cancel); echo "cancel: $R" | cut -c1-200
OUT=$(curl -s "${H[@]}" $API/driver-advances/driver/$DRV/outstanding | j "d['data']")
python3 -c "import sys; sys.exit(0 if abs(float('$OUT')-500)<0.001 else 1)" && pass "cancel returns advance to outstanding" || fail "cancel advance" "$OUT"
R=$(post /driver-payrolls/generate '{"driverId":'$DRV',"payYear":'$PY',"payMonth":'$PM'}')
echo "$R" | grep -q '"success":true' && pass "month can be regenerated after cancel" || fail "regenerate" "$(echo $R | cut -c1-200)"


# ---------------- Security & business guards ----------------
tok() { curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}" | j "d['data']['token']"; }
as() { local T=$1; shift; local M=$1; shift; curl -s -o /tmp/r.json -w "%{http_code}" -X $M -H "Authorization: Bearer $T" -H 'Content-Type: application/json' "$API$1" ${2:+-d "$2"}; }
RID() { post /roles "{\"code\":\"$1\",\"name\":\"$1\",\"status\":\"ACTIVE\"}" | j "d['data']['id']"; }
R_DRV=$(RID DRIVER); R_OP=$(RID OPERATOR); R_VW=$(RID VIEWER); R_CA=$(RID COMPANY_ADMIN)
SA_ROLE=$($PSQL "SELECT id FROM app_roles WHERE code='SUPER_ADMIN' LIMIT 1")
mkuser() { post /users "{\"username\":\"$1\",\"name\":\"$1\",\"code\":\"$1\",\"password\":\"Secret@123\",\"status\":\"ACTIVE\",\"roles\":[{\"id\":$2}]}" | j "d['success']"; }
mkuser drv1 $R_DRV >/dev/null; mkuser op1 $R_OP >/dev/null; mkuser vw1 $R_VW >/dev/null; mkuser ca1 $R_CA >/dev/null
TD=$(tok drv1 Secret@123); TO=$(tok op1 Secret@123); TV=$(tok vw1 Secret@123); TC=$(tok ca1 Secret@123)
[ -n "$TD" ] && [ -n "$TO" ] && [ -n "$TV" ] && [ -n "$TC" ] && pass "test users can log in" || fail "test users" "$TD|$TO|$TV|$TC"
C=$(as "$TD" GET /invoices); [ "$C" = "403" ] && pass "driver login cannot read invoices" || fail "driver invoices" "$C"
C=$(as "$TD" GET /maintenance-requests); [ "$C" != "403" ] && pass "driver login reaches maintenance requests ($C)" || fail "driver maintenance" "$C $(cut -c1-150 /tmp/r.json)"
C=$(as "$TD" GET /vehicles); [ "$C" = "200" ] && pass "driver login can list vehicles" || fail "driver vehicles" "$C"
C=$(as "$TV" POST /customers '{"code":"VX","name":"Viewer Try"}'); [ "$C" = "403" ] && pass "viewer cannot create" || fail "viewer write" "$C"
C=$(as "$TV" GET /customers); [ "$C" = "200" ] && pass "viewer can read" || fail "viewer read" "$C"
C=$(as "$TO" POST /roles '{"code":"SUPER_ADMIN","name":"x"}'); [ "$C" = "403" ] && pass "operator cannot create roles" || fail "operator role" "$C"
OPID=$($PSQL "SELECT id FROM app_users WHERE username='op1'")
C=$(as "$TO" PUT /users/$OPID "{\"name\":\"op1\",\"status\":\"ACTIVE\",\"roles\":[{\"id\":$SA_ROLE}]}"); [ "$C" = "403" ] && pass "operator cannot grant self SUPER_ADMIN" || fail "self escalation" "$C"
C=$(as "$TC" POST /roles '{"code":"super_admin","name":"sneaky"}'); grep -q "reserved" /tmp/r.json && pass "company admin cannot create SUPER_ADMIN role" || fail "reserved role" "$C $(cut -c1-150 /tmp/r.json)"
CAID=$($PSQL "SELECT id FROM app_users WHERE username='ca1'")
C=$(as "$TC" PUT /users/$CAID "{\"name\":\"ca1\",\"status\":\"ACTIVE\",\"roles\":[{\"id\":$SA_ROLE}]}"); [ "$C" != "200" ] && pass "company admin cannot grant SUPER_ADMIN ($C)" || fail "ca escalation" "$C"
C=$(as "$TO" POST /journal '{"amount":1}'); [ "$C" = "403" ] && pass "operator cannot post manual journals" || fail "operator journal" "$C"
R=$(post /bookings/$BOOK/reject); echo "$R" | grep -q "Booking Has Trips" && pass "booking with trips cannot be rejected" || fail "reject w/ trips" "$(echo $R | cut -c1-200)"
R=$(put /bookings/$BOOK '{"details":[{"material":{"id":'$MAT'},"quantity":1,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}')
echo "$R" | grep -q "Quantity Below Delivered" && pass "booking qty cannot drop below moved" || fail "booking qty guard" "$(echo $R | cut -c1-200)"
FUT=$(date -d '+3 days' +%F)
R=$(post /expenses '{"expenseDate":"'$FUT'","category":"OFFICE","amount":100,"totalAmount":100,"paymentMethod":"CASH","description":"future"}'); EX=$(echo "$R" | j "d['data']['id']")
echo "$R" | grep -q "Expense Date In Future" && pass "future-dated expense rejected" || fail "future expense" "$(echo $R | cut -c1-200)"
PAST=$(date -d '-2 days' +%F)
R=$(post /expenses '{"expenseDate":"'$PAST'","category":"OFFICE","amount":100,"totalAmount":100,"paymentMethod":"CASH","description":"past"}'); EX=$(echo "$R" | j "d['data']['id']"); ENO=$(echo "$R" | j "d['data']['expenseNumber']")
post /expenses/$EX/approve >/dev/null
JD=$($PSQL "SELECT voucher_date FROM journal_vouchers WHERE reference_number='$ENO' LIMIT 1")
[ "$JD" = "$PAST" ] && pass "back-dated expense JV dated $JD" || fail "expense JV date" "$JD vs $PAST"


# ---------------- Approvals, booking completion, bata ----------------
$PSQL "INSERT INTO app_settings (key_name, value_data, company_id, branch_id, description, is_deleted, version, code, name, status, created_by, created_date, updated_date) VALUES ('REQUIRE_SEPARATE_APPROVER','true',1,1,'maker-checker',false,0,'REQUIRE_SEPARATE_APPROVER','Separate approver','ACTIVE','ci',now(),now())" >/dev/null
R=$(post /expenses '{"category":"OFFICE","amount":50,"totalAmount":50,"paymentMethod":"CASH","description":"mc"}'); MX=$(echo "$R" | j "d['data']['id']")
R=$(post /expenses/$MX/approve); echo "$R" | grep -q "Second Person Must Approve" && pass "creator cannot approve own expense when enabled" || fail "maker-checker" "$(echo $R | cut -c1-200)"
C=$(as "$TO" POST /expenses/$MX/approve); [ "$C" = "200" ] && pass "another user can approve" || fail "second approver" "$C $(cut -c1-200 /tmp/r.json)"
$PSQL "UPDATE app_settings SET value_data='false' WHERE key_name='REQUIRE_SEPARATE_APPROVER'" >/dev/null

R=$(post /bookings '{"customer":{"id":'$CUST'},"details":[{"material":{"id":'$MAT'},"quantity":5,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'); B2=$(echo "$R" | j "d['data']['id']")
post /bookings/$B2/approve >/dev/null
R=$(post /trips '{"booking":{"id":'$B2'},"details":[{"material":{"id":'$MAT'},"quantity":5}]}'); T2=$(echo "$R" | j "d['data']['id']")
$PSQL "UPDATE trips SET status='DISPATCHED' WHERE id=$T2" >/dev/null
post /trips/$T2/complete >/dev/null
BS=$(curl -s "${H[@]}" $API/bookings/$B2 | j "d['data']['status']")
[ "$BS" = "COMPLETED" ] && pass "booking auto-completed when fully delivered" || fail "auto complete" "$BS"
R=$(post /trips '{"booking":{"id":'$B2'},"details":[{"material":{"id":'$MAT'},"quantity":1}]}'); echo "$R" | grep -q "Booking Not Approved" && pass "no trips on a completed booking" || fail "trip on completed" "$(echo $R | cut -c1-160)"
R=$(post /bookings '{"customer":{"id":'$CUST'},"details":[{"material":{"id":'$MAT'},"quantity":50,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'); B3=$(echo "$R" | j "d['data']['id']")
post /bookings/$B3/approve >/dev/null
R=$(post /bookings/$B3/close); S3=$(echo "$R" | j "d['data']['status']"); [ "$S3" = "COMPLETED" ] && pass "booking can be closed early" || fail "close booking" "$(echo $R | cut -c1-160)"

R=$(post /expenses '{"expenseDate":"'$PREV'-10","category":"DRIVER_BATA","driver":{"id":'$DRV'},"amount":300,"totalAmount":300,"paymentMethod":"CASH","description":"bata"}'); BX=$(echo "$R" | j "d['data']['id']")
post /expenses/$BX/approve >/dev/null
PR2=$($PSQL "SELECT id FROM driver_payrolls WHERE status='DRAFT' ORDER BY id DESC LIMIT 1")
R=$(post /driver-payrolls/$PR2/recalculate); BP=$(echo "$R" | j "d['data']['bataPaid']"); NP=$(echo "$R" | j "d['data']['netSalaryPayable']")
python3 -c "import sys; sys.exit(0 if abs(float('$BP')-300)<0.001 else 1)" && pass "bata 300 shown on payroll (net unchanged $NP)" || fail "bata" "$(echo $R | cut -c1-200)"


# ---------------- Exports, attachments, photos ----------------
for m in vehicles trips invoices work-orders stock journal; do
  for f in xlsx pdf; do
    CT=$(curl -s -o /tmp/e.bin -w "%{http_code}:%{content_type}" "${H[@]}" "$API/exports/$m?format=$f"); SZ=$(stat -c%s /tmp/e.bin)
    [[ "$CT" == 200:* ]] && [ "$SZ" -gt 500 ] && pass "export $m.$f ($SZ bytes)" || fail "export $m.$f" "$CT $SZ $(head -c 200 /tmp/e.bin)"
  done
done
C=$(as "$TO" GET "/exports/invoices?format=xlsx"); [ "$C" = "403" ] && pass "operator cannot export invoices" || fail "finance export guard" "$C"
printf '%%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%%%EOF\n' > /tmp/doc.pdf
R=$(curl -s -H "Authorization: Bearer $TOKEN" -F entityType=INVOICE -F entityId=$INV -F category=SIGNED_COPY -F "file=@/tmp/doc.pdf;type=application/pdf" $API/attachments); echo "attach: $R" | cut -c1-250
AF=$(echo "$R" | j "d['data']['fileName']")
N=$(curl -s -H "Authorization: Bearer $TOKEN" "$API/attachments?entityType=INVOICE&entityId=$INV" | j "len(d['data'])")
[ "$N" = "1" ] && pass "invoice has 1 attachment" || fail "attachment list" "$N"
DL=$(curl -s -o /tmp/dl.bin -w "%{http_code}:%{content_type}" -H "Authorization: Bearer $TOKEN" $API/files/download/$AF)
[[ "$DL" == 200:application/pdf* ]] && cmp -s /tmp/dl.bin /tmp/doc.pdf && pass "attachment downloads intact from DB" || fail "download" "$DL"
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TD" -F entityType=INVOICE -F entityId=$INV -F "file=@/tmp/doc.pdf;type=application/pdf" $API/attachments)
[ "$C" = "403" ] && pass "driver cannot attach to invoices" || fail "driver attach" "$C"
python3 -c "import struct,zlib;w=h=2;raw=b''.join(b'\x00'+b'\xff\x00\x00'*w for _ in range(h));c=lambda t,d:struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff);open('/tmp/p.png','wb').write(b'\x89PNG\r\n\x1a\n'+c(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+c(b'IDAT',zlib.compress(raw))+c(b'IEND',b''))"
VID=$($PSQL "SELECT id FROM vehicles ORDER BY id LIMIT 1"); [ -z "$VID" ] && VID=0
R=$(curl -s -H "Authorization: Bearer $TOKEN" -F "file=@/tmp/p.png;type=image/png" $API/photos/drivers/$DRV); PF=$(echo "$R" | j "d['data']['photoFile']")
PH=$($PSQL "SELECT photo_file FROM drivers WHERE id=$DRV")
[ -n "$PF" ] && [ "$PH" = "$PF" ] && pass "driver photo saved" || fail "driver photo" "$(echo $R | cut -c1-200)"
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" -F "file=@/tmp/doc.pdf;type=application/pdf" $API/photos/drivers/$DRV)
[ "$C" != "200" ] && pass "PDF rejected as photo ($C)" || fail "photo type check" "$C"
R=$(put /drivers/$DRV '{"code":"D001","name":"Kumar R","licenseNumber":"TN0120260001","phoneNumber":"9000000001","status":"ACTIVE"}')
PH2=$($PSQL "SELECT photo_file FROM drivers WHERE id=$DRV"); [ "$PH2" = "$PF" ] && pass "driver edit keeps photo" || fail "photo lost on edit" "$PH2"


# ---------------- Company short name + data export ----------------
C1=$(curl -s "${H[@]}" $API/platform-admin/clients/1); echo "client1: $C1" | cut -c1-200
BODY=$(echo "$C1" | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];d['shortName']='pkc';print(json.dumps(d))")
R=$(put /platform-admin/companies/1 "$BODY"); echo "upd: $R" | cut -c1-200
SN=$($PSQL "SELECT short_name FROM companies WHERE id=1"); [ "$SN" = "PKC" ] && pass "short name saved upper-case (PKC)" || fail "short name" "$SN"
BAD=$(echo "$C1" | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];d['shortName']='P.K.C!';print(json.dumps(d))")
R=$(put /platform-admin/companies/1 "$BAD"); echo "$R" | grep -q "Invalid Short Name" && pass "invalid short name rejected" || fail "short name validation" "$(echo $R | cut -c1-160)"
L=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"ca1","password":"Secret@123"}' | j "d['data'].get('companyShortName')")
[ "$L" = "PKC" ] && pass "login returns company initials" || fail "login brand" "$L"
B=$(curl -s -H "Authorization: Bearer $TC" $API/auth/tenant-brand | j "d['data'].get('companyShortName')"); [ "$B" = "PKC" ] && pass "tenant-brand endpoint" || fail "tenant-brand" "$B"
CT=$(curl -s -o /tmp/z.zip -w "%{http_code}" "${H[@]}" $API/platform-admin/companies/1/data-export); N=$(python3 -c "import zipfile;print(len(zipfile.ZipFile('/tmp/z.zip').namelist()))" 2>/dev/null)
[ "$CT" = "200" ] && [ "$N" = "18" ] && pass "company data export ZIP (18 files)" || fail "data export" "$CT $N"
C=$(as "$TC" GET /platform-admin/companies/1/data-export); [ "$C" = "403" ] && pass "company admin cannot use platform export" || fail "platform guard" "$C"


# ---------------- Maintenance & inventory end-to-end (company admin) ----------------
api() { local M=$1 P=$2 B=${3:-}; curl -s -X $M -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' "$API$P" ${B:+-d "$B"}; }
UOM=$(api GET /spare-parts/uoms | j "d['data'][0]['id']")
R=$(api POST /warehouses '{"code":"WH-MAIN","name":"Main Store","status":"ACTIVE","branchId":1}'); echo "wh: $R" | cut -c1-220; W1=$(echo "$R" | j "d['data']['id']")
R=$(api POST /spare-parts '{"code":"FLT-OIL","name":"Oil Filter","defaultUomId":'$UOM',"defaultRate":450,"reorderLevel":5,"status":"ACTIVE"}'); echo "sp: $R" | cut -c1-220; SP=$(echo "$R" | j "d['data']['id']")
[ -n "$W1" ] && [ -n "$SP" ] && pass "warehouse + spare part created" || fail "inv masters" "W1=$W1 SP=$SP UOM=$UOM"
R=$(api PUT /spare-parts/$SP '{"code":"FLT-OIL","name":"Oil Filter (Tata)","defaultUomId":'$UOM',"defaultRate":460,"reorderLevel":5,"status":"ACTIVE"}'); echo "$R" | grep -q "Oil Filter (Tata)" && pass "spare part can be edited" || fail "part edit" "$(echo $R | cut -c1-160)"
R=$(api POST /inventory/stock/opening-balance '{"warehouseId":'$W1',"sparePartId":'$SP',"quantity":10,"unitRate":400}'); echo "opening: $R" | cut -c1-220
R=$(api POST /inventory/stock/receipt '{"warehouseId":'$W1',"sparePartId":'$SP',"quantity":10,"unitRate":500,"referenceNumber":"BILL-77"}'); echo "receipt: $R" | cut -c1-220
ST=$(api GET "/inventory/stock?warehouseId=$W1&size=50" | j "[ (r['availableQuantity'], r.get('averageCost'), r.get('stockValue')) for r in d['data']['content'] if r.get('sparePartId')==$SP][0]")
echo "stock: $ST"; python3 -c "q,a,v=$ST; import sys; sys.exit(0 if abs(float(q)-20)<1e-6 and abs(float(a)-450)<1e-6 and abs(float(v)-9000)<1e-6 else 1)" && pass "stock 20 @ avg 450 = 9000" || fail "avg cost" "$ST"
INVJ=$($PSQL "SELECT COALESCE(SUM(CASE WHEN d.account_code='1200' THEN j.amount ELSE 0 END),0) FROM journal_vouchers j JOIN chart_of_accounts d ON d.id=j.debit_account_id WHERE j.reference_number LIKE 'STK-%'")
[ "$INVJ" = "9000.00" ] && pass "inventory account debited 9000 (opening 4000 + receipt 5000)" || fail "stock JVs" "$INVJ"
R=$(api POST /vehicles '{"code":"TN01AB1234","name":"TN01AB1234","status":"ACTIVE"}'); echo "vehicle: $R" | cut -c1-200; VH=$(echo "$R" | j "d['data']['id']" 2>/dev/null)
if [ -z "$VH" ] || [ "$VH" = "None" ]; then $PSQL "INSERT INTO vehicles (code,name,status,company_id,branch_id,created_date,updated_date,is_deleted,version) VALUES ('TN01AB1234','TN01AB1234','ACTIVE',1,1,now(),now(),false,0)" >/dev/null; VH=$($PSQL "SELECT id FROM vehicles WHERE code='TN01AB1234'"); fi
echo "VH=$VH"
MR=$(api POST /maintenance-requests '{"vehicleId":'$VH',"title":"Brake noise","description":"front","priority":"HIGH"}'); echo "mr: $MR" | cut -c1-300; MRID=$(echo "$MR" | j "d['data']['id']")
api POST /maintenance-requests/$MRID/review '{}' >/dev/null; api POST /maintenance-requests/$MRID/approve >/dev/null
R=$(api POST /maintenance-requests/$MRID/convert '{}'); echo "convert: $R" | cut -c1-200; WOID=$(echo "$R" | j "d['data'].get('workOrderId')")
[ -n "$WOID" ] && [ "$WOID" != "None" ] && pass "request converted to work order $WOID" || { WOID=$(echo "$(api POST /work-orders '{"vehicleId":'$VH',"source":"MANUAL","maintenanceType":"REPAIR","name":"Brake job","priority":"HIGH"}')" | j "d['data']['id']"); fail "convert" "$(echo $R | cut -c1-160)"; }
R=$(api POST /work-orders/$WOID/parts '{"sparePartId":'$SP',"quantity":4,"unitRate":450}'); L1=$(echo "$R" | j "d['data']['parts'][0]['id']"); echo "part line $L1"
api POST /work-orders/$WOID/labour '{"description":"Mechanic","hours":2,"rate":300}' >/dev/null
R=$(api POST /inventory/stock/issue '{"warehouseId":'$W1',"workOrderId":'$WOID',"workOrderPartId":'$L1',"quantity":4}'); echo "issue: $R" | cut -c1-200
R=$(api POST /inventory/stock/issue '{"warehouseId":'$W1',"workOrderId":'$WOID',"workOrderPartId":'$L1',"quantity":1}'); echo "$R" | grep -qi "exceeds" && pass "cannot issue more than the line quantity" || fail "over-issue" "$(echo $R | cut -c1-150)"
R=$(api POST /inventory/stock/return '{"warehouseId":'$W1',"workOrderId":'$WOID',"workOrderPartId":'$L1',"quantity":1}'); echo "return: $R" | cut -c1-160
R=$(api POST /inventory/stock/issue '{"warehouseId":'$W1',"workOrderId":'$WOID',"workOrderPartId":'$L1',"quantity":1}'); echo "$R" | grep -q '"success":true' && pass "returned part can be issued again" || fail "re-issue after return" "$(echo $R | cut -c1-150)"
AV=$(api GET "/inventory/stock?warehouseId=$W1&size=50" | j "[r['availableQuantity'] for r in d['data']['content'] if r.get('sparePartId')==$SP][0]")
python3 -c "import sys; sys.exit(0 if abs(float('$AV')-16)<1e-6 else 1)" && pass "stock 20 - 4 + 1 - 1 = 16" || fail "stock after moves" "$AV"
R=$(api POST /work-orders/$WOID/cancel '{"cancellationReason":"test"}'); echo "$R" | grep -q "Issued Parts Not Returned" && pass "cannot cancel work order holding stock" || fail "cancel with issued" "$(echo $R | cut -c1-160)"
api POST /work-orders/$WOID/start '{}' >/dev/null
PC=$(api GET /work-orders/$WOID | j "d['data'].get('partsCostFromStock')"); python3 -c "import sys; sys.exit(0 if abs(float('$PC')-1800)<1e-6 else 1)" && pass "parts cost from stock 4 x 450 = 1800" || fail "parts cost" "$PC"
R=$(api POST /work-orders/$WOID/complete '{"completionNotes":"pads","actualCost":1000}'); echo "$R" | grep -q "Actual Cost Too Low" && pass "actual cost below parts cost rejected" || fail "cost floor" "$(echo $R | cut -c1-160)"
R=$(api POST /work-orders/$WOID/complete '{"completionNotes":"pads replaced","actualCost":2400}'); echo "complete: $R" | cut -c1-200
P1=$($PSQL "SELECT COALESCE(SUM(amount),0) FROM journal_vouchers WHERE reference_number LIKE 'MAINT-PARTS-%'")
P2=$($PSQL "SELECT COALESCE(SUM(j.amount),0) FROM journal_vouchers j JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE j.reference_number LIKE 'MAINT-WO%' AND j.reference_number NOT LIKE 'MAINT-PARTS-%' AND c.account_code='2000'")
[ "$P1" = "1800.00" ] && [ "$P2" = "600.00" ] && pass "completion: 1800 inventory->expense, 600 payable" || fail "completion JVs" "parts=$P1 payable=$P2"
MRS=$(api GET /maintenance-requests/$MRID | j "d['data'].get('workOrderStatus')"); [ "$MRS" = "COMPLETED" ] && pass "request shows work order COMPLETED" || fail "request WO status" "$MRS"
MR2=$(api POST /maintenance-requests '{"vehicleId":'$VH',"title":"Horn","description":"x","priority":"LOW"}' | j "d['data']['id']")
api POST /maintenance-requests/$MR2/review '{}' >/dev/null; api POST /maintenance-requests/$MR2/approve >/dev/null
R=$(api POST /maintenance-requests/$MR2/cancel '{"cancellationReason":"fixed on road"}'); echo "$R" | grep -q '"CANCELLED"' && pass "approved request can be cancelled" || fail "cancel approved" "$(echo $R | cut -c1-160)"
TX=$(api GET "/inventory/transactions?size=50" | j "len(d['data']['content'])"); [ "$TX" -ge 5 ] && pass "inventory transactions listed ($TX)" || fail "txn list" "$TX"
for m in work-orders maintenance-requests spare-parts stock inventory-transactions warehouses; do
  for f in xlsx pdf; do
    CT=$(curl -s -o /tmp/e.bin -w "%{http_code}" -H "Authorization: Bearer $TC" "$API/exports/$m?format=$f"); SZ=$(stat -c%s /tmp/e.bin)
    [ "$CT" = "200" ] && [ "$SZ" -gt 500 ] && pass "export with data $m.$f ($SZ)" || fail "export $m.$f" "$CT $SZ $(head -c 300 /tmp/e.bin)"
  done
done


# ---------------- Receipt payment mode + initial cost ----------------
SUPP=$($PSQL "INSERT INTO suppliers (code,name,status,company_id,branch_id,created_date,updated_date,is_deleted,version) VALUES ('SUP-1','Sri Auto Spares','ACTIVE',1,1,now(),now(),false,0) RETURNING id" | head -1)
R=$(api POST /spare-parts '{"code":"AIR-FLT","name":"Air Filter","defaultUomId":'$UOM',"defaultRate":300,"status":"ACTIVE"}'); SP3=$(echo "$R" | j "d['data']['id']")
R=$(api POST /inventory/stock/receipt '{"warehouseId":'$W1',"sparePartId":'$SP3',"quantity":2,"unitRate":300,"paymentMode":"CREDIT"}'); echo "$R" | grep -q "supplier" && pass "credit purchase needs a supplier" || fail "credit supplier" "$(echo $R | cut -c1-160)"
R=$(api POST /inventory/stock/receipt '{"warehouseId":'$W1',"sparePartId":'$SP3',"quantity":2,"unitRate":300,"paymentMode":"CASH"}'); TID=$(echo "$R" | j "d['data']['transactionId']" 2>/dev/null || echo)
CR=$($PSQL "SELECT c.account_code FROM journal_vouchers j JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE j.reference_number LIKE 'STK-RCPT-%' ORDER BY j.id DESC LIMIT 1")
[ "$CR" = "1000" ] && pass "cash purchase credits Cash, not payable" || fail "cash receipt" "$CR $(echo $R | cut -c1-160)"
R=$(api POST /inventory/stock/receipt '{"warehouseId":'$W1',"sparePartId":'$SP3',"quantity":1,"unitRate":320,"paymentMode":"CREDIT","supplierId":'$SUPP'}')
CR=$($PSQL "SELECT c.account_code FROM journal_vouchers j JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE j.reference_number LIKE 'STK-RCPT-%' ORDER BY j.id DESC LIMIT 1")
[ "$CR" = "2000" ] && pass "credit purchase with supplier credits Accounts Payable" || fail "credit receipt" "$CR $(echo $R | cut -c1-160)"
R=$(api POST /spare-parts '{"code":"OLD-BELT","name":"Fan Belt","defaultUomId":'$UOM',"status":"ACTIVE"}'); SP4=$(echo "$R" | j "d['data']['id']")
api POST /inventory/stock/opening-balance '{"warehouseId":'$W1',"sparePartId":'$SP4',"quantity":6}' >/dev/null
SID=$(api GET "/inventory/stock?warehouseId=$W1&size=50" | j "[r['id'] for r in d['data']['content'] if r.get('sparePartId')==$SP4][0]")
R=$(api POST /inventory/stock/$SID/initial-cost '{"unitCost":250}'); AVG=$(echo "$R" | j "d['data'].get('averageCost')")
VJ=$($PSQL "SELECT amount FROM journal_vouchers WHERE reference_number='STK-VAL-$SID'")
python3 -c "import sys; sys.exit(0 if abs(float('$AVG')-250)<1e-6 else 1)" && [ "$VJ" = "1500.00" ] && pass "unvalued stock costed once: 6 x 250 = 1500 posted" || fail "initial cost" "$AVG $VJ $(echo $R | cut -c1-160)"
R=$(api POST /inventory/stock/$SID/initial-cost '{"unitCost":300}'); echo "$R" | grep -qi "already has a cost" && pass "initial cost only once" || fail "initial cost twice" "$(echo $R | cut -c1-160)"


# ---------------- Reports hub ----------------
RF=$(date -d "$(date +%Y-%m-01) -3 month" +%F); RT=$(date +%F)
KEYS=$(curl -s "${H[@]}" $API/report-hub/catalog | j "' '.join(r['key'] for r in d['data'])")
NK=$(echo $KEYS | wc -w); [ "$NK" -ge 35 ] && pass "report catalog has $NK reports" || fail "catalog" "$NK"
for k in $KEYS; do
  R=$(curl -s "${H[@]}" "$API/report-hub/run/$k?from=$RF&to=$RT"); OK=$(echo "$R" | j "d['success']" 2>/dev/null)
  [ "$OK" = "True" ] && pass "report $k ($(echo "$R" | j "d['data']['rowCount']") rows)" || fail "report $k" "$(echo $R | cut -c1-220)"
  for f in xlsx pdf; do
    CT=$(curl -s -o /tmp/r.bin -w "%{http_code}" "${H[@]}" "$API/report-hub/export/$k?format=$f&from=$RF&to=$RT"); SZ=$(stat -c%s /tmp/r.bin)
    [ "$CT" = "200" ] && [ "$SZ" -gt 400 ] || fail "export $k.$f" "$CT $SZ $(head -c 200 /tmp/r.bin)"
  done
done
T1=$(curl -s "${H[@]}" "$API/report-hub/run/sales-register?from=$RF&to=$RT" | j "d['data']['totals']['taxable']")
D1=$($PSQL "SELECT COALESCE(SUM(taxable_amount),0) FROM sales_invoices WHERE company_id=1 AND is_deleted=false AND status NOT IN ('DRAFT','CANCELLED') AND invoice_date BETWEEN '$RF' AND '$RT'")
python3 -c "import sys; sys.exit(0 if abs(float('$T1')-float('$D1'))<0.01 else 1)" && pass "sales register taxable total = DB ($D1)" || fail "sales total" "$T1 vs $D1"
T2=$(curl -s "${H[@]}" "$API/report-hub/run/stock-valuation" | j "d['data']['totals']['value']")
D2=$($PSQL "SELECT COALESCE(ROUND(SUM(available_quantity*average_cost),2),0) FROM warehouse_stock WHERE company_id=1 AND is_deleted=false")
python3 -c "import sys; sys.exit(0 if abs(float('$T2')-float('$D2'))<0.05 else 1)" && pass "stock valuation total = DB ($D2)" || fail "stock value" "$T2 vs $D2"
T3=$(curl -s "${H[@]}" "$API/report-hub/run/unbilled-trips?from=$(date -d "-2 year" +%F)&to=$RT" | j "d['data']['rowCount']")
D3=$($PSQL "SELECT COUNT(*) FROM trips t JOIN trip_details td ON td.trip_id=t.id AND td.is_deleted=false WHERE t.company_id=1 AND t.is_deleted=false AND t.status='COMPLETED' AND NOT EXISTS (SELECT 1 FROM sales_invoice_details sd JOIN sales_invoices si ON si.id=sd.invoice_id WHERE sd.trip_id=t.id AND sd.is_deleted=false AND si.is_deleted=false AND si.status<>'CANCELLED')")
[ "$T3" = "$D3" ] && pass "unbilled trips = DB ($D3)" || fail "unbilled" "$T3 vs $D3"
K=$(curl -s "${H[@]}" "$API/report-hub/run/management-kpis?from=$RF&to=$RT" | j "len(d['data']['rows'])"); [ "$K" -ge 19 ] && pass "$K management KPIs" || fail "kpis" "$K"
C=$(as "$TO" GET "/report-hub/run/sales-register?from=$RF&to=$RT"); [ "$C" = "403" ] && pass "operator cannot run finance report" || fail "finance guard" "$C"
C=$(as "$TO" GET "/report-hub/run/trip-register?from=$RF&to=$RT"); [ "$C" = "200" ] && pass "operator can run trip register" || fail "ops report" "$C"
C=$(as "$TD" GET "/report-hub/catalog"); [ "$C" = "403" ] && pass "driver login cannot open reports" || fail "driver reports" "$C"
R=$(curl -s "${H[@]}" "$API/report-hub/run/trip-register?from=2026-05-01&to=2026-04-01"); echo "$R" | grep -q "Invalid Period" && pass "bad period rejected" || fail "period" "$(echo $R | cut -c1-120)"


# ---------------- Payables & multi-trip invoicing ----------------
AB=$($PSQL "SELECT status||':'||total_amount FROM supplier_bills WHERE source_type='STOCK_RECEIPT' ORDER BY id DESC LIMIT 1")
[ "$AB" = "APPROVED:320.00" ] && pass "credit stock receipt created an approved supplier bill (320)" || fail "auto bill" "$AB"
R=$(api POST /payables/bills '{"supplier":{"id":'$SUPP'},"supplierBillNo":"SA-991","billDate":"'$(date +%F)'","category":"REPAIR","taxableAmount":1000,"gstAmount":180}'); echo "bill: $R" | cut -c1-200
BID=$(echo "$R" | j "d['data']['id']"); BNO=$(echo "$R" | j "d['data']['billNumber']")
R=$(api POST /payables/bills/$BID/approve); echo "$R" | grep -q '"APPROVED"' && pass "manual bill approved ($BNO)" || fail "bill approve" "$(echo $R | cut -c1-200)"
AP=$($PSQL "SELECT COALESCE(SUM(j.amount),0) FROM journal_vouchers j JOIN chart_of_accounts c ON c.id=j.credit_account_id WHERE c.account_code='2000' AND c.company_id=1 AND j.reference_number LIKE 'SB-POST-$BID%'")
[ "$AP" = "1180.00" ] && pass "bill posted 1180 to Accounts Payable" || fail "bill JV" "$AP"
DUE=$(api GET /payables/suppliers/$SUPP/open-bills | j "sum(float(b['totalAmount'])-float(b['paidAmount']) for b in d['data'])")
R=$(api POST /payables/payments '{"supplierId":'$SUPP',"paymentDate":"'$(date +%F)'","amount":1000,"paymentMethod":"BANK_TRANSFER","referenceNumber":"UTR9"}'); echo "pay: $R" | cut -c1-200; PID=$(echo "$R" | j "d['data']['id']")
DUE2=$(api GET /payables/suppliers/$SUPP/open-bills | j "sum(float(b['totalAmount'])-float(b['paidAmount']) for b in d['data'])")
python3 -c "import sys; sys.exit(0 if abs(float('$DUE')-float('$DUE2')-1000)<0.01 else 1)" && pass "payment reduced supplier due by 1000 ($DUE -> $DUE2)" || fail "payment alloc" "$DUE -> $DUE2"
OLDEST=$($PSQL "SELECT payment_status FROM supplier_bills WHERE source_type='STOCK_RECEIPT' ORDER BY id DESC LIMIT 1"); [ "$OLDEST" = "PAID" ] && pass "oldest bill paid first" || fail "fifo" "$OLDEST"
R=$(api POST /payables/payments/$PID/cancel); DUE3=$(api GET /payables/suppliers/$SUPP/open-bills | j "sum(float(b['totalAmount'])-float(b['paidAmount']) for b in d['data'])")
python3 -c "import sys; sys.exit(0 if abs(float('$DUE3')-float('$DUE'))<0.01 else 1)" && pass "cancelled payment re-opens bills" || fail "payment cancel" "$DUE3 $(echo $R | cut -c1-150)"
C=$(as "$TO" POST /payables/payments '{"supplierId":'$SUPP',"amount":1}'); [ "$C" = "403" ] && pass "operator cannot pay suppliers" || fail "pay guard" "$C"
R=$(post /bookings '{"customer":{"id":'$CUST'},"details":[{"material":{"id":'$MAT'},"quantity":20,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'); B4=$(echo "$R" | j "d['data']['id']"); post /bookings/$B4/approve >/dev/null
TA=$(post /trips '{"booking":{"id":'$B4'},"details":[{"material":{"id":'$MAT'},"quantity":2}]}' | j "d['data']['id']")
TB=$(post /trips '{"booking":{"id":'$B4'},"details":[{"material":{"id":'$MAT'},"quantity":3}]}' | j "d['data']['id']")
$PSQL "UPDATE trips SET status='COMPLETED' WHERE id IN ($TA,$TB)" >/dev/null
R=$(post /invoices/from-trips '{"tripIds":['$TA','$TB']}'); echo "multi: $R" | cut -c1-200
NL=$(echo "$R" | j "len(d['data']['details'])"); TX=$(echo "$R" | j "d['data']['taxableAmount']")
[ "$NL" = "2" ] && python3 -c "import sys; sys.exit(0 if abs(float('$TX')-5000)<0.01 else 1)" && pass "one invoice for 2 trips (5 t x 1000 = 5000 taxable)" || fail "multi-trip invoice" "$NL $TX"
R=$(post /invoices/from-trips '{"tripIds":['$TA']}'); echo "$R" | grep -qi "already" && pass "trip cannot be billed twice" || fail "double bill" "$(echo $R | cut -c1-150)"
RF=$(date -d "-1 month" +%F); RT=$(date +%F)
for k in supplier-outstanding supplier-bill-register supplier-payment-register trip-profitability unlinked-costs; do
  R=$(curl -s "${H[@]}" "$API/report-hub/run/$k?from=$RF&to=$RT"); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "report $k ok" || fail "report $k" "$(echo $R | cut -c1-200)"
done
SO=$(curl -s "${H[@]}" "$API/report-hub/run/supplier-outstanding?to=$RT" | j "d['data']['totals'].get('total') or d['data']['totals'].get('balance')")
python3 -c "import sys; sys.exit(0 if abs(float('$SO')-float('$DUE3'))<0.05 else 1)" && pass "supplier outstanding report = open bills ($SO)" || fail "supplier outstanding" "$SO vs $DUE3"


# ---------------- Supplier master & subscription notices ----------------
R=$(api POST /suppliers '{"code":"SUP-9","name":"Kaveri Tyres","gstNumber":"33ABCDE1234F1Z5","creditDays":45,"status":"ACTIVE"}'); CD=$(echo "$R" | j "d['data']['creditDays']")
[ "$CD" = "45" ] && pass "supplier saved with 45 credit days" || fail "supplier credit days" "$(echo $R | cut -c1-160)"
R=$(api POST /suppliers '{"code":"SUP-10","name":"Bad GST","gstNumber":"12345","status":"ACTIVE"}'); echo "$R" | grep -q "GSTIN" && pass "invalid supplier GSTIN rejected" || fail "gstin" "$(echo $R | cut -c1-160)"
R=$(api DELETE /suppliers/$SUPP); echo "$R" | grep -q "has bills" && pass "supplier with bills cannot be deleted" || fail "supplier delete guard" "$(echo $R | cut -c1-160)"
$PSQL "UPDATE companies SET subscription_end_date = CURRENT_DATE + 2, subscription_status='ACTIVE' WHERE id=1" >/dev/null
B=$(api GET /auth/tenant-brand); SOON=$(echo "$B" | j "d['data'].get('subscriptionExpiringSoon')"); DL=$(echo "$B" | j "d['data'].get('subscriptionDaysLeft')")
[ "$SOON" = "True" ] && [ "$DL" = "2" ] && pass "tenant warned: subscription ends in 2 days" || fail "expiry warning" "$SOON $DL"
EC=$(curl -s "${H[@]}" $API/auth/tenant-brand | j "len(d['data'].get('expiringClients') or [])"); [ "$EC" -ge 1 ] && pass "platform admin sees $EC expiring client(s)" || fail "platform expiring" "$EC"
$PSQL "UPDATE companies SET subscription_end_date = CURRENT_DATE - 1 WHERE id=1" >/dev/null
C=$(curl -s -o /tmp/x -w "%{http_code}" -H "Authorization: Bearer $TC" $API/customers); grep -q SUBSCRIPTION_EXPIRED /tmp/x && pass "expired tenant blocked from data ($C)" || fail "expired block" "$C $(head -c 120 /tmp/x)"
C=$(curl -s -o /tmp/x -w "%{http_code}" -H "Authorization: Bearer $TC" $API/auth/plans); [ "$C" = "200" ] && pass "expired admin can still load renewal plans" || fail "plans after expiry" "$C $(head -c 120 /tmp/x)"
L=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"op1","password":"Secret@123"}'); echo "$L" | grep -qi "expired" && pass "non-admin cannot log in after expiry" || fail "expired login" "$(echo $L | cut -c1-120)"
$PSQL "UPDATE companies SET subscription_end_date = CURRENT_DATE + 2 WHERE id=1" >/dev/null


# ---------------- New client onboarding -> first login ----------------
PLAN=$(curl -s $API/auth/plans -H "Authorization: Bearer $TOKEN" | j "d['data'][0]['id']")
R=$(post /platform-admin/onboard '{"name":"Velan Earth Movers","shortName":"VEM","ownerName":"Velan","phone":"9000011111","email":"velan@example.com","state":"Tamil Nadu","subscriptionPlanId":'$PLAN',"adminUsername":"velan.admin","adminPassword":"Velan@2026","sendWelcomeEmail":false}')
echo "onboard: $R" | cut -c1-250; NCID=$(echo "$R" | j "d['data']['companyId']")
LR=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"velan.admin","password":"Velan@2026"}'); NT=$(echo "$LR" | j "d['data']['token']")
[ -n "$NT" ] && [ "$NT" != "None" ] && pass "new client admin can log in" || fail "new client login" "$(echo $LR | cut -c1-200)"
ST=$(curl -s -H "Authorization: Bearer $NT" $API/setup/status | j "d['data']['setupCompleted']"); [ "$ST" = "True" ] && pass "onboarded client is marked set up (no wizard)" || fail "setup flag" "$ST"
CN=$(curl -s -H "Authorization: Bearer $NT" "$API/customers?size=50" | j "d['data']['totalElements']"); [ "$CN" = "0" ] && pass "new client sees no other tenant's customers" || fail "tenant isolation list" "$CN"
for pair in customers:$CUST materials:$MAT bookings:$BOOK trips:$TRIP invoices:$INV drivers:$DRV warehouses:$W1 spare-parts:$SP suppliers:$SUPP driver-payrolls:$PR payables/bills:$BID; do
  e=${pair%%:*}; id=${pair##*:}
  R=$(curl -s -H "Authorization: Bearer $NT" "$API/$e/$id"); OK=$(echo "$R" | j "d.get('success')" 2>/dev/null)
  [ "$OK" != "True" ] && pass "isolation: $e/$id hidden from other tenant" || fail "isolation $e" "$(echo $R | cut -c1-160)"
done
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $NT" $API/platform-admin/stats); [ "$C" = "403" ] && pass "client admin blocked from platform admin" || fail "platform guard" "$C"
FY=$($PSQL "SELECT COUNT(*) FROM financial_years WHERE company_id=$NCID AND is_deleted=false"); [ "$FY" -ge 1 ] && pass "client has a financial year" || fail "client FY" "$FY"
NS=$(curl -s -H "Authorization: Bearer $NT" $API/setup/status | j "d['data']['hasBusinessData']"); [ "$NS" = "False" ] && pass "new client has no business data yet but still skips the wizard" || fail "business data" "$NS"


# ---------------- Branch Master ----------------
S=$(curl -s -H "Authorization: Bearer $TC" "$API/branches/summary" | j "[ (r['branch_id'], r['vehicles']) for r in d['data'] ]"); echo "summary: $S"
echo "$S" | grep -q "(1, [1-9]" && pass "head office shows its vehicles in branch summary" || fail "branch summary" "$S"
NB0=$($PSQL "SELECT COUNT(*) FROM vehicles WHERE branch_id IS NULL")$($PSQL "SELECT COUNT(*) FROM drivers WHERE branch_id IS NULL"); [ "$NB0" = "00" ] && pass "no vehicle/driver without a branch" || fail "null branches" "$NB0"
R=$(curl -s -X POST -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' $API/branches -d '{"code":"YRD-'$RANDOM'","name":"Coimbatore Yard","gstNumber":"33BAD"}'); echo "$R" | grep -q "GSTIN" && pass "branch GSTIN validated" || fail "branch gstin" "$(echo $R | cut -c1-160)"
R=$(curl -s -X POST -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' $API/branches -d '{"code":"YRD-'$RANDOM'","name":"Coimbatore Yard","gstNumber":"33ABCDE1234F1Z5","manager":"Ravi","status":"ACTIVE","companyId":1}'); NB=$(echo "$R" | j "d['data']['id']")
[ -n "$NB" ] && [ "$NB" != "None" ] && pass "new branch created" || fail "branch create" "$(echo $R | cut -c1-160)"
R=$(curl -s -X DELETE -H "Authorization: Bearer $TC" $API/branches/1); echo "$R" | grep -q "Inactive instead" && pass "branch with vehicles/trips cannot be deleted" || fail "branch delete guard" "$(echo $R | cut -c1-160)"
R=$(curl -s -X PUT -H "Authorization: Bearer $NT" -H 'Content-Type: application/json' $API/branches/$NB -d '{"code":"X","name":"Hijack"}'); echo "$R" | grep -q '"success":true' && fail "cross-tenant branch edit" "$(echo $R | cut -c1-160)" || pass "other tenant cannot edit this branch"
R=$(curl -s -X DELETE -H "Authorization: Bearer $NT" $API/branches/$NB); N2=$($PSQL "SELECT is_deleted FROM branches WHERE id=$NB"); [ "$N2" = "f" ] && pass "other tenant cannot delete this branch" || fail "cross-tenant delete" "$N2"


# ---------------- Branch master & dropdown lists ----------------
S=$(api GET /branches/summary | j "len(d['data'])"); [ "$S" -ge 1 ] && pass "branch summary ($S branches)" || fail "branch summary" "$S"
R=$(api POST /branches '{"code":"YRD-'$RANDOM'","name":"Coimbatore Yard","gstNumber":"33ABCDE1234F1Z5","status":"ACTIVE"}'); NB=$(echo "$R" | j "d['data']['id']")
[ -n "$NB" ] && [ "$NB" != "None" ] && pass "branch created" || fail "branch create" "$(echo $R | cut -c1-160)"
R=$(api POST /branches '{"code":"BAD","name":"Bad GST","gstNumber":"1234","status":"ACTIVE"}'); echo "$R" | grep -q "GSTIN" && pass "invalid branch GSTIN rejected" || fail "branch gstin" "$(echo $R | cut -c1-160)"
R=$(api DELETE /branches/1); echo "$R" | grep -q "Inactive instead" && pass "branch in use cannot be deleted" || fail "branch delete guard" "$(echo $R | cut -c1-160)"
R=$(api DELETE /branches/$NB); echo "$R" | grep -q '"success":true' && pass "unused branch can be deleted" || fail "branch delete" "$(echo $R | cut -c1-160)"
C=$(as "$TO" POST /lookups '{"lookupType":"FUEL_TYPE","code":"X","name":"X"}'); [ "$C" = "403" ] && pass "operator cannot change dropdown lists" || fail "lookup guard" "$C"


# ---------------- Masters structure & search isolation ----------------
for q in "vehicles?search=TN01" "customers?search=Karnataka" "customers?search=C-KA" "drivers?search=D001" "materials?search=MSAND" "suppliers?search=SUP"; do
  N=$(curl -s -H "Authorization: Bearer $NT" "$API/$q&size=50" | j "d['data']['totalElements']")
  [ "$N" = "0" ] && pass "search isolation: $q returns nothing to another tenant" || fail "search isolation $q" "$N"
done
N=$(api GET "/customers?search=Karnataka&size=50" | j "d['data']['totalElements']"); [ "$N" -ge 1 ] && pass "own tenant search still finds customers ($N)" || fail "own search" "$N"
BS=$(api GET "/branches/summary" | j "len(d['data'])" 2>/dev/null); [ -n "$BS" ] && [ "$BS" -ge 1 ] && pass "branch master summary ($BS branch)" || fail "branch summary" "$BS"
R=$(api POST /branches '{"code":"BR-2","name":"Salem Yard","gstNumber":"BADGST","status":"ACTIVE"}'); echo "$R" | grep -qi "GSTIN\|gst" && pass "branch GSTIN validated" || fail "branch gst" "$(echo $R | cut -c1-150)"
R=$(api POST /branches '{"code":"BR-2","name":"Salem Yard","city":"Salem","status":"ACTIVE"}'); B2=$(echo "$R" | j "d['data']['id']")
R=$(api DELETE /branches/1); echo "$R" | grep -qi "Inactive\|in use\|has users" && pass "branch in use cannot be deleted" || fail "branch delete guard" "$(echo $R | cut -c1-150)"
R=$(api POST /vehicles '{"code":"TN02XY9999","name":"TN02XY9999","status":"ACTIVE"}'); VB=$(echo "$R" | j "d['data'].get('branchId')"); [ -n "$VB" ] && [ "$VB" != "None" ] && pass "new vehicle gets a branch ($VB)" || fail "vehicle branch" "$(echo $R | cut -c1-150)"


# ---------------- Masters restructure (API unchanged) ----------------
R=$(api POST /vehicles '{"code":"TN02CD5678","name":"Tipper 2","status":"ACTIVE","ownerType":"SELF"}'); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "vehicle created from Vehicles screen API" || fail "vehicle create" "$(echo $R | cut -c1-160)"
R=$(api POST /customers '{"code":"C-NEW","name":"New Site Builders","gstNumber":"33AAAAA1111A1Z5","creditLimit":50000,"status":"ACTIVE"}'); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "customer created from Customers screen API" || fail "customer create" "$(echo $R | cut -c1-160)"
C=$(as "$TO" POST /branches '{"code":"X","name":"X"}'); [ "$C" = "403" ] && pass "only admins can add branches" || fail "branch guard" "$C"
VB=$($PSQL "SELECT branch_id FROM vehicles WHERE code='TN02CD5678'"); [ -n "$VB" ] && pass "new vehicle gets a branch ($VB)" || fail "vehicle branch" "empty"
NB=$($PSQL "SELECT COUNT(*) FROM vehicles WHERE branch_id IS NULL AND is_deleted=false"); [ "$NB" = "0" ] && pass "no vehicle without a branch" || fail "vehicles no branch" "$NB"


# ---------------- Masters structure, Branch Master & search isolation ----------------
for q in "vehicles?search=TN01" "customers?search=C-KA" "customers?search=Karnataka" "drivers?search=D001" "materials?search=MSAND" "suppliers?search=SUP" "branches?search=HO"; do
  N=$(curl -s -H "Authorization: Bearer $NT" "$API/$q&size=50" | j "d['data']['totalElements']")
  [ "$N" = "0" ] || { [ "$q" = "branches?search=HO" ] && [ "$N" = "1" ]; } && pass "search isolation: $q ($N)" || fail "search isolation $q" "$N"
done
S=$(api GET /branches/summary | j "len(d['data'])"); [ "$S" -ge 1 ] && pass "branch summary ($S branches)" || fail "branch summary" "$S"
R=$(api POST /branches '{"code":"YRD-'$RANDOM'","name":"Coimbatore Yard","gstNumber":"33ABCDE1234F1Z5","manager":"Ravi","phone":"9000022222","status":"ACTIVE"}'); NB=$(echo "$R" | j "d['data']['id']")
[ -n "$NB" ] && [ "$NB" != "None" ] && pass "branch created ($NB)" || fail "branch create" "$(echo $R | cut -c1-200)"
R=$(api POST /branches '{"code":"BAD","name":"Bad GST","gstNumber":"123","status":"ACTIVE"}'); echo "$R" | grep -qi "gst" && [ "$(echo "$R" | j "d['success']")" = "False" ] && pass "invalid branch GSTIN rejected" || fail "branch gstin" "$(echo $R | cut -c1-200)"
HO=$($PSQL "SELECT MIN(id) FROM branches WHERE company_id=1 AND is_deleted=false")
R=$(api DELETE /branches/$HO); [ "$(echo "$R" | j "d['success']")" = "False" ] && pass "branch with data cannot be deleted" || fail "branch delete guard" "$(echo $R | cut -c1-200)"
R=$(api DELETE /branches/$NB); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "empty branch can be deleted" || fail "branch delete" "$(echo $R | cut -c1-200)"
NHO=$($PSQL "SELECT MIN(id) FROM branches WHERE company_id=$NCID")
R=$(curl -s -X DELETE -H "Authorization: Bearer $TC" $API/branches/$NHO); [ "$(echo "$R" | j "d['success']")" = "False" ] && pass "cannot delete another tenant's branch" || fail "cross-tenant branch delete" "$(echo $R | cut -c1-200)"
C=$(as "$TO" POST /lookups '{"lookupType":"FUEL_TYPE","code":"X","name":"X"}'); [ "$C" = "403" ] && pass "operator cannot change dropdown lists" || fail "lookup guard" "$C"
R=$(api POST /vehicles '{"code":"TN09ZZ0001","name":"TN09ZZ0001","status":"ACTIVE"}'); VB=$(echo "$R" | j "d['data'].get('branchId')"); [ -n "$VB" ] && [ "$VB" != "None" ] && pass "new vehicle gets a branch ($VB)" || fail "vehicle branch" "$(echo $R | cut -c1-200)"


# ---------------- Vehicle & customer CRUD ----------------
R=$(api POST /vehicles '{"code":"TN10CR0001","name":"Tipper TN10CR0001","status":"ACTIVE","brand":"Tata","ownerType":"SELF"}'); VID2=$(echo "$R" | j "d['data']['id']")
[ -n "$VID2" ] && [ "$VID2" != "None" ] && pass "vehicle create" || fail "vehicle create" "$(echo $R | cut -c1-200)"
R=$(api GET /vehicles/$VID2); [ "$(echo "$R" | j "d['data']['brand']")" = "Tata" ] && pass "vehicle view" || fail "vehicle view" "$(echo $R | cut -c1-160)"
R=$(api PUT /vehicles/$VID2 '{"code":"TN10CR0001","name":"Tipper TN10CR0001","status":"ACTIVE","brand":"Ashok Leyland","ownerType":"SELF"}'); [ "$(echo "$R" | j "d['data']['brand']")" = "Ashok Leyland" ] && pass "vehicle edit" || fail "vehicle edit" "$(echo $R | cut -c1-160)"
R=$(api DELETE /vehicles/$VID2); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "unused vehicle delete" || fail "vehicle delete" "$(echo $R | cut -c1-160)"
R=$(api POST /customers '{"code":"C-CRUD","name":"Sakthi Builders","gstNumber":"33AAACS1234K1Z9","creditLimit":200000,"status":"ACTIVE"}'); CID2=$(echo "$R" | j "d['data']['id']")
[ -n "$CID2" ] && [ "$CID2" != "None" ] && pass "customer create" || fail "customer create" "$(echo $R | cut -c1-200)"
R=$(api PUT /customers/$CID2 '{"code":"C-CRUD","name":"Sakthi Builders Pvt","gstNumber":"33AAACS1234K1Z9","creditLimit":250000,"status":"ACTIVE"}'); [ "$(echo "$R" | j "d['data']['name']")" = "Sakthi Builders Pvt" ] && pass "customer edit" || fail "customer edit" "$(echo $R | cut -c1-160)"
R=$(api DELETE /customers/$CUST); [ "$(echo "$R" | j "d['success']")" = "False" ] && pass "customer with bookings cannot be deleted" || fail "customer delete guard" "$(echo $R | cut -c1-160)"
R=$(api DELETE /customers/$CID2); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "unused customer delete" || fail "customer delete" "$(echo $R | cut -c1-160)"


# ---------------- One-person company (single Company Admin) ----------------
nt() { local M=$1 P=$2 B=${3:-}; curl -s -X $M -H "Authorization: Bearer $NT" -H 'Content-Type: application/json' "$API$P" ${B:+-d "$B"}; }
NU=$($PSQL "SELECT COUNT(*) FROM app_users WHERE company_id=$NCID AND is_deleted=false"); [ "$NU" = "1" ] && pass "new client has exactly one login" || fail "single login" "$NU"
$PSQL "INSERT INTO app_settings (key_name,value_data,company_id,branch_id,is_deleted,version,code,name,status,created_by,created_date,updated_date) VALUES ('REQUIRE_SEPARATE_APPROVER','true',$NCID,NULL,false,0,'RSA','Separate approver','ACTIVE','ci',now(),now()) ON CONFLICT DO NOTHING" >/dev/null
NC=$(nt POST /customers '{"code":"VC1","name":"Kovai Builders","gstNumber":"33AAACK1234K1Z9","status":"ACTIVE"}' | j "d['data']['id']")
NM=$(nt POST /materials '{"code":"JALLI","name":"Blue Metal 20mm","status":"ACTIVE"}' | j "d['data']['id']")
R=$(nt POST /bookings '{"customer":{"id":'$NC'},"details":[{"material":{"id":'$NM'},"quantity":10,"rate":800,"transportRate":300,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'); NBK=$(echo "$R" | j "d['data']['id']")
R=$(nt POST /bookings/$NBK/approve); [ "$(echo "$R" | j "d['data']['status']")" = "APPROVED" ] && pass "single admin approves own booking" || fail "own booking approve" "$(echo $R | cut -c1-160)"
NTR=$(nt POST /trips '{"booking":{"id":'$NBK'},"details":[{"material":{"id":'$NM'},"quantity":10}]}' | j "d['data']['id']")
$PSQL "UPDATE trips SET status='COMPLETED' WHERE id=$NTR" >/dev/null
R=$(nt POST /invoices/from-trips '{"tripIds":['$NTR']}'); NIV=$(echo "$R" | j "d['data']['id']")
R=$(nt POST /invoices/$NIV/approve); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "single admin approves own invoice (second-approver rule skipped)" || fail "own invoice approve" "$(echo $R | cut -c1-200)"
R=$(nt POST /expenses '{"category":"OFFICE","amount":250,"totalAmount":250,"paymentMethod":"CASH","description":"stationery"}'); NEX=$(echo "$R" | j "d['data']['id']")
R=$(nt POST /expenses/$NEX/approve); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "single admin approves own expense" || fail "own expense approve" "$(echo $R | cut -c1-200)"
R=$(curl -s -H "Authorization: Bearer $NT" "$API/report-hub/run/sales-register?from=$(date -d '-1 month' +%F)&to=$(date +%F)"); [ "$(echo "$R" | j "d['data']['rowCount']")" = "1" ] && pass "single admin runs finance reports (own data only)" || fail "own report" "$(echo $R | cut -c1-160)"
MEID=$($PSQL "SELECT id FROM app_users WHERE username='velan.admin'")
R=$(nt DELETE /users/$MEID); echo "$R" | grep -qi "own login\|only Company Admin" && pass "admin cannot delete own login" || fail "self delete" "$(echo $R | cut -c1-160)"
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $NT" $API/customers); [ "$C" = "200" ] && pass "admin still active after blocked delete" || fail "admin active" "$C"
R=$(post /expenses '{"category":"OFFICE","amount":10,"totalAmount":10,"paymentMethod":"CASH","description":"multi"}'); MX2=$(echo "$R" | j "d['data']['id']")
$PSQL "UPDATE app_settings SET value_data='true' WHERE key_name='REQUIRE_SEPARATE_APPROVER' AND company_id=1" >/dev/null
R=$(post /expenses/$MX2/approve); echo "$R" | grep -q "Second Person Must Approve" && pass "multi-user company still needs a second approver" || fail "multi-user maker-checker" "$(echo $R | cut -c1-160)"
$PSQL "UPDATE app_settings SET value_data='false' WHERE key_name='REQUIRE_SEPARATE_APPROVER' AND company_id=1" >/dev/null


# ================= PKC demo dataset =================
PLAN=$(curl -s "${H[@]}" $API/auth/plans | j "d['data'][0]['id']")
R=$(curl -s -X POST "${H[@]}" $API/platform-admin/onboard -d '{"name":"PKC Transport","shortName":"PKC","ownerName":"P. Kannan","phone":"9443012345","email":"office@pkctransport.in","state":"Tamil Nadu","gstNumber":"33AAKFP4521M1Z6","subscriptionPlanId":'$PLAN',"adminUsername":"pkc.admin","adminPassword":"Pkc@2026","sendWelcomeEmail":false}')
echo "onboard: $(echo $R | cut -c1-200)"
python3 ../.ci/seed_pkc_demo.py --url $API --username pkc.admin --password 'Pkc@2026' 2>&1 | tee /tmp/seed.log | tail -80
PT=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"pkc.admin","password":"Pkc@2026"}' | j "d['data']['token']")
RF=$(date -d '-90 day' +%F); RT=$(date +%F)
KEYS=$(curl -s -H "Authorization: Bearer $PT" $API/report-hub/catalog | j "' '.join(r['key'] for r in d['data'])")
EMPTY=""
for k in $KEYS; do
  N=$(curl -s -H "Authorization: Bearer $PT" "$API/report-hub/run/$k?from=$RF&to=$RT" | j "d['data']['rowCount'] if d.get('success') else 'ERR'")
  echo "REPORT $k = $N"; [ "$N" = "0" ] || [ "$N" = "ERR" ] && EMPTY="$EMPTY $k"
done
echo "EMPTY_REPORTS:$EMPTY"
export PGPASSWORD=ci_password
psql -h localhost -U transport_admin -d transport_erp -tAc "SELECT 'COUNTS', (SELECT COUNT(*) FROM customers c JOIN companies co ON co.id=c.company_id WHERE co.name='PKC Transport'), (SELECT COUNT(*) FROM trips t JOIN companies co ON co.id=t.company_id WHERE co.name='PKC Transport'), (SELECT COUNT(*) FROM sales_invoices s JOIN companies co ON co.id=s.company_id WHERE co.name='PKC Transport'), (SELECT COUNT(*) FROM journal_vouchers s JOIN companies co ON co.id=s.company_id WHERE co.name='PKC Transport'), (SELECT COUNT(*) FROM customers WHERE company_id<>(SELECT id FROM companies WHERE name='PKC Transport'))"


[ -z "$(echo $EMPTY | tr -d ' ')" ] && pass "PKC demo: every report has data" || fail "PKC demo empty reports" "$EMPTY"


# ---------------- Excel bulk upload ----------------
pip install -q openpyxl >/dev/null 2>&1 || pip3 install -q openpyxl --break-system-packages >/dev/null 2>&1
for m in customers vehicles drivers suppliers materials spare-parts opening-stock; do
  CT=$(curl -s -o /tmp/t.xlsx -w "%{http_code}" -H "Authorization: Bearer $TC" $API/bulk-import/$m/template); SZ=$(stat -c%s /tmp/t.xlsx)
  [ "$CT" = "200" ] && [ "$SZ" -gt 3000 ] && pass "template $m" || fail "template $m" "$CT $SZ"
done
python3 - <<'PY'
from openpyxl import Workbook
def book(path, headers, rows):
    wb=Workbook(); ws=wb.active; ws.append(headers)
    for r in rows: ws.append(r)
    wb.save(path)
book('/tmp/veh.xlsx',['Registration No *','Display Name *','Ownership','Insurance Valid Till','Brand'],[
 ['TN47BU0001','Tipper BU1','SELF','2027-03-31','Tata'],
 ['TN47BU0002','Tipper BU2','HIRED','31-12-2026','Eicher'],
 ['TN47BU0003','Tipper BU3','SELF','','Ashok Leyland'],
 ['TN47BU0004','Tipper BU4','SELF','2027-01-15','BharatBenz'],
 ['TN47BU0005','','OWNED','not a date','Tata']])
book('/tmp/cus.xlsx',['Customer Code *','Customer Name *','GSTIN','Phone','Credit Limit (₹)'],[
 ['BU-C1','Bulk Builders 1','33AAGFS1234K1Z5','9443100001',100000],
 ['BU-C1','Bulk Builders dup','','',0],
 ['BU-C3','Bulk Builders 3','12345','98',-5]])
book('/tmp/drv.xlsx',['Driver Code *','Driver Name *','Licence No *','Licence Valid Till'],[
 ['BU-D1','Bulk Driver 1','TN47 20200000001','2030-01-01'],
 ['BU-D2','Bulk Driver 2','TN47 20200000001','2030-01-01']])
book('/tmp/stk.xlsx',['Warehouse Code *','Part Code *','Quantity *','Unit Cost (₹)'],[['NOPE','XX-99',5,10]])
PY
R=$(curl -s -H "Authorization: Bearer $TC" -F "file=@/tmp/veh.xlsx" $API/bulk-import/vehicles/validate); echo "veh: $(echo $R | cut -c1-300)"
V=$(echo "$R" | j "(d['data']['total'], d['data']['valid'], d['data']['invalid'])"); [ "$V" = "(5, 4, 1)" ] && pass "vehicle upload preview: 5 rows, 4 valid, 1 invalid" || fail "vehicle preview" "$V"
ERR=$(echo "$R" | j "' | '.join(d['data']['rows'][4]['errors'])"); echo "row5 errors: $ERR"
echo "$ERR" | grep -q "Display Name is required" && echo "$ERR" | grep -q "Ownership must be one of" && echo "$ERR" | grep -q "must be a date" && pass "row errors explain each problem" || fail "row errors" "$ERR"
ALL=$(echo "$R" | python3 -c "import sys,json;d=json.load(sys.stdin);print(json.dumps({'rows':[r['values'] for r in d['data']['rows']]}))")
VALID=$(echo "$R" | python3 -c "import sys,json;d=json.load(sys.stdin);print(json.dumps({'rows':[r['values'] for r in d['data']['rows'] if not r['errors']]}))")
B=$($PSQL "SELECT COUNT(*) FROM vehicles WHERE code LIKE 'TN47BU%'")
R=$(curl -s -X POST -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' $API/bulk-import/vehicles/create -d "$ALL"); A=$($PSQL "SELECT COUNT(*) FROM vehicles WHERE code LIKE 'TN47BU%'")
echo "$R" | grep -q "Invalid Rows" && [ "$A" = "$B" ] && pass "create refused while an invalid row remains (nothing saved)" || fail "invalid create" "$A $(echo $R | cut -c1-160)"
R=$(curl -s -X POST -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' $API/bulk-import/vehicles/create -d "$VALID"); A=$($PSQL "SELECT COUNT(*) FROM vehicles WHERE code LIKE 'TN47BU%' AND is_deleted=false")
[ "$(echo "$R" | j "d['data']['created']")" = "4" ] && [ "$A" = "4" ] && pass "4 valid vehicles created" || fail "bulk create" "$A $(echo $R | cut -c1-200)"
R=$(curl -s -H "Authorization: Bearer $TC" -F "file=@/tmp/veh.xlsx" $API/bulk-import/vehicles/validate); N=$(echo "$R" | j "sum(1 for r in d['data']['rows'] if any('already exists' in e for e in r['errors']))")
[ "$N" = "4" ] && pass "re-upload flags the 4 existing vehicles" || fail "existing check" "$N"
R=$(curl -s -H "Authorization: Bearer $TC" -F "file=@/tmp/cus.xlsx" $API/bulk-import/customers/validate); E=$(echo "$R" | j "' | '.join(' ; '.join(r['errors']) for r in d['data']['rows'])"); echo "cus: $E"
echo "$E" | grep -q "Duplicate of row 2" && echo "$E" | grep -q "GSTIN must be 15" && echo "$E" | grep -q "cannot be negative" && pass "customer checks: duplicate in file, GSTIN, negative amount" || fail "customer checks" "$E"
R=$(curl -s -H "Authorization: Bearer $TC" -F "file=@/tmp/drv.xlsx" $API/bulk-import/drivers/validate); E=$(echo "$R" | j "' | '.join(' ; '.join(r['errors']) for r in d['data']['rows'])")
echo "$E" | grep -qi "duplicate of row 2" && pass "driver duplicate licence in file caught" || fail "driver dup" "$E"
R=$(curl -s -H "Authorization: Bearer $TC" -F "file=@/tmp/stk.xlsx" $API/bulk-import/opening-stock/validate); E=$(echo "$R" | j "' | '.join(' ; '.join(r['errors']) for r in d['data']['rows'])")
echo "$E" | grep -q "not an existing warehouse" && echo "$E" | grep -q "not an existing spare part" && pass "opening stock checks master values" || fail "stock checks" "$E"
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $NT" -F "file=@/tmp/veh.xlsx" $API/bulk-import/vehicles/validate); [ "$C" = "200" ] && pass "other tenant can upload (own scope)" || fail "tenant upload" "$C"
VID=$($PSQL "SELECT id FROM vehicles WHERE code='TN47BU0002'"); R=$(curl -s -H "Authorization: Bearer $TC" $API/vehicles/$VID)
[ "$(echo "$R" | j "d['data']['ownerType']")" = "HIRED" ] && [ "$(echo "$R" | j "d['data']['insuranceExpiryDate']")" = "2026-12-31" ] && pass "edit loads saved vehicle (DD-MM-YYYY date parsed)" || fail "vehicle edit load" "$(echo $R | cut -c1-200)"
cp /tmp/veh.xlsx ../.ci/veh.xlsx 2>/dev/null || true


# ---------------- Settlement dashboard, advances, credit limit, cross-tenant writes ----------------
D=$(curl -s "${H[@]}" "$API/receipts/dashboard"); echo "dash: $(echo $D | cut -c1-200)"
OK=$(echo "$D" | j "all(a['totalOutstanding']>=b['totalOutstanding'] for a,b in zip(d['data']['topOutstandingCustomers'],d['data']['topOutstandingCustomers'][1:]))"); [ "$OK" = "True" ] && pass "customers with dues sorted largest first" || fail "dues sort" "$OK"
OK=$(echo "$D" | j "all(c['totalOutstanding']>0 for c in d['data']['topOutstandingCustomers'])"); [ "$OK" = "True" ] && pass "only customers who owe are listed" || fail "owing filter" "$OK"
OK=$(echo "$D" | j "all(a['agingDays']>=b['agingDays'] for a,b in zip(d['data']['agingInvoices'],d['data']['agingInvoices'][1:]))"); [ "$OK" = "True" ] && pass "open invoices most overdue first" || fail "aging sort" "$OK"
TO_DUE=$(echo "$D" | j "d['data']['summary']['totalOutstanding']"); DB_DUE=$($PSQL "SELECT COALESCE(SUM(net_amount-paid_amount),0) FROM sales_invoices WHERE company_id=1 AND status='APPROVED' AND is_deleted=false")
python3 -c "import sys; sys.exit(0 if abs(float('$TO_DUE')-float('$DB_DUE'))<0.01 else 1)" && pass "dashboard dues = invoices (₹$DB_DUE)" || fail "dues total" "$TO_DUE vs $DB_DUE"
R0=$(curl -s "${H[@]}" "$API/receipts/dashboard?fromDate=2000-01-01&toDate=2000-01-31" | j "d['data']['summary']['totalReceived']"); [ "$(python3 -c "print(float('$R0'))")" = "0.0" ] && pass "period filter applies to received" || fail "period filter" "$R0"
# advance: receipt larger than the open invoice -> advance -> new invoice -> apply advance
R=$(post /bookings '{"customer":{"id":'$CUST'},"details":[{"material":{"id":'$MAT'},"quantity":10,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'); B9=$(echo "$R" | j "d['data']['id']"); post /bookings/$B9/approve >/dev/null
T9=$(post /trips '{"booking":{"id":'$B9'},"details":[{"material":{"id":'$MAT'},"quantity":2}]}' | j "d['data']['id']"); $PSQL "UPDATE trips SET status='COMPLETED' WHERE id=$T9" >/dev/null
I9=$(post /invoices/from-trips '{"tripIds":['$T9']}' | j "d['data']['id']"); post /invoices/$I9/approve >/dev/null
NET9=$($PSQL "SELECT net_amount FROM sales_invoices WHERE id=$I9")
R=$(post /receipts '{"customerId":'$CUST',"receiptDate":"'$(date +%F)'","amountReceived":5000,"advanceAmount":5000,"paymentMethod":"CASH","referenceNumber":"ADV-1","allocations":[]}'); RC=$(echo "$R" | j "d['data'].get('receiptId') or d['data'].get('id')"); echo "adv receipt: $(echo $R | cut -c1-160)"
post /receipts/$RC/approve >/dev/null
R=$(post /receipts/customers/$CUST/apply-advance); echo "apply: $(echo $R | cut -c1-200)"
PAIDX=$($PSQL "SELECT COALESCE(SUM(a.allocated_amount),0) FROM customer_receipt_allocations a WHERE a.receipt_id=$RC AND a.is_deleted=false")
OLDEST=$($PSQL "SELECT paid_amount>0 FROM sales_invoices WHERE customer_id=$CUST AND status='APPROVED' AND is_deleted=false ORDER BY invoice_date, id LIMIT 1")
python3 -c "import sys; sys.exit(0 if float('$PAIDX')>0 else 1)" && [ "$OLDEST" = "t" ] && pass "advance applied to the oldest open invoice first (₹$PAIDX)" || fail "apply advance" "$PAIDX $OLDEST"
LEFT=$($PSQL "SELECT advance_amount FROM customer_receipts WHERE id=$RC"); ALLOC=$($PSQL "SELECT COALESCE(SUM(allocated_amount),0) FROM customer_receipt_allocations WHERE receipt_id=$RC AND is_deleted=false")
python3 -c "import sys; sys.exit(0 if abs(float('$LEFT')+float('$ALLOC')-5000)<0.01 else 1)" && pass "receipt advance reduced by what was applied" || fail "advance left" "$LEFT + $ALLOC"
R=$(post /receipts/$RC/apply-advance); echo "$R" | grep -q '"success":false' && pass "cannot apply beyond open invoices / advance" || echo "note second apply: $(echo $R | cut -c1-120)"
# credit limit
$PSQL "UPDATE customers SET credit_limit=1000 WHERE id=$CUST" >/dev/null
R=$(post /bookings '{"customer":{"id":'$CUST'},"details":[{"material":{"id":'$MAT'},"quantity":10,"rate":900,"transportRate":100,"royaltyRate":0,"loadingCharge":0,"gstPercentage":5}]}'); BCL=$(echo "$R" | j "d['data']['id']")
R=$(post /bookings/$BCL/approve); echo "$R" | grep -q "Credit Limit Exceeded" && pass "booking over credit limit blocked" || fail "credit limit" "$(echo $R | cut -c1-160)"
$PSQL "UPDATE customers SET credit_limit=0 WHERE id=$CUST" >/dev/null
R=$(post /bookings/$BCL/approve); [ "$(echo "$R" | j "d['success']")" = "True" ] && pass "no limit set = no block" || fail "no limit" "$(echo $R | cut -c1-160)"
# another company must not be able to change company 1 records
X="$API"; WR=0
for spec in "POST invoices/$INV/cancel" "POST bookings/$BOOK/reject" "POST bookings/$B9/close" "POST trips/$TRIP/complete" "POST expenses/$MX2/approve" "POST receipts/$RC/cancel" "POST receipts/$RC/apply-advance" "POST driver-payrolls/$PR/cancel" "POST payables/bills/$BID/approve" "PUT customers/$CUST" "PUT vehicles/$VID2" "DELETE drivers/$DRV" "POST work-orders/$WOID/cancel" "POST maintenance-requests/$MRID/cancel"; do
  M=${spec%% *}; P=${spec#* }
  R=$(curl -s -X $M -H "Authorization: Bearer $NT" -H 'Content-Type: application/json' "$X/$P" -d '{"name":"hack","code":"HACK","status":"ACTIVE","cancellationReason":"x"}')
  S=$(echo "$R" | python3 -c "import sys,json
try: print(json.load(sys.stdin).get('success'))
except Exception: print('NOJSON')")
  [ "$S" = "True" ] && { fail "cross-tenant $spec" "$(echo $R | cut -c1-160)"; WR=1; }
done
[ "$WR" = "0" ] && pass "other company cannot change 14 kinds of company-1 records"


# ---------------- Temporary password, 403s, receipt PDF ----------------
PLAN2=$(curl -s "${H[@]}" $API/auth/plans | j "d['data'][0]['id']")
R=$(curl -s -X POST "${H[@]}" $API/platform-admin/onboard -d '{"name":"Temp Pwd Movers","shortName":"TPM","ownerName":"T","phone":"9000022222","email":"tpm@example.com","state":"Tamil Nadu","subscriptionPlanId":'$PLAN2',"adminUsername":"tpm.admin","sendWelcomeEmail":false}')
TMP=$(echo "$R" | j "d['data'].get('temporaryPassword') or d['data'].get('adminPassword') or ''"); echo "tmp pwd present: $([ -n "$TMP" ] && echo yes || echo no) $(echo $R | cut -c1-160)"
LR=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"tpm.admin\",\"password\":\"$TMP\"}"); FT=$(echo "$LR" | j "d['data']['token']"); FF=$(echo "$LR" | j "d['data'].get('forcePasswordChange')")
[ "$FF" = "True" ] && pass "generated password: login says change password" || fail "force flag" "$FF $(echo $LR | cut -c1-160)"
C=$(curl -s -o /tmp/x -w "%{http_code}" -H "Authorization: Bearer $FT" $API/customers); grep -q PASSWORD_CHANGE_REQUIRED /tmp/x && [ "$C" = "403" ] && pass "app blocked until the temporary password is changed" || fail "force block" "$C $(head -c 120 /tmp/x)"
R=$(curl -s -X PUT -H "Authorization: Bearer $FT" -H 'Content-Type: application/json' $API/auth/change-password -d "{\"oldPassword\":\"$TMP\",\"newPassword\":\"$TMP\"}"); echo "$R" | grep -qi "different" && pass "new password must differ" || fail "same pwd" "$(echo $R | cut -c1-160)"
R=$(curl -s -X PUT -H "Authorization: Bearer $FT" -H 'Content-Type: application/json' $API/auth/change-password -d "{\"oldPassword\":\"$TMP\",\"newPassword\":\"Tpm@Secure2026\"}"); echo "change: $(echo $R | cut -c1-120)"
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $FT" $API/customers); [ "$C" = "200" ] && pass "after changing password the app opens" || fail "after change" "$C"
LR=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"velan.admin","password":"Velan@2026"}'); [ "$(echo "$LR" | j "d['data'].get('forcePasswordChange')")" = "False" ] && pass "admin-chosen onboarding password: straight to dashboard" || fail "chosen pwd" "$(echo $LR | cut -c1-160)"
C=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $NT" $API/customers/$CUST); [ "$C" = "403" ] && pass "other company's record returns HTTP 403" || fail "403 status" "$C"
CT=$(curl -s -o /tmp/r.pdf -w "%{http_code}" -H "Authorization: Bearer $TC" $API/receipts/$RC/pdf); [ "$CT" = "200" ] && [ "$(head -c 4 /tmp/r.pdf)" = "%PDF" ] && pass "receipt voucher PDF" || fail "receipt pdf" "$CT"

echo "SMOKE_FAILS=$FAILS"
[ "$FAILS" -eq 0 ]
