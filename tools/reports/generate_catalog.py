import json
B = lambda a: f"(CAST(:branchId AS BIGINT) IS NULL OR {a}.branch_id = CAST(:branchId AS BIGINT))"
OPT = lambda col, p, typ='BIGINT': f"(CAST(:{p} AS {typ}) IS NULL OR {col} = CAST(:{p} AS {typ}))"
DATE = lambda col: f"{col} BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)"
STATUS = lambda col: f"(CAST(:status AS VARCHAR) IS NULL OR {col} = CAST(:status AS VARCHAR))"
def C(key,label,typ='text',total=False): return {"key":key,"label":label,"type":typ,"total":total}
R=[]
def rep(key,cat,title,purpose,filters,cols,sql,finance=False):
    R.append(dict(key=key,category=cat,title=title,purpose=purpose,filters=filters,columns=cols,sql=" ".join(sql.split()),finance=finance))

# ---------------- OPERATIONS / TRIPS ----------------
TRIP_JOINS="""FROM trips t JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false
 LEFT JOIN bookings b ON b.id = t.booking_id LEFT JOIN customers c ON c.id = b.customer_id
 LEFT JOIN vehicles v ON v.id = t.vehicle_id LEFT JOIN drivers d ON d.id = t.driver_id
 LEFT JOIN quarries q ON q.id = t.quarry_id LEFT JOIN materials m ON m.id = td.material_id"""
TRIP_WHERE=f"""WHERE t.company_id = :companyId AND t.is_deleted = false AND {DATE('t.trip_date')} AND {B('t')}
 AND {OPT('t.vehicle_id','vehicleId')} AND {OPT('t.driver_id','driverId')} AND {OPT('b.customer_id','customerId')}"""
rep("trip-register","Operations","Trip register","Every trip line in the period with weighbridge weights and billing status — the daily dispatch record.",
 ["date","branch","vehicle","driver","customer","status"],
 [C("trip_no","Trip No"),C("trip_date","Date","date"),C("booking","Booking"),C("customer","Customer"),C("vehicle","Vehicle"),C("driver","Driver"),
  C("quarry","Quarry"),C("material","Material"),C("planned_qty","Planned","qty",True),C("loaded_qty","Loaded","qty",True),C("delivered_qty","Delivered","qty",True),
  C("shortage_qty","Shortage","qty",True),C("status","Status"),C("invoice_no","Invoice")],
 f"""SELECT t.trip_number AS trip_no, t.trip_date, b.booking_number AS booking, c.name AS customer, v.name AS vehicle, d.name AS driver,
 q.name AS quarry, m.name AS material, td.quantity AS planned_qty, td.loaded_quantity AS loaded_qty, td.delivered_quantity AS delivered_qty,
 CASE WHEN td.loaded_quantity IS NOT NULL AND td.delivered_quantity IS NOT NULL THEN td.loaded_quantity - td.delivered_quantity END AS shortage_qty,
 t.status, inv.invoice_number AS invoice_no {TRIP_JOINS}
 LEFT JOIN LATERAL (SELECT si.invoice_number FROM sales_invoice_details sd JOIN sales_invoices si ON si.id = sd.invoice_id
   WHERE sd.trip_id = t.id AND sd.is_deleted = false AND si.is_deleted = false AND si.status <> 'CANCELLED' ORDER BY si.id LIMIT 1) inv ON true
 {TRIP_WHERE} AND {STATUS('t.status')} ORDER BY t.trip_date, t.trip_number""")

rep("daily-dispatch","Operations","Daily dispatch summary","Trips planned, on the road and completed per day, with vehicles and quantity moved.",
 ["date","branch"],
 [C("day","Date","date"),C("trips","Trips","number",True),C("planned","Planned","number",True),C("dispatched","On road","number",True),C("completed","Completed","number",True),
  C("vehicles","Vehicles used","number"),C("drivers","Drivers","number"),C("delivered_qty","Qty delivered","qty",True)],
 f"""SELECT t.trip_date AS day, COUNT(DISTINCT t.id) AS trips,
 COUNT(DISTINCT t.id) FILTER (WHERE t.status = 'PLANNED') AS planned, COUNT(DISTINCT t.id) FILTER (WHERE t.status = 'DISPATCHED') AS dispatched,
 COUNT(DISTINCT t.id) FILTER (WHERE t.status = 'COMPLETED') AS completed, COUNT(DISTINCT t.vehicle_id) AS vehicles, COUNT(DISTINCT t.driver_id) AS drivers,
 COALESCE(SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)) FILTER (WHERE t.status = 'COMPLETED'),0) AS delivered_qty
 FROM trips t LEFT JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false
 WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status <> 'CANCELLED' AND {DATE('t.trip_date')} AND {B('t')}
 GROUP BY t.trip_date ORDER BY t.trip_date""")

rep("weighbridge-shortage","Operations","Weighbridge shortage","Loaded vs delivered weight per trip line; finds loss in transit by vehicle, driver and quarry.",
 ["date","branch","vehicle","driver","customer"],
 [C("trip_no","Trip No"),C("trip_date","Date","date"),C("vehicle","Vehicle"),C("driver","Driver"),C("quarry","Quarry"),C("customer","Customer"),C("material","Material"),
  C("loaded_qty","Loaded","qty",True),C("delivered_qty","Delivered","qty",True),C("shortage_qty","Shortage","qty",True),C("shortage_pct","Shortage %","percent")],
 f"""SELECT t.trip_number AS trip_no, t.trip_date, v.name AS vehicle, d.name AS driver, q.name AS quarry, c.name AS customer, m.name AS material,
 td.loaded_quantity AS loaded_qty, td.delivered_quantity AS delivered_qty, td.loaded_quantity - td.delivered_quantity AS shortage_qty,
 ROUND(100 * (td.loaded_quantity - td.delivered_quantity) / NULLIF(td.loaded_quantity,0), 2) AS shortage_pct {TRIP_JOINS}
 {TRIP_WHERE} AND td.loaded_quantity IS NOT NULL AND td.delivered_quantity IS NOT NULL AND td.loaded_quantity > td.delivered_quantity
 ORDER BY shortage_pct DESC NULLS LAST""")

# ---------------- VEHICLES ----------------
rep("vehicle-performance","Vehicles & trips","Vehicle performance & profitability","Per vehicle: trips, quantity, revenue, fuel, expenses, maintenance and net contribution for the period.",
 ["date","branch","vehicle"],
 [C("vehicle","Vehicle"),C("trips","Trips","number",True),C("delivered_qty","Qty delivered","qty",True),C("revenue","Revenue (taxable)","money",True),
  C("fuel_litres","Fuel (L)","qty",True),C("fuel_cost","Fuel cost","money",True),C("km_run","Km run","number",True),C("km_per_litre","Km/L","number"),
  C("other_expenses","Other expenses","money",True),C("maintenance","Maintenance","money",True),C("net_contribution","Net contribution","money",True)],
 f"""WITH tr AS (SELECT t.vehicle_id, COUNT(DISTINCT t.id) trips, SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)) qty
   FROM trips t JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false
   WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND {DATE('t.trip_date')} GROUP BY t.vehicle_id),
 rv AS (SELECT t.vehicle_id, SUM(sd.taxable_amount) revenue FROM sales_invoice_details sd JOIN sales_invoices si ON si.id = sd.invoice_id
   JOIN trips t ON t.id = sd.trip_id WHERE si.company_id = :companyId AND si.is_deleted = false AND sd.is_deleted = false AND si.status NOT IN ('DRAFT','CANCELLED')
   AND {DATE('t.trip_date')} GROUP BY t.vehicle_id),
 fu AS (SELECT f.vehicle_id, SUM(f.fuel_quantity) litres, SUM(f.total_amount) cost, MAX(f.current_odometer) - MIN(COALESCE(f.previous_odometer, f.current_odometer)) km
   FROM fuel_entries f WHERE f.company_id = :companyId AND f.is_deleted = false AND f.status = 'APPROVED' AND {DATE('f.fuel_date')} GROUP BY f.vehicle_id),
 ex AS (SELECT e.vehicle_id, SUM(COALESCE(e.total_amount, e.amount)) amt FROM expenses e WHERE e.company_id = :companyId AND e.is_deleted = false
   AND e.status IN ('APPROVED','PAID') AND {DATE('e.expense_date')} GROUP BY e.vehicle_id),
 mt AS (SELECT w.vehicle_id, SUM(COALESCE(w.actual_cost,0)) amt FROM work_orders w WHERE w.company_id = :companyId AND w.is_deleted = false
   AND w.status = 'COMPLETED' AND CAST(w.completed_at AS DATE) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE) GROUP BY w.vehicle_id)
 SELECT v.name AS vehicle, COALESCE(tr.trips,0) trips, COALESCE(tr.qty,0) delivered_qty, COALESCE(rv.revenue,0) revenue,
 COALESCE(fu.litres,0) fuel_litres, COALESCE(fu.cost,0) fuel_cost, COALESCE(fu.km,0) km_run, ROUND(fu.km / NULLIF(fu.litres,0), 2) km_per_litre,
 COALESCE(ex.amt,0) other_expenses, COALESCE(mt.amt,0) maintenance,
 COALESCE(rv.revenue,0) - COALESCE(fu.cost,0) - COALESCE(ex.amt,0) - COALESCE(mt.amt,0) net_contribution
 FROM vehicles v LEFT JOIN tr ON tr.vehicle_id = v.id LEFT JOIN rv ON rv.vehicle_id = v.id LEFT JOIN fu ON fu.vehicle_id = v.id
 LEFT JOIN ex ON ex.vehicle_id = v.id LEFT JOIN mt ON mt.vehicle_id = v.id
 WHERE v.company_id = :companyId AND v.is_deleted = false AND {B('v')} AND {OPT('v.id','vehicleId')}
 ORDER BY net_contribution DESC""")

