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
R=$(api POST /vehicles '{"code":"TN01AB1234","name":"TN01AB1234","status":"ACTIVE","type":"TIPPER"}'); VH=$(echo "$R" | j "d['data']['id']"); [ -z "$VH" ] && VH=$($PSQL "SELECT id FROM vehicles ORDER BY id LIMIT 1")
MR=$(api POST /maintenance-requests '{"vehicleId":'$VH',"title":"Brake noise","description":"front","priority":"HIGH"}'); echo "mr: $MR" | cut -c1-200; MRID=$(echo "$MR" | j "d['data']['id']")
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

echo "SMOKE_FAILS=$FAILS"
[ "$FAILS" -eq 0 ]