rep("compliance-expiry","Vehicles & trips","Document expiry (compliance)","Vehicle insurance, fitness, permit and driver licence expiring within the next N days or already expired.",
 ["branch","days"],
 [C("kind","Type"),C("name","Vehicle / Driver"),C("document","Document"),C("expiry_date","Expiry","date"),C("days_left","Days left","number"),C("state","State")],
 f"""WITH x AS (
 SELECT 'Vehicle' kind, v.name, 'Insurance' document, v.insurance_expiry_date expiry_date, v.company_id, v.branch_id, v.is_deleted FROM vehicles v
 UNION ALL SELECT 'Vehicle', v.name, 'Fitness', v.fitness_expiry_date, v.company_id, v.branch_id, v.is_deleted FROM vehicles v
 UNION ALL SELECT 'Vehicle', v.name, 'Permit', v.permit_expiry_date, v.company_id, v.branch_id, v.is_deleted FROM vehicles v
 UNION ALL SELECT 'Driver', d.name, 'Driving licence', d.license_expiry_date, d.company_id, d.branch_id, d.is_deleted FROM drivers d)
 SELECT kind, name, document, expiry_date, expiry_date - CURRENT_DATE AS days_left,
 CASE WHEN expiry_date < CURRENT_DATE THEN 'EXPIRED' ELSE 'DUE' END AS state
 FROM x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.expiry_date IS NOT NULL AND {B('x')}
 AND x.expiry_date <= CURRENT_DATE + CAST(COALESCE(CAST(:days AS INTEGER), 30) AS INTEGER) ORDER BY expiry_date""")

# ---------------- DRIVERS ----------------
rep("driver-performance","Drivers & settlement","Driver performance","Per driver: trips, working days, quantity, shortage, fuel drawn, bata, payroll and advance balance.",
 ["date","branch","driver"],
 [C("driver","Driver"),C("trips","Trips","number",True),C("trip_days","Trip days","number",True),C("delivered_qty","Qty delivered","qty",True),
  C("shortage_qty","Shortage","qty",True),C("fuel_litres","Fuel (L)","qty",True),C("bata","Bata paid","money",True),C("payroll_gross","Payroll gross","money",True),
  C("advance_outstanding","Advance outstanding","money",True)],
 f"""WITH tr AS (SELECT t.driver_id, COUNT(DISTINCT t.id) trips, COUNT(DISTINCT t.trip_date) days,
   SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)) qty,
   SUM(CASE WHEN td.loaded_quantity > td.delivered_quantity THEN td.loaded_quantity - td.delivered_quantity ELSE 0 END) short
   FROM trips t JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false
   WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND {DATE('t.trip_date')} GROUP BY t.driver_id),
 fu AS (SELECT f.driver_id, SUM(f.fuel_quantity) litres FROM fuel_entries f WHERE f.company_id = :companyId AND f.is_deleted = false
   AND f.status = 'APPROVED' AND {DATE('f.fuel_date')} GROUP BY f.driver_id),
 bt AS (SELECT e.driver_id, SUM(COALESCE(e.total_amount, e.amount)) amt FROM expenses e WHERE e.company_id = :companyId AND e.is_deleted = false
   AND e.category = 'DRIVER_BATA' AND e.status IN ('APPROVED','PAID') AND {DATE('e.expense_date')} GROUP BY e.driver_id),
 pr AS (SELECT p.driver_id, SUM(p.gross_amount) gross FROM driver_payrolls p WHERE p.company_id = :companyId AND p.is_deleted = false
   AND p.status IN ('POSTED','PAID') AND make_date(p.pay_year, p.pay_month, 1) BETWEEN date_trunc('month', CAST(:from AS DATE)) AND CAST(:to AS DATE) GROUP BY p.driver_id),
 ad AS (SELECT a.driver_id, SUM(a.amount - a.recovered_amount) bal FROM driver_advances a WHERE a.company_id = :companyId AND a.is_deleted = false
   AND a.status = 'ISSUED' GROUP BY a.driver_id)
 SELECT d.name AS driver, COALESCE(tr.trips,0) trips, COALESCE(tr.days,0) trip_days, COALESCE(tr.qty,0) delivered_qty, COALESCE(tr.short,0) shortage_qty,
 COALESCE(fu.litres,0) fuel_litres, COALESCE(bt.amt,0) bata, COALESCE(pr.gross,0) payroll_gross, COALESCE(ad.bal,0) advance_outstanding
 FROM drivers d LEFT JOIN tr ON tr.driver_id = d.id LEFT JOIN fu ON fu.driver_id = d.id LEFT JOIN bt ON bt.driver_id = d.id
 LEFT JOIN pr ON pr.driver_id = d.id LEFT JOIN ad ON ad.driver_id = d.id
 WHERE d.company_id = :companyId AND d.is_deleted = false AND {B('d')} AND {OPT('d.id','driverId')} ORDER BY trips DESC, d.name""")

rep("driver-payroll-register","Drivers & settlement","Driver payroll register","Payrolls for the months in the period: trips, gross, deductions, advance recovered, net, status and payment.",
 ["date","branch","driver","status"],
 [C("payroll_no","Payroll No"),C("driver","Driver"),C("period","Month"),C("trips","Trips","number",True),C("trip_earnings","Trip earnings","money",True),
  C("basic","Basic","money",True),C("allowance","Allowance","money",True),C("gross","Gross","money",True),C("deductions","Deductions","money",True),
  C("advance","Advance recovered","money",True),C("net","Net payable","money",True),C("status","Status"),C("paid_date","Paid on","date")],
 f"""SELECT p.payroll_number payroll_no, d.name driver, to_char(make_date(p.pay_year,p.pay_month,1),'Mon YYYY') period, p.total_trips trips,
 p.trip_earnings, p.basic_salary basic, p.allowance_amount allowance, p.gross_amount gross, p.deduction_amount deductions, p.advance_adjustment advance,
 p.net_salary_payable net, p.status, p.paid_date
 FROM driver_payrolls p JOIN drivers d ON d.id = p.driver_id
 WHERE p.company_id = :companyId AND p.is_deleted = false AND {B('p')} AND {OPT('p.driver_id','driverId')} AND {STATUS('p.status')}
 AND make_date(p.pay_year, p.pay_month, 1) BETWEEN date_trunc('month', CAST(:from AS DATE)) AND CAST(:to AS DATE)
 ORDER BY p.pay_year, p.pay_month, d.name""", finance=True)

rep("driver-advances","Drivers & settlement","Driver advances & balance","Advances given, recovered through payroll and still outstanding per driver.",
 ["date","branch","driver","status"],
 [C("advance_no","Advance No"),C("driver","Driver"),C("advance_date","Date","date"),C("amount","Amount","money",True),C("recovered","Recovered","money",True),
  C("outstanding","Outstanding","money",True),C("method","Paid by"),C("status","Status")],
 f"""SELECT a.advance_number advance_no, d.name driver, a.advance_date, a.amount, a.recovered_amount recovered,
 CASE WHEN a.status = 'ISSUED' THEN a.amount - a.recovered_amount ELSE 0 END outstanding, a.payment_method method, a.status
 FROM driver_advances a JOIN drivers d ON d.id = a.driver_id
 WHERE a.company_id = :companyId AND a.is_deleted = false AND {B('a')} AND {OPT('a.driver_id','driverId')} AND {STATUS('a.status')} AND {DATE('a.advance_date')}
 ORDER BY a.advance_date""", finance=True)

# ---------------- FUEL & EXPENSES ----------------
rep("fuel-register","Fuel & expenses","Fuel register","Every fuel fill with litres, rate, amount and odometer.",
 ["date","branch","vehicle","driver","status"],
 [C("entry_no","Entry No"),C("fuel_date","Date","date"),C("vehicle","Vehicle"),C("driver","Driver"),C("station","Station"),C("litres","Litres","qty",True),
  C("rate","Rate/L","money"),C("amount","Amount","money",True),C("odometer","Odometer","number"),C("km","Km since last","number",True),C("status","Status")],
 f"""SELECT f.fuel_entry_number entry_no, f.fuel_date, v.name vehicle, d.name driver, f.fuel_station station, f.fuel_quantity litres, f.rate_per_litre rate,
 f.total_amount amount, f.current_odometer odometer, CASE WHEN f.previous_odometer IS NOT NULL THEN f.current_odometer - f.previous_odometer END km, f.status
 FROM fuel_entries f LEFT JOIN vehicles v ON v.id = f.vehicle_id LEFT JOIN drivers d ON d.id = f.driver_id
 WHERE f.company_id = :companyId AND f.is_deleted = false AND {DATE('f.fuel_date')} AND {B('f')} AND {OPT('f.vehicle_id','vehicleId')}
 AND {OPT('f.driver_id','driverId')} AND {STATUS('f.status')} ORDER BY f.fuel_date, f.id""")

rep("fuel-efficiency","Fuel & expenses","Fuel efficiency by vehicle","Km per litre and fuel cost per km for each vehicle — spots fuel theft and poor mileage.",
 ["date","branch","vehicle"],
 [C("vehicle","Vehicle"),C("fills","Fills","number",True),C("litres","Litres","qty",True),C("cost","Fuel cost","money",True),C("km_run","Km run","number",True),
  C("km_per_litre","Km/L","number"),C("cost_per_km","Cost/Km","money"),C("avg_rate","Avg rate/L","money")],
 f"""SELECT v.name vehicle, COUNT(*) fills, SUM(f.fuel_quantity) litres, SUM(f.total_amount) cost,
 SUM(CASE WHEN f.previous_odometer IS NOT NULL THEN f.current_odometer - f.previous_odometer ELSE 0 END) km_run,
 ROUND(SUM(CASE WHEN f.previous_odometer IS NOT NULL THEN f.current_odometer - f.previous_odometer ELSE 0 END) / NULLIF(SUM(f.fuel_quantity),0), 2) km_per_litre,
 ROUND(SUM(f.total_amount) / NULLIF(SUM(CASE WHEN f.previous_odometer IS NOT NULL THEN f.current_odometer - f.previous_odometer ELSE 0 END),0), 2) cost_per_km,
 ROUND(SUM(f.total_amount) / NULLIF(SUM(f.fuel_quantity),0), 2) avg_rate
 FROM fuel_entries f JOIN vehicles v ON v.id = f.vehicle_id
 WHERE f.company_id = :companyId AND f.is_deleted = false AND f.status = 'APPROVED' AND {DATE('f.fuel_date')} AND {B('f')} AND {OPT('f.vehicle_id','vehicleId')}
 GROUP BY v.name ORDER BY km_per_litre NULLS LAST""")

rep("expense-register","Fuel & expenses","Expense register","All expense vouchers with category, vehicle, driver, GST and status.",
 ["date","branch","vehicle","driver","status","category"],
 [C("expense_no","Voucher"),C("expense_date","Date","date"),C("category","Category"),C("vehicle","Vehicle"),C("driver","Driver"),C("amount","Amount","money",True),
  C("gst","GST","money",True),C("total","Total","money",True),C("method","Paid by"),C("status","Status"),C("remarks","Remarks")],
 f"""SELECT e.expense_number expense_no, e.expense_date, e.category, v.name vehicle, d.name driver, e.amount, e.gst_amount gst,
 COALESCE(e.total_amount, e.amount) total, e.payment_method method, e.status, e.remarks
 FROM expenses e LEFT JOIN vehicles v ON v.id = e.vehicle_id LEFT JOIN drivers d ON d.id = e.driver_id
 WHERE e.company_id = :companyId AND e.is_deleted = false AND {DATE('e.expense_date')} AND {B('e')} AND {OPT('e.vehicle_id','vehicleId')}
 AND {OPT('e.driver_id','driverId')} AND {STATUS('e.status')} AND (CAST(:category AS VARCHAR) IS NULL OR e.category = CAST(:category AS VARCHAR))
 ORDER BY e.expense_date, e.id""")

rep("expense-summary","Fuel & expenses","Expense summary by category","Approved spending grouped by category, plus fuel, for cost control.",
 ["date","branch","vehicle"],
 [C("category","Category"),C("vouchers","Vouchers","number",True),C("amount","Amount","money",True),C("gst","GST","money",True),C("total","Total","money",True),C("share_pct","Share %","percent")],
 f"""WITH x AS (
 SELECT e.category, COUNT(*) vouchers, SUM(e.amount) amount, SUM(COALESCE(e.gst_amount,0)) gst, SUM(COALESCE(e.total_amount, e.amount)) total
 FROM expenses e WHERE e.company_id = :companyId AND e.is_deleted = false AND e.status IN ('APPROVED','PAID') AND {DATE('e.expense_date')} AND {B('e')} AND {OPT('e.vehicle_id','vehicleId')}
 GROUP BY e.category
 UNION ALL SELECT 'FUEL', COUNT(*), SUM(f.total_amount), 0, SUM(f.total_amount) FROM fuel_entries f
 WHERE f.company_id = :companyId AND f.is_deleted = false AND f.status = 'APPROVED' AND {DATE('f.fuel_date')} AND {B('f')} AND {OPT('f.vehicle_id','vehicleId')})
 SELECT category, vouchers, amount, gst, total, ROUND(100 * total / NULLIF(SUM(total) OVER (),0), 2) share_pct FROM x WHERE vouchers > 0 ORDER BY total DESC""")

# ---------------- MAINTENANCE ----------------
rep("work-order-register","Maintenance & work orders","Work order register","Jobs with type, status, days open, parts from stock, labour and actual cost.",
 ["date","branch","vehicle","status"],
 [C("wo_no","WO No"),C("vehicle","Vehicle"),C("job","Job"),C("type","Type"),C("priority","Priority"),C("status","Status"),C("opened","Opened","date"),
  C("completed","Completed","date"),C("days_open","Days","number"),C("parts_cost","Parts from stock","money",True),C("labour","Labour","money",True),
  C("estimated","Estimated","money",True),C("actual","Actual cost","money",True),C("workshop","Workshop")],
 f"""SELECT w.work_order_number wo_no, v.name vehicle, w.name job, w.maintenance_type type, w.priority, w.status,
 CAST(w.opened_at AS DATE) opened, CAST(w.completed_at AS DATE) completed,
 CAST(COALESCE(w.completed_at, w.cancelled_at, CURRENT_TIMESTAMP) AS DATE) - CAST(w.opened_at AS DATE) days_open,
 COALESCE((SELECT SUM(CASE WHEN it.transaction_type = 'ISSUE' THEN 1 ELSE -1 END * it.quantity * COALESCE(it.unit_rate,0)) FROM inventory_transactions it
   WHERE it.work_order_id = w.id AND it.is_deleted = false AND it.transaction_type IN ('ISSUE','RETURN')),0) parts_cost,
 COALESCE((SELECT SUM(l.line_total) FROM work_order_labour l WHERE l.work_order_id = w.id AND l.is_deleted = false),0) labour,
 w.estimated_cost estimated, w.actual_cost actual, s.name workshop
 FROM work_orders w JOIN vehicles v ON v.id = w.vehicle_id LEFT JOIN suppliers s ON s.id = w.supplier_id
 WHERE w.company_id = :companyId AND w.is_deleted = false AND CAST(w.opened_at AS DATE) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
 AND {B('w')} AND {OPT('w.vehicle_id','vehicleId')} AND {STATUS('w.status')} ORDER BY w.opened_at""")

rep("maintenance-cost-by-vehicle","Maintenance & work orders","Maintenance cost by vehicle","Completed jobs per vehicle split by type (preventive, repair, breakdown…) with parts and total cost.",
 ["date","branch","vehicle"],
 [C("vehicle","Vehicle"),C("jobs","Jobs","number",True),C("preventive","Preventive","number",True),C("other","Repair/other","number",True),
  C("parts_cost","Parts from stock","money",True),C("total_cost","Total cost","money",True),C("avg_days","Avg days","number")],
 f"""SELECT v.name vehicle, COUNT(*) jobs, COUNT(*) FILTER (WHERE w.source = 'PREVENTIVE' OR w.maintenance_rule_id IS NOT NULL) preventive,
 COUNT(*) FILTER (WHERE NOT (w.source = 'PREVENTIVE' OR w.maintenance_rule_id IS NOT NULL)) other,
 COALESCE(SUM((SELECT SUM(CASE WHEN it.transaction_type = 'ISSUE' THEN 1 ELSE -1 END * it.quantity * COALESCE(it.unit_rate,0)) FROM inventory_transactions it
   WHERE it.work_order_id = w.id AND it.is_deleted = false AND it.transaction_type IN ('ISSUE','RETURN'))),0) parts_cost,
 COALESCE(SUM(w.actual_cost),0) total_cost, ROUND(AVG(CAST(w.completed_at AS DATE) - CAST(w.opened_at AS DATE)), 1) avg_days
 FROM work_orders w JOIN vehicles v ON v.id = w.vehicle_id
 WHERE w.company_id = :companyId AND w.is_deleted = false AND w.status = 'COMPLETED'
 AND CAST(w.completed_at AS DATE) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE) AND {B('w')} AND {OPT('w.vehicle_id','vehicleId')}
 GROUP BY v.name ORDER BY total_cost DESC""")

rep("maintenance-request-register","Maintenance & work orders","Maintenance request register","Problems reported by drivers/staff: priority, how fast they were approved and the work order outcome.",
 ["date","branch","vehicle","status"],
 [C("request_no","Request No"),C("requested","Reported","date"),C("vehicle","Vehicle"),C("driver","Driver"),C("title","Problem"),C("priority","Priority"),
  C("status","Status"),C("hours_to_approve","Hours to approve","number"),C("wo_no","Work order"),C("wo_status","WO status")],
 f"""SELECT r.request_number request_no, CAST(r.requested_at AS DATE) requested, v.name vehicle, d.name driver, r.name title, r.priority, r.status,
 ROUND(CAST(EXTRACT(EPOCH FROM (r.approved_at - r.requested_at)) / 3600 AS NUMERIC), 1) hours_to_approve, w.work_order_number wo_no, w.status wo_status
 FROM maintenance_requests r JOIN vehicles v ON v.id = r.vehicle_id LEFT JOIN drivers d ON d.id = r.driver_id LEFT JOIN work_orders w ON w.id = r.work_order_id
 WHERE r.company_id = :companyId AND r.is_deleted = false AND CAST(r.requested_at AS DATE) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
 AND {B('r')} AND {OPT('r.vehicle_id','vehicleId')} AND {STATUS('r.status')} ORDER BY r.requested_at""")

rep("parts-consumption","Maintenance & work orders","Spare parts consumption","Parts issued to work orders (net of returns) by part and vehicle, with value at cost.",
 ["date","branch","vehicle","sparePart"],
 [C("part_code","Part code"),C("part","Part"),C("vehicle","Vehicle"),C("jobs","Jobs","number",True),C("net_qty","Net qty","qty",True),C("value","Value","money",True)],
 f"""SELECT sp.code part_code, sp.name part, v.name vehicle, COUNT(DISTINCT it.work_order_id) jobs,
 SUM(CASE WHEN it.transaction_type = 'ISSUE' THEN it.quantity ELSE -it.quantity END) net_qty,
 SUM(CASE WHEN it.transaction_type = 'ISSUE' THEN 1 ELSE -1 END * it.quantity * COALESCE(it.unit_rate,0)) value
 FROM inventory_transactions it JOIN spare_parts sp ON sp.id = it.spare_part_id JOIN work_orders w ON w.id = it.work_order_id JOIN vehicles v ON v.id = w.vehicle_id
 WHERE it.company_id = :companyId AND it.is_deleted = false AND it.transaction_type IN ('ISSUE','RETURN')
 AND CAST(it.created_date AS DATE) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE) AND {B('it')} AND {OPT('w.vehicle_id','vehicleId')} AND {OPT('it.spare_part_id','sparePartId')}
 GROUP BY sp.code, sp.name, v.name HAVING SUM(CASE WHEN it.transaction_type = 'ISSUE' THEN it.quantity ELSE -it.quantity END) <> 0 ORDER BY value DESC""")

# ---------------- STOCK ----------------
rep("stock-valuation","Warehouse & stock","Stock valuation","Current quantity, average cost, value and reorder status for every part in every warehouse.",
 ["branch","warehouse","sparePart"],
 [C("warehouse","Warehouse"),C("part_code","Part code"),C("part","Part"),C("qty","On hand","qty",True),C("avg_cost","Avg cost","money"),C("value","Value","money",True),
  C("reorder_level","Reorder level","qty"),C("state","Status")],
 f"""SELECT w.name warehouse, sp.code part_code, sp.name part, s.available_quantity qty, s.average_cost avg_cost, ROUND(s.available_quantity * s.average_cost, 2) value,
 sp.reorder_level, CASE WHEN s.available_quantity <= 0 THEN 'OUT' WHEN sp.reorder_level IS NOT NULL AND s.available_quantity <= sp.reorder_level THEN 'REORDER' ELSE 'OK' END state
 FROM warehouse_stock s JOIN warehouses w ON w.id = s.warehouse_id JOIN spare_parts sp ON sp.id = s.spare_part_id
 WHERE s.company_id = :companyId AND s.is_deleted = false AND {B('s')} AND {OPT('s.warehouse_id','warehouseId')} AND {OPT('s.spare_part_id','sparePartId')}
 ORDER BY w.name, sp.name""")

rep("reorder-list","Warehouse & stock","Reorder / out-of-stock list","Parts at or below the reorder level — the purchase list for the stores.",
 ["branch","warehouse"],
 [C("warehouse","Warehouse"),C("part_code","Part code"),C("part","Part"),C("qty","On hand","qty"),C("reorder_level","Reorder level","qty"),
  C("shortfall","Suggested order","qty",True),C("last_rate","Last rate","money"),C("est_value","Est. value","money",True)],
 f"""SELECT w.name warehouse, sp.code part_code, sp.name part, s.available_quantity qty, sp.reorder_level,
 GREATEST(sp.reorder_level * 2 - s.available_quantity, 0) shortfall, NULLIF(s.average_cost,0) last_rate,
 ROUND(GREATEST(sp.reorder_level * 2 - s.available_quantity, 0) * COALESCE(NULLIF(s.average_cost,0), sp.default_rate, 0), 2) est_value
 FROM warehouse_stock s JOIN warehouses w ON w.id = s.warehouse_id JOIN spare_parts sp ON sp.id = s.spare_part_id
 WHERE s.company_id = :companyId AND s.is_deleted = false AND sp.reorder_level IS NOT NULL AND s.available_quantity <= sp.reorder_level
 AND {B('s')} AND {OPT('s.warehouse_id','warehouseId')} ORDER BY s.available_quantity - sp.reorder_level""")

SIGN="CASE WHEN it.transaction_type = 'ISSUE' THEN -1 ELSE 1 END"
rep("stock-movement-summary","Warehouse & stock","Stock movement summary","Opening, received, issued/returned and closing quantity per part for the period.",
 ["date","branch","warehouse","sparePart"],
 [C("warehouse","Warehouse"),C("part","Part"),C("opening","Opening","qty",True),C("received","Received","qty",True),C("returned","Returned","qty",True),
  C("issued","Issued","qty",True),C("closing","Closing","qty",True),C("received_value","Received value","money",True)],
 f"""SELECT w.name warehouse, sp.name part,
 COALESCE(SUM({SIGN} * it.quantity) FILTER (WHERE CAST(it.created_date AS DATE) < CAST(:from AS DATE)),0) opening,
 COALESCE(SUM(it.quantity) FILTER (WHERE it.transaction_type IN ('RECEIPT','OPENING_BALANCE') AND {DATE('CAST(it.created_date AS DATE)')}),0) received,
 COALESCE(SUM(it.quantity) FILTER (WHERE it.transaction_type = 'RETURN' AND {DATE('CAST(it.created_date AS DATE)')}),0) returned,
 COALESCE(SUM(it.quantity) FILTER (WHERE it.transaction_type = 'ISSUE' AND {DATE('CAST(it.created_date AS DATE)')}),0) issued,
 COALESCE(SUM({SIGN} * it.quantity) FILTER (WHERE CAST(it.created_date AS DATE) <= CAST(:to AS DATE)),0) closing,
 COALESCE(SUM(it.quantity * COALESCE(it.unit_rate,0)) FILTER (WHERE it.transaction_type IN ('RECEIPT','OPENING_BALANCE') AND {DATE('CAST(it.created_date AS DATE)')}),0) received_value
 FROM inventory_transactions it JOIN warehouses w ON w.id = it.warehouse_id JOIN spare_parts sp ON sp.id = it.spare_part_id
 WHERE it.company_id = :companyId AND it.is_deleted = false AND {B('it')} AND {OPT('it.warehouse_id','warehouseId')} AND {OPT('it.spare_part_id','sparePartId')}
 GROUP BY w.name, sp.name ORDER BY w.name, sp.name""")

rep("inventory-ledger","Warehouse & stock","Inventory ledger","Every stock movement with in/out quantity, rate, value and the work order or supplier.",
 ["date","branch","warehouse","sparePart"],
 [C("txn_date","Date","date"),C("txn_no","Reference"),C("type","Type"),C("warehouse","Warehouse"),C("part","Part"),C("qty_in","In","qty",True),
  C("qty_out","Out","qty",True),C("rate","Rate","money"),C("value","Value","money",True),C("work_order","Work order"),C("supplier","Supplier"),C("bill_no","Bill no")],
 f"""SELECT CAST(it.created_date AS DATE) txn_date, it.code txn_no, it.transaction_type type, w.name warehouse, sp.name part,
 CASE WHEN it.transaction_type <> 'ISSUE' THEN it.quantity END qty_in, CASE WHEN it.transaction_type = 'ISSUE' THEN it.quantity END qty_out,
 it.unit_rate rate, ROUND(it.quantity * COALESCE(it.unit_rate,0), 2) value, wo.work_order_number work_order, s.name supplier, it.external_reference bill_no
 FROM inventory_transactions it JOIN warehouses w ON w.id = it.warehouse_id JOIN spare_parts sp ON sp.id = it.spare_part_id
 LEFT JOIN work_orders wo ON wo.id = it.work_order_id LEFT JOIN suppliers s ON s.id = it.supplier_id
 WHERE it.company_id = :companyId AND it.is_deleted = false AND {DATE('CAST(it.created_date AS DATE)')} AND {B('it')}
 AND {OPT('it.warehouse_id','warehouseId')} AND {OPT('it.spare_part_id','sparePartId')} ORDER BY it.created_date, it.id""")

rep("purchase-register","Warehouse & stock","Spare parts purchase register","Stock receipts with supplier, bill number, quantity, rate, value and how they were paid.",
 ["date","branch","warehouse","sparePart"],
 [C("txn_date","Date","date"),C("txn_no","Receipt"),C("supplier","Supplier"),C("bill_no","Bill no"),C("warehouse","Warehouse"),C("part","Part"),
  C("qty","Qty","qty",True),C("rate","Rate","money"),C("value","Value","money",True),C("payment_mode","Paid by")],
 f"""SELECT CAST(it.created_date AS DATE) txn_date, it.code txn_no, s.name supplier, it.external_reference bill_no, w.name warehouse, sp.name part,
 it.quantity qty, it.unit_rate rate, ROUND(it.quantity * COALESCE(it.unit_rate,0), 2) value, COALESCE(it.payment_mode, 'CREDIT') payment_mode
 FROM inventory_transactions it JOIN warehouses w ON w.id = it.warehouse_id JOIN spare_parts sp ON sp.id = it.spare_part_id LEFT JOIN suppliers s ON s.id = it.supplier_id
 WHERE it.company_id = :companyId AND it.is_deleted = false AND it.transaction_type = 'RECEIPT' AND {DATE('CAST(it.created_date AS DATE)')} AND {B('it')}
 AND {OPT('it.warehouse_id','warehouseId')} AND {OPT('it.spare_part_id','sparePartId')} ORDER BY it.created_date""", finance=False)

# ---------------- BOOKINGS ----------------
DELIV="""(SELECT COALESCE(SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)),0) FROM trip_details td JOIN trips t ON t.id = td.trip_id
  WHERE t.booking_id = bk.id AND td.material_id = bd.material_id AND t.status = 'COMPLETED' AND t.is_deleted = false AND td.is_deleted = false)"""
rep("booking-register","Bookings & delivery","Booking & delivery status","Booked vs delivered vs pending quantity per booking and material.",
 ["date","branch","customer","status"],
 [C("booking_no","Booking"),C("booking_date","Date","date"),C("customer","Customer"),C("site","Delivery site"),C("material","Material"),
  C("booked","Booked","qty",True),C("delivered","Delivered","qty",True),C("pending","Pending","qty",True),C("fulfilment_pct","Done %","percent"),
  C("value","Booking value","money",True),C("status","Status")],
 f"""SELECT bk.booking_number booking_no, bk.booking_date, c.name customer, ds.site_name site, m.name material, bd.quantity booked,
 {DELIV} delivered, GREATEST(bd.quantity - {DELIV}, 0) pending, ROUND(100 * {DELIV} / NULLIF(bd.quantity,0), 1) fulfilment_pct, bd.net_amount value, bk.status
 FROM bookings bk JOIN booking_details bd ON bd.booking_id = bk.id AND bd.is_deleted = false LEFT JOIN customers c ON c.id = bk.customer_id
 LEFT JOIN customer_delivery_sites ds ON ds.id = bk.delivery_site_id LEFT JOIN materials m ON m.id = bd.material_id
 WHERE bk.company_id = :companyId AND bk.is_deleted = false AND {DATE('bk.booking_date')} AND {B('bk')} AND {OPT('bk.customer_id','customerId')}
 AND {STATUS('bk.status')} ORDER BY bk.booking_date, bk.booking_number""")

rep("pending-deliveries","Bookings & delivery","Pending deliveries","Approved bookings that still have quantity to deliver, oldest first.",
 ["branch","customer"],
 [C("booking_no","Booking"),C("booking_date","Date","date"),C("age_days","Age (days)","number"),C("customer","Customer"),C("site","Site"),C("material","Material"),
  C("booked","Booked","qty",True),C("delivered","Delivered","qty",True),C("pending","Pending","qty",True),C("open_trips","Trips on road","number",True)],
 f"""SELECT * FROM (SELECT bk.booking_number booking_no, bk.booking_date, CURRENT_DATE - bk.booking_date age_days, c.name customer, ds.site_name site, m.name material,
 bd.quantity booked, {DELIV} delivered, bd.quantity - {DELIV} pending,
 (SELECT COUNT(*) FROM trips t WHERE t.booking_id = bk.id AND t.is_deleted = false AND t.status IN ('PLANNED','DISPATCHED')) open_trips
 FROM bookings bk JOIN booking_details bd ON bd.booking_id = bk.id AND bd.is_deleted = false LEFT JOIN customers c ON c.id = bk.customer_id
 LEFT JOIN customer_delivery_sites ds ON ds.id = bk.delivery_site_id LEFT JOIN materials m ON m.id = bd.material_id
 WHERE bk.company_id = :companyId AND bk.is_deleted = false AND bk.status = 'APPROVED' AND {B('bk')} AND {OPT('bk.customer_id','customerId')}) x
 WHERE pending > 0 ORDER BY booking_date""")

# ---------------- SALES / RECEIPTS ----------------
INV_OK="si.status NOT IN ('DRAFT','CANCELLED')"
rep("sales-register","Invoices & receipts","Sales register (GST)","Approved invoices with GSTIN, place of supply, taxable value and CGST/SGST/IGST — for GST returns.",
 ["date","branch","customer","status"],
 [C("invoice_no","Invoice"),C("invoice_date","Date","date"),C("customer","Customer"),C("gstin","GSTIN"),C("pos","Place of supply"),C("supply","Supply"),
  C("taxable","Taxable","money",True),C("cgst","CGST","money",True),C("sgst","SGST","money",True),C("igst","IGST","money",True),C("total","Invoice total","money",True),
  C("paid","Received","money",True),C("balance","Balance","money",True),C("status","Status")],
 f"""SELECT si.invoice_number invoice_no, si.invoice_date, c.name customer, c.gst_number gstin, si.place_of_supply pos, si.supply_type supply, si.taxable_amount taxable,
 (SELECT COALESCE(SUM(sd.cgst),0) FROM sales_invoice_details sd WHERE sd.invoice_id = si.id AND sd.is_deleted = false) cgst,
 (SELECT COALESCE(SUM(sd.sgst),0) FROM sales_invoice_details sd WHERE sd.invoice_id = si.id AND sd.is_deleted = false) sgst,
 (SELECT COALESCE(SUM(sd.igst),0) FROM sales_invoice_details sd WHERE sd.invoice_id = si.id AND sd.is_deleted = false) igst,
 si.net_amount total, COALESCE(si.paid_amount,0) paid, si.net_amount - COALESCE(si.paid_amount,0) balance, si.status
 FROM sales_invoices si LEFT JOIN customers c ON c.id = si.customer_id
 WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND {DATE('si.invoice_date')} AND {B('si')} AND {OPT('si.customer_id','customerId')}
 AND {STATUS('si.payment_status')} ORDER BY si.invoice_date, si.invoice_number""", finance=True)

rep("gst-summary","Invoices & receipts","GST summary (rate-wise)","Taxable value and tax by month, GST rate and intra/inter-state — the figures for GSTR-1 / 3B.",
 ["date","branch"],
 [C("month","Month"),C("supply","Supply"),C("gst_rate","GST %","number"),C("invoices","Invoices","number",True),C("taxable","Taxable","money",True),
  C("cgst","CGST","money",True),C("sgst","SGST","money",True),C("igst","IGST","money",True),C("total_tax","Total tax","money",True)],
 f"""SELECT to_char(si.invoice_date,'YYYY-MM') AS month, si.supply_type supply, sd.gst_percentage gst_rate, COUNT(DISTINCT si.id) invoices, SUM(sd.taxable_amount) taxable,
 SUM(sd.cgst) cgst, SUM(sd.sgst) sgst, SUM(sd.igst) igst, SUM(sd.cgst + sd.sgst + sd.igst) total_tax
 FROM sales_invoices si JOIN sales_invoice_details sd ON sd.invoice_id = si.id AND sd.is_deleted = false
 WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND {DATE('si.invoice_date')} AND {B('si')}
 GROUP BY to_char(si.invoice_date,'YYYY-MM'), si.supply_type, sd.gst_percentage ORDER BY 1, 2, 3""", finance=True)

rep("customer-revenue","Invoices & receipts","Customer-wise revenue","Billing, collections and balance per customer for the period — who are the best and slowest payers.",
 ["date","branch","customer"],
 [C("customer","Customer"),C("invoices","Invoices","number",True),C("taxable","Taxable","money",True),C("tax","Tax","money",True),C("billed","Billed","money",True),
  C("received","Received","money",True),C("balance","Balance","money",True),C("share_pct","Revenue share %","percent")],
 f"""SELECT c.name customer, COUNT(*) invoices, SUM(si.taxable_amount) taxable, SUM(si.tax_amount) tax, SUM(si.net_amount) billed,
 SUM(COALESCE(si.paid_amount,0)) received, SUM(si.net_amount - COALESCE(si.paid_amount,0)) balance,
 ROUND(100 * SUM(si.taxable_amount) / NULLIF(SUM(SUM(si.taxable_amount)) OVER (),0), 2) share_pct
 FROM sales_invoices si JOIN customers c ON c.id = si.customer_id
 WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND {DATE('si.invoice_date')} AND {B('si')} AND {OPT('si.customer_id','customerId')}
 GROUP BY c.name ORDER BY billed DESC""", finance=True)

rep("receipt-register","Invoices & receipts","Receipt register","Money received with mode, reference, amount allocated to invoices and advance left.",
 ["date","branch","customer","status"],
 [C("receipt_no","Receipt"),C("receipt_date","Date","date"),C("customer","Customer"),C("mode","Mode"),C("reference","Reference"),C("amount","Amount","money",True),
  C("allocated","Allocated","money",True),C("unallocated","Unallocated","money",True),C("status","Status")],
 f"""SELECT r.receipt_number receipt_no, r.receipt_date, c.name customer, r.payment_method mode, r.reference_number reference, r.amount_received amount,
 COALESCE((SELECT SUM(a.allocated_amount) FROM customer_receipt_allocations a WHERE a.receipt_id = r.id AND a.is_deleted = false),0) allocated,
 r.amount_received - COALESCE((SELECT SUM(a.allocated_amount) FROM customer_receipt_allocations a WHERE a.receipt_id = r.id AND a.is_deleted = false),0) unallocated,
 r.status FROM customer_receipts r LEFT JOIN customers c ON c.id = r.customer_id
 WHERE r.company_id = :companyId AND r.is_deleted = false AND {DATE('r.receipt_date')} AND {B('r')} AND {OPT('r.customer_id','customerId')} AND {STATUS('r.status')}
 ORDER BY r.receipt_date, r.receipt_number""", finance=True)

AGE="CAST(:to AS DATE) - si.invoice_date"
rep("customer-outstanding","Outstanding & pending","Customer outstanding & ageing","Unpaid invoice balance per customer as on the To date, split into 0-30, 31-60, 61-90 and 90+ days, against credit limit.",
 ["to","branch","customer"],
 [C("customer","Customer"),C("invoices","Open invoices","number",True),C("d0_30","0-30 days","money",True),C("d31_60","31-60","money",True),C("d61_90","61-90","money",True),
  C("d90","90+","money",True),C("total","Total due","money",True),C("credit_limit","Credit limit","money"),C("over_limit","Over limit","money",True)],
 f"""SELECT c.name customer, COUNT(*) invoices,
 SUM(si.net_amount - COALESCE(si.paid_amount,0)) FILTER (WHERE {AGE} <= 30) d0_30,
 SUM(si.net_amount - COALESCE(si.paid_amount,0)) FILTER (WHERE {AGE} BETWEEN 31 AND 60) d31_60,
 SUM(si.net_amount - COALESCE(si.paid_amount,0)) FILTER (WHERE {AGE} BETWEEN 61 AND 90) d61_90,
 SUM(si.net_amount - COALESCE(si.paid_amount,0)) FILTER (WHERE {AGE} > 90) d90,
 SUM(si.net_amount - COALESCE(si.paid_amount,0)) total, MAX(c.credit_limit) credit_limit,
 CASE WHEN MAX(c.credit_limit) > 0 THEN GREATEST(SUM(si.net_amount - COALESCE(si.paid_amount,0)) - MAX(c.credit_limit), 0) END over_limit
 FROM sales_invoices si JOIN customers c ON c.id = si.customer_id
 WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND si.invoice_date <= CAST(:to AS DATE)
 AND si.net_amount - COALESCE(si.paid_amount,0) > 0.009 AND {B('si')} AND {OPT('si.customer_id','customerId')}
 GROUP BY c.name ORDER BY total DESC""", finance=True)

rep("unbilled-trips","Outstanding & pending","Unbilled completed trips","Completed trips not on any active invoice — revenue at risk. Estimated value from booking rates.",
 ["date","branch","customer","vehicle"],
 [C("trip_no","Trip"),C("trip_date","Date","date"),C("age_days","Age (days)","number"),C("customer","Customer"),C("vehicle","Vehicle"),C("material","Material"),
  C("qty","Qty","qty",True),C("est_taxable","Est. taxable","money",True)],
 f"""SELECT t.trip_number trip_no, t.trip_date, CURRENT_DATE - t.trip_date age_days, c.name customer, v.name vehicle, m.name material,
 COALESCE(NULLIF(td.delivered_quantity,0), td.quantity) qty,
 ROUND(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity) * (COALESCE(NULLIF(td.rate,0), bd.rate, 0) + COALESCE(bd.transport_rate,0)
   + COALESCE(NULLIF(td.loading_charges,0), bd.loading_charge, 0) + COALESCE(NULLIF(td.royalty,0), bd.royalty_rate, 0)), 2) est_taxable
 {TRIP_JOINS} LEFT JOIN LATERAL (SELECT * FROM booking_details x WHERE x.booking_id = t.booking_id AND x.material_id = td.material_id AND x.is_deleted = false LIMIT 1) bd ON true
 {TRIP_WHERE} AND t.status = 'COMPLETED'
 AND NOT EXISTS (SELECT 1 FROM sales_invoice_details sd JOIN sales_invoices si ON si.id = sd.invoice_id
   WHERE sd.trip_id = t.id AND sd.is_deleted = false AND si.is_deleted = false AND si.status <> 'CANCELLED')
 ORDER BY t.trip_date""", finance=True)

rep("pending-approvals","Outstanding & pending","Pending approvals & actions","Everything waiting for someone: bookings, draft invoices, receipts, expenses, fuel, payroll, requests and open jobs.",
 ["branch"],
 [C("module","Module"),C("waiting_for","Waiting for"),C("count","Count","number",True),C("amount","Amount","money",True),C("oldest","Oldest","date")],
 f"""SELECT * FROM (
 SELECT 'Bookings' module, 'Approval' waiting_for, COUNT(*) count, SUM(0) amount, MIN(booking_date) oldest FROM bookings x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('PENDING','DRAFT','ON_HOLD') AND {B('x')}
 UNION ALL SELECT 'Invoices', 'Approval (draft)', COUNT(*), SUM(net_amount), MIN(invoice_date) FROM sales_invoices x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status = 'DRAFT' AND {B('x')}
 UNION ALL SELECT 'Receipts', 'Approval', COUNT(*), SUM(amount_received), MIN(receipt_date) FROM customer_receipts x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('DRAFT','SUBMITTED','PENDING') AND {B('x')}
 UNION ALL SELECT 'Expenses', 'Approval', COUNT(*), SUM(COALESCE(total_amount, amount)), MIN(expense_date) FROM expenses x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('DRAFT','SUBMITTED') AND {B('x')}
 UNION ALL SELECT 'Fuel entries', 'Approval', COUNT(*), SUM(total_amount), MIN(fuel_date) FROM fuel_entries x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('DRAFT','SUBMITTED','PENDING') AND {B('x')}
 UNION ALL SELECT 'Driver payroll', 'Approve / post', COUNT(*), SUM(net_salary_payable), MIN(make_date(pay_year,pay_month,1)) FROM driver_payrolls x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('DRAFT','APPROVED') AND {B('x')}
 UNION ALL SELECT 'Driver payroll', 'Salary payment', COUNT(*), SUM(net_salary_payable), MIN(make_date(pay_year,pay_month,1)) FROM driver_payrolls x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status = 'POSTED' AND {B('x')}
 UNION ALL SELECT 'Maintenance requests', 'Review / approval', COUNT(*), SUM(0), MIN(CAST(requested_at AS DATE)) FROM maintenance_requests x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('OPEN','UNDER_REVIEW','APPROVED') AND {B('x')}
 UNION ALL SELECT 'Work orders', 'Completion', COUNT(*), SUM(COALESCE(estimated_cost,0)), MIN(CAST(opened_at AS DATE)) FROM work_orders x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('OPEN','IN_PROGRESS') AND {B('x')}
 UNION ALL SELECT 'Trips', 'Dispatch / completion', COUNT(*), SUM(0), MIN(trip_date) FROM trips x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status IN ('PLANNED','DISPATCHED') AND {B('x')}
 ) q WHERE count > 0 ORDER BY oldest""")

# ---------------- ACCOUNTS ----------------
rep("day-book","Financial & accounting","Day book (journal)","All journal entries in the period with debit and credit accounts.",
 ["date","branch"],
 [C("voucher_date","Date","date"),C("voucher_no","Voucher"),C("debit","Debit account"),C("credit","Credit account"),C("amount","Amount","money",True),
  C("reference","Reference"),C("narration","Narration")],
 f"""SELECT j.voucher_date, j.voucher_number voucher_no, da.account_code || ' ' || da.account_name debit, ca.account_code || ' ' || ca.account_name credit, j.amount,
 j.reference_number reference, j.description narration
 FROM journal_vouchers j JOIN chart_of_accounts da ON da.id = j.debit_account_id JOIN chart_of_accounts ca ON ca.id = j.credit_account_id
 WHERE j.company_id = :companyId AND j.is_deleted = false AND {DATE('j.voucher_date')} AND {B('j')} ORDER BY j.voucher_date, j.id""", finance=True)

rep("account-ledger","Financial & accounting","Account ledger / cash & bank book","Running balance for one account (default 1000 Cash on Hand; use 1010 for bank, 2000 payables, 1100 receivables).",
 ["date","branch","accountCode"],
 [C("voucher_date","Date","date"),C("voucher_no","Voucher"),C("against","Against account"),C("narration","Narration"),C("debit","Debit","money",True),
  C("credit","Credit","money",True),C("balance","Balance","money")],
 f"""WITH acc AS (SELECT id, account_type, COALESCE(opening_balance,0) ob FROM chart_of_accounts
   WHERE company_id = :companyId AND is_deleted = false AND account_code = COALESCE(CAST(:accountCode AS VARCHAR), '1000') ORDER BY id LIMIT 1),
 mv AS (SELECT j.voucher_date, j.voucher_number, j.id, j.description,
   CASE WHEN j.debit_account_id = acc.id THEN j.amount ELSE 0 END dr, CASE WHEN j.credit_account_id = acc.id THEN j.amount ELSE 0 END cr,
   CASE WHEN j.debit_account_id = acc.id THEN j.credit_account_id ELSE j.debit_account_id END other_id, acc.account_type, acc.ob
   FROM journal_vouchers j, acc WHERE j.company_id = :companyId AND j.is_deleted = false AND (j.debit_account_id = acc.id OR j.credit_account_id = acc.id) AND {B('j')}),
 opening AS (SELECT MAX(acc.ob) + COALESCE(SUM(CASE WHEN acc.account_type IN ('ASSET','EXPENSE') THEN mv.dr - mv.cr ELSE mv.cr - mv.dr END),0) bal
   FROM acc LEFT JOIN mv ON mv.voucher_date < CAST(:from AS DATE))
 SELECT CAST(:from AS DATE) voucher_date, 'OPENING' voucher_no, NULL against, 'Opening balance' narration, NULL debit, NULL credit, (SELECT bal FROM opening) balance
 UNION ALL
 SELECT mv.voucher_date, mv.voucher_number, o.account_code || ' ' || o.account_name, mv.description, mv.dr, mv.cr,
 (SELECT bal FROM opening) + SUM(CASE WHEN mv.account_type IN ('ASSET','EXPENSE') THEN mv.dr - mv.cr ELSE mv.cr - mv.dr END) OVER (ORDER BY mv.voucher_date, mv.id)
 FROM mv JOIN chart_of_accounts o ON o.id = mv.other_id WHERE {DATE('mv.voucher_date')}""", finance=True)

rep("account-balances","Financial & accounting","Account balances (period)","Opening, debits, credits and closing for every ledger account in the period — a quick trial balance with movement.",
 ["date","branch"],
 [C("code","Code"),C("account","Account"),C("type","Type"),C("opening","Opening","money",True),C("debits","Debits","money",True),C("credits","Credits","money",True),C("closing","Closing","money",True)],
 f"""WITH mv AS (SELECT a.id, SUM(CASE WHEN j.debit_account_id = a.id THEN j.amount ELSE 0 END) FILTER (WHERE j.voucher_date < CAST(:from AS DATE)) dr0,
   SUM(CASE WHEN j.credit_account_id = a.id THEN j.amount ELSE 0 END) FILTER (WHERE j.voucher_date < CAST(:from AS DATE)) cr0,
   SUM(CASE WHEN j.debit_account_id = a.id THEN j.amount ELSE 0 END) FILTER (WHERE {DATE('j.voucher_date')}) dr,
   SUM(CASE WHEN j.credit_account_id = a.id THEN j.amount ELSE 0 END) FILTER (WHERE {DATE('j.voucher_date')}) cr
   FROM chart_of_accounts a JOIN journal_vouchers j ON (j.debit_account_id = a.id OR j.credit_account_id = a.id) AND j.is_deleted = false AND {B('j')}
   WHERE a.company_id = :companyId GROUP BY a.id)
 SELECT a.account_code code, a.account_name account, a.account_type type,
 COALESCE(a.opening_balance,0) + CASE WHEN a.account_type IN ('ASSET','EXPENSE') THEN COALESCE(mv.dr0,0) - COALESCE(mv.cr0,0) ELSE COALESCE(mv.cr0,0) - COALESCE(mv.dr0,0) END opening,
 COALESCE(mv.dr,0) debits, COALESCE(mv.cr,0) credits,
 COALESCE(a.opening_balance,0) + CASE WHEN a.account_type IN ('ASSET','EXPENSE') THEN COALESCE(mv.dr0,0)+COALESCE(mv.dr,0)-COALESCE(mv.cr0,0)-COALESCE(mv.cr,0)
   ELSE COALESCE(mv.cr0,0)+COALESCE(mv.cr,0)-COALESCE(mv.dr0,0)-COALESCE(mv.dr,0) END closing
 FROM chart_of_accounts a LEFT JOIN mv ON mv.id = a.id WHERE a.company_id = :companyId AND a.is_deleted = false ORDER BY a.account_code""", finance=True)

# ---------------- MANAGEMENT ----------------
MONTHS="generate_series(date_trunc('month', CAST(:from AS DATE)), date_trunc('month', CAST(:to AS DATE)), interval '1 month')"
rep("monthly-summary","Monthly & management","Monthly business summary","Month by month: trips, quantity, billing, collections, fuel, expenses, maintenance, payroll and operating margin.",
 ["date","branch"],
 [C("month","Month"),C("trips","Trips","number",True),C("qty","Qty delivered","qty",True),C("billed","Billed (taxable)","money",True),C("collected","Collected","money",True),
  C("fuel","Fuel","money",True),C("expenses","Other expenses","money",True),C("maintenance","Maintenance","money",True),C("payroll","Driver payroll","money",True),
  C("margin","Operating margin","money",True),C("margin_pct","Margin %","percent")],
 f"""WITH m AS (SELECT CAST(g AS DATE) m0, CAST(g + interval '1 month' - interval '1 day' AS DATE) m1 FROM {MONTHS} g)
 SELECT to_char(m.m0,'Mon YYYY') AS month,
 (SELECT COUNT(*) FROM trips t WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND t.trip_date BETWEEN m.m0 AND m.m1 AND {B('t')}) trips,
 (SELECT COALESCE(SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)),0) FROM trips t JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false
   WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND t.trip_date BETWEEN m.m0 AND m.m1 AND {B('t')}) qty,
 (SELECT COALESCE(SUM(si.taxable_amount),0) FROM sales_invoices si WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND si.invoice_date BETWEEN m.m0 AND m.m1 AND {B('si')}) billed,
 (SELECT COALESCE(SUM(r.amount_received),0) FROM customer_receipts r WHERE r.company_id = :companyId AND r.is_deleted = false AND r.status = 'APPROVED' AND r.receipt_date BETWEEN m.m0 AND m.m1 AND {B('r')}) collected,
 (SELECT COALESCE(SUM(f.total_amount),0) FROM fuel_entries f WHERE f.company_id = :companyId AND f.is_deleted = false AND f.status = 'APPROVED' AND f.fuel_date BETWEEN m.m0 AND m.m1 AND {B('f')}) fuel,
 (SELECT COALESCE(SUM(COALESCE(e.total_amount, e.amount)),0) FROM expenses e WHERE e.company_id = :companyId AND e.is_deleted = false AND e.status IN ('APPROVED','PAID') AND e.expense_date BETWEEN m.m0 AND m.m1 AND {B('e')}) expenses,
 (SELECT COALESCE(SUM(w.actual_cost),0) FROM work_orders w WHERE w.company_id = :companyId AND w.is_deleted = false AND w.status = 'COMPLETED' AND CAST(w.completed_at AS DATE) BETWEEN m.m0 AND m.m1 AND {B('w')}) maintenance,
 (SELECT COALESCE(SUM(p.gross_amount),0) FROM driver_payrolls p WHERE p.company_id = :companyId AND p.is_deleted = false AND p.status IN ('POSTED','PAID') AND make_date(p.pay_year,p.pay_month,1) = m.m0 AND {B('p')}) payroll
 FROM m ORDER BY m.m0""", finance=True)

rep("management-kpis","Monthly & management","Management KPIs","Headline numbers for the period: revenue, collections, dues, unbilled work, fleet use, fuel, shortage, maintenance and stock.",
 ["date","branch"],
 [C("area","Area"),C("kpi","KPI"),C("value","Value","number"),C("unit","Unit")],
 f"""WITH p AS (SELECT CAST(:from AS DATE) d0, CAST(:to AS DATE) d1)
 SELECT * FROM (
 SELECT 1 o, 'Sales' area, 'Billed (taxable)' kpi, (SELECT COALESCE(SUM(si.taxable_amount),0) FROM sales_invoices si, p WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND si.invoice_date BETWEEN p.d0 AND p.d1 AND {B('si')}) value, '₹' unit
 UNION ALL SELECT 2, 'Sales', 'Collected', (SELECT COALESCE(SUM(r.amount_received),0) FROM customer_receipts r, p WHERE r.company_id = :companyId AND r.is_deleted = false AND r.status = 'APPROVED' AND r.receipt_date BETWEEN p.d0 AND p.d1 AND {B('r')}), '₹'
 UNION ALL SELECT 3, 'Sales', 'Customer dues (all open invoices)', (SELECT COALESCE(SUM(si.net_amount - COALESCE(si.paid_amount,0)),0) FROM sales_invoices si WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND {B('si')}), '₹'
 UNION ALL SELECT 4, 'Sales', 'Dues older than 60 days', (SELECT COALESCE(SUM(si.net_amount - COALESCE(si.paid_amount,0)),0) FROM sales_invoices si WHERE si.company_id = :companyId AND si.is_deleted = false AND {INV_OK} AND si.invoice_date < CURRENT_DATE - 60 AND {B('si')}), '₹'
 UNION ALL SELECT 5, 'Sales', 'Completed trips not invoiced', (SELECT COUNT(*) FROM trips t WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND {B('t')} AND NOT EXISTS (SELECT 1 FROM sales_invoice_details sd JOIN sales_invoices si ON si.id = sd.invoice_id WHERE sd.trip_id = t.id AND sd.is_deleted = false AND si.is_deleted = false AND si.status <> 'CANCELLED')), 'trips'
 UNION ALL SELECT 6, 'Operations', 'Trips completed', (SELECT COUNT(*) FROM trips t, p WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND t.trip_date BETWEEN p.d0 AND p.d1 AND {B('t')}), 'trips'
 UNION ALL SELECT 7, 'Operations', 'Quantity delivered', (SELECT COALESCE(SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)),0) FROM trips t JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false, p WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND t.trip_date BETWEEN p.d0 AND p.d1 AND {B('t')}), 'qty'
 UNION ALL SELECT 8, 'Operations', 'Pending delivery quantity (approved bookings)', (SELECT COALESCE(SUM(GREATEST(bd.quantity - (SELECT COALESCE(SUM(COALESCE(NULLIF(td.delivered_quantity,0), td.quantity)),0) FROM trip_details td JOIN trips t ON t.id = td.trip_id WHERE t.booking_id = bk.id AND td.material_id = bd.material_id AND t.status = 'COMPLETED' AND t.is_deleted = false AND td.is_deleted = false),0)),0) FROM bookings bk JOIN booking_details bd ON bd.booking_id = bk.id AND bd.is_deleted = false WHERE bk.company_id = :companyId AND bk.is_deleted = false AND bk.status = 'APPROVED' AND {B('bk')}), 'qty'
 UNION ALL SELECT 9, 'Operations', 'Weighbridge shortage %', (SELECT ROUND(100 * SUM(td.loaded_quantity - td.delivered_quantity) / NULLIF(SUM(td.loaded_quantity),0), 2) FROM trips t JOIN trip_details td ON td.trip_id = t.id AND td.is_deleted = false, p WHERE t.company_id = :companyId AND t.is_deleted = false AND td.loaded_quantity IS NOT NULL AND td.delivered_quantity IS NOT NULL AND t.trip_date BETWEEN p.d0 AND p.d1 AND {B('t')}), '%'
 UNION ALL SELECT 10, 'Fleet', 'Vehicles used / active', (SELECT ROUND(100.0 * COUNT(DISTINCT t.vehicle_id) / NULLIF((SELECT COUNT(*) FROM vehicles v WHERE v.company_id = :companyId AND v.is_deleted = false AND v.status = 'ACTIVE' AND {B('v')}),0), 1) FROM trips t, p WHERE t.company_id = :companyId AND t.is_deleted = false AND t.status = 'COMPLETED' AND t.trip_date BETWEEN p.d0 AND p.d1 AND {B('t')}), '%'
 UNION ALL SELECT 11, 'Fleet', 'Average km per litre', (SELECT ROUND(SUM(CASE WHEN f.previous_odometer IS NOT NULL THEN f.current_odometer - f.previous_odometer ELSE 0 END) / NULLIF(SUM(f.fuel_quantity),0), 2) FROM fuel_entries f, p WHERE f.company_id = :companyId AND f.is_deleted = false AND f.status = 'APPROVED' AND f.fuel_date BETWEEN p.d0 AND p.d1 AND {B('f')}), 'km/L'
 UNION ALL SELECT 12, 'Fleet', 'Fuel cost', (SELECT COALESCE(SUM(f.total_amount),0) FROM fuel_entries f, p WHERE f.company_id = :companyId AND f.is_deleted = false AND f.status = 'APPROVED' AND f.fuel_date BETWEEN p.d0 AND p.d1 AND {B('f')}), '₹'
 UNION ALL SELECT 13, 'Maintenance', 'Open work orders', (SELECT COUNT(*) FROM work_orders w WHERE w.company_id = :companyId AND w.is_deleted = false AND w.status IN ('OPEN','IN_PROGRESS') AND {B('w')}), 'jobs'
 UNION ALL SELECT 14, 'Maintenance', 'Maintenance cost', (SELECT COALESCE(SUM(w.actual_cost),0) FROM work_orders w, p WHERE w.company_id = :companyId AND w.is_deleted = false AND w.status = 'COMPLETED' AND CAST(w.completed_at AS DATE) BETWEEN p.d0 AND p.d1 AND {B('w')}), '₹'
 UNION ALL SELECT 15, 'Stores', 'Stock value', (SELECT COALESCE(ROUND(SUM(s.available_quantity * s.average_cost), 2),0) FROM warehouse_stock s WHERE s.company_id = :companyId AND s.is_deleted = false AND {B('s')}), '₹'
 UNION ALL SELECT 16, 'Stores', 'Parts to reorder', (SELECT COUNT(*) FROM warehouse_stock s JOIN spare_parts sp ON sp.id = s.spare_part_id WHERE s.company_id = :companyId AND s.is_deleted = false AND sp.reorder_level IS NOT NULL AND s.available_quantity <= sp.reorder_level AND {B('s')}), 'parts'
 UNION ALL SELECT 17, 'People', 'Driver salary awaiting payment', (SELECT COALESCE(SUM(x.net_salary_payable),0) FROM driver_payrolls x WHERE x.company_id = :companyId AND x.is_deleted = false AND x.status = 'POSTED' AND {B('x')}), '₹'
 UNION ALL SELECT 18, 'People', 'Driver advances outstanding', (SELECT COALESCE(SUM(a.amount - a.recovered_amount),0) FROM driver_advances a WHERE a.company_id = :companyId AND a.is_deleted = false AND a.status = 'ISSUED' AND {B('a')}), '₹'
 UNION ALL SELECT 19, 'Compliance', 'Documents expired or due in 30 days', (SELECT (SELECT COUNT(*) FROM vehicles v WHERE v.company_id = :companyId AND v.is_deleted = false AND {B('v')} AND (v.insurance_expiry_date <= CURRENT_DATE + 30 OR v.fitness_expiry_date <= CURRENT_DATE + 30 OR v.permit_expiry_date <= CURRENT_DATE + 30)) + (SELECT COUNT(*) FROM drivers d WHERE d.company_id = :companyId AND d.is_deleted = false AND {B('d')} AND d.license_expiry_date <= CURRENT_DATE + 30)), 'items'
 ) k ORDER BY o""", finance=True)

for r in R:
    if r['key']=='monthly-summary':
        r['sql']="SELECT x.*, x.billed - x.fuel - x.expenses - x.maintenance - x.payroll AS margin, ROUND(100 * (x.billed - x.fuel - x.expenses - x.maintenance - x.payroll) / NULLIF(x.billed,0), 1) AS margin_pct FROM (" + r['sql'].replace(' ORDER BY m.m0','') + ") x"
import os
out=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..','transport-backend','src','main','resources','reports','report-catalog.json')
json.dump(R, open(out,'w'), indent=1, ensure_ascii=False)
json.dump(R, open('/tmp/report-catalog.json','w'), indent=1, ensure_ascii=False)
print(len(R))
