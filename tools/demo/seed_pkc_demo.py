#!/usr/bin/env python3
"""
PKC Transport — complete, connected demo data (Perambalur / Thanirpandal region).

Loads data THROUGH THE TRANSAFLOW API as the PKC Company Admin, so every record follows the real business flow:
document numbers, GST, stock average cost, accounting vouchers, payroll slabs and all validations are produced by the
application itself (direct SQL inserts would skip the accounting and stock postings and leave the reports wrong).

Usage:
    python3 seed_pkc_demo.py --url https://<your-backend>/api/v1 --username <pkc admin> --password <password>

Only the logged-in company (PKC Transport) is touched. The script stops if PKC demo codes already exist (run once).
Python 3.8+, no extra packages.
"""
import argparse
import json
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

TODAY = date.today()


def d(days_ago: int) -> str:
    return (TODAY - timedelta(days=days_ago)).isoformat()


class Api:
    def __init__(self, base, username, password):
        self.base = base.rstrip('/')
        self.token = None
        self.errors = []
        r = self.call('POST', '/auth/login', {'username': username, 'password': password}, auth=False)
        if not r or not r.get('token'):
            sys.exit('Login failed — check URL, username and password.')
        self.token = r['token']
        self.company_id = r.get('companyId')
        self.branch_id = r.get('branchId')
        self.roles = r.get('roles') or []
        print(f"Logged in as {username} — company {r.get('companyName') or self.company_id} ({r.get('companyShortName')}), roles {self.roles}")

    def call(self, method, path, body=None, auth=True, quiet=False):
        req = urllib.request.Request(self.base + path, method=method)
        req.add_header('Content-Type', 'application/json')
        if auth and self.token:
            req.add_header('Authorization', 'Bearer ' + self.token)
        data = json.dumps(body).encode() if body is not None else None
        try:
            with urllib.request.urlopen(req, data=data, timeout=60) as resp:
                raw = resp.read().decode() or '{}'
        except urllib.error.HTTPError as e:
            raw = e.read().decode() or '{}'
        try:
            js = json.loads(raw)
        except ValueError:
            js = {'success': False, 'message': raw[:200]}
        if isinstance(js, dict) and js.get('success') is False:
            msg = f"{method} {path}: {js.get('message')} {(js.get('errors') or [''])[0]}"
            self.errors.append(msg)
            if not quiet:
                print('  ! ' + msg)
            return None
        return js.get('data') if isinstance(js, dict) and 'data' in js else js

    def get(self, path):
        return self.call('GET', path)

    def post(self, path, body=None, quiet=False):
        return self.call('POST', path, body if body is not None else {}, quiet=quiet)

    def put(self, path, body):
        return self.call('PUT', path, body)


def content(page):
    if page is None:
        return []
    return page.get('content', page) if isinstance(page, dict) else page


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--url', required=True, help='Backend API base, e.g. https://erp-api.onrender.com/api/v1')
    ap.add_argument('--username', required=True)
    ap.add_argument('--password', required=True)
    a = ap.parse_args()
    api = Api(a.url, a.username, a.password)
    if 'SUPER_ADMIN' in api.roles:
        sys.exit('Log in with the PKC Transport Company Admin, not the platform admin.')

    existing = content(api.get('/customers?search=PKC-C01&size=5'))
    if any(c.get('code') == 'PKC-C01' for c in existing):
        sys.exit('PKC demo data already exists for this company — nothing done.')

    cid = api.company_id

    # ------------------------------------------------------------------ 1. Company setup
    print('\n[1] Branches, pay slabs')
    branches = content(api.get(f'/branches?companyId={cid}&size=50'))
    ho = branches[0]
    ho.update({'name': 'Head Office — Perambalur', 'gstNumber': '33AAKFP4521M1Z6', 'manager': 'P. Kannan',
               'phone': '9443012345', 'email': 'office@pkctransport.in',
               'address': '14, Trichy Main Road, Near New Bus Stand, Perambalur 621212'})
    api.put(f"/branches/{ho['id']}", ho)
    tpl = api.post('/branches', {'code': 'TPL', 'name': 'Thanirpandal Yard', 'gstNumber': '33AAKFP4521M1Z6',
                                 'manager': 'R. Senthil', 'phone': '9443098765', 'status': 'ACTIVE',
                                 'address': 'NH-81 (Trichy–Chidambaram Road), Thanirpandal, Perambalur Dt 621115'})
    api.put('/driver-pay-slabs', [{'tripsFrom': 1, 'tripsTo': 1, 'dailyAmount': 500},
                                  {'tripsFrom': 2, 'tripsTo': None, 'dailyAmount': 1000}])

    # ------------------------------------------------------------------ 2. Materials, quarries, loading points
    print('[2] Materials, quarries, loading points')
    mats = {}
    for code, name, rate, tr, roy, load, gst in [
        ('MSAND', 'M-Sand (Manufactured Sand)', 950, 320, 60, 40, 5),
        ('PSAND', 'P-Sand (Plastering Sand)', 1150, 320, 60, 40, 5),
        ('BM20', 'Blue Metal 20 mm', 820, 300, 55, 35, 5),
        ('BM40', 'Blue Metal 40 mm', 780, 300, 55, 35, 5),
        ('JALLI6', 'Jalli 6 mm (Chips)', 900, 300, 55, 35, 5),
        ('GRAVEL', 'Red Gravel (Earth Fill)', 450, 260, 40, 30, 5)]:
        m = api.post('/materials', {'code': code, 'name': name, 'status': 'ACTIVE', 'defaultRate': rate})
        if m:
            mats[code] = {'id': m['id'], 'rate': rate, 'tr': tr, 'roy': roy, 'load': load, 'gst': gst}
    quarries = []
    for code, name, place, owner, phone in [
        ('Q-TPL', 'Thanirpandal Blue Metal Quarry', 'Thanirpandal, Perambalur Dt', 'S. Rajendran', '9842011111'),
        ('Q-KUN', 'Kunnam Stone Crusher', 'Kunnam, Perambalur Dt', 'M. Palanisamy', '9842022222'),
        ('Q-PDL', 'Padalur M-Sand Unit', 'Padalur, Perambalur Dt', 'K. Velu', '9842033333'),
        ('Q-ALT', 'Alathur Gate Quarry', 'Alathur, Perambalur Dt', 'T. Murugesan', '9842044444'),
        ('Q-VPR', 'Veppur Red Gravel Pit', 'Veppur, Perambalur Dt', 'A. Selvam', '9842055555')]:
        q = api.post('/quarries', {'code': code, 'name': name, 'locationAddress': place, 'ownerName': owner,
                                   'contactNumber': phone, 'workingHours': '6:00 AM – 7:00 PM', 'status': 'ACTIVE'})
        if q:
            quarries.append(q)
    for code, point, charge in [('LP-TPL', 'Thanirpandal Crusher Bay 1', 35), ('LP-KUN', 'Kunnam Crusher Yard', 35),
                                ('LP-PDL', 'Padalur Sand Wash Plant', 40), ('LP-ALT', 'Alathur Pit Gate', 30),
                                ('LP-VPR', 'Veppur Gravel Pit Ramp', 30)]:
        api.post('/loading-locations', {'code': code, 'name': point, 'locationCode': code, 'loadingPoint': point,
                                        'loadingCharges': charge, 'status': 'ACTIVE'})

    # ------------------------------------------------------------------ 3. Customers & sites
    print('[3] Customers and delivery sites')
    customers = []
    for i, (name, gstin, town, phone, limit, site) in enumerate([
        ('Sri Murugan Constructions', '33AAGFS1234K1Z5', 'Perambalur', '9443100001', 500000, 'Collectorate Road Apartment Site, Perambalur'),
        ('Aravind Builders', '33AAHFA2345L1Z2', 'Ariyalur', '9443100002', 400000, 'Ariyalur Bypass Villa Project'),
        ('Ramco Infra Projects', '33AABCR3456M1Z8', 'Tiruchirappalli', '9443100003', 800000, 'Samayapuram NH Service Road Work'),
        ('Lakshmi Housing', '33AAIFL4567N1Z4', 'Thuraiyur', '9443100004', 300000, 'Thuraiyur Lakshmi Nagar Layout'),
        ('Sakthi Ready Mix Concrete', '33AAJCS5678P1Z1', 'Siruganur', '9443100005', 900000, 'Siruganur RMC Plant'),
        ('Kaveri Road Contractors', '33AAKFK6789Q1Z7', 'Lalgudi', '9443100006', 600000, 'Lalgudi–Pullambadi Road Widening'),
        ('Thangam Builders', '33AALFT7890R1Z3', 'Kunnam', '9443100007', 250000, 'Kunnam Panchayat School Building'),
        ('Arul Promoters', '33AAMFA8901S1Z9', 'Veppanthattai', '9443100008', 350000, 'Veppanthattai Arul Nagar Plots'),
        ('Selvam Brick Works', '33AANFS9012T1Z6', 'Labbaikudikadu', '9443100009', 200000, 'Labbaikudikadu Kiln Yard'),
        ('Pondy Civil Works', '34AAOFP0123U1Z2', 'Puducherry', '9443100010', 500000, 'Villianur Industrial Shed, Puducherry')]):
        c = api.post('/customers', {'code': f'PKC-C{i + 1:02d}', 'name': name, 'gstNumber': gstin, 'phone': phone,
                                    'email': f"accounts{i + 1}@example.com", 'address': f'{town}',
                                    'creditLimit': limit, 'status': 'ACTIVE'})
        if not c:
            continue
        s = api.post(f"/customers/{c['id']}/delivery-sites", {'siteCode': f'S{i + 1:02d}', 'siteName': site,
                                                              'address': site, 'managerName': 'Site Engineer',
                                                              'code': f'S{i + 1:02d}', 'name': site})
        c['site'] = s
        customers.append(c)

    # ------------------------------------------------------------------ 4. Vehicles, drivers, assignments
    print('[4] Vehicles, drivers, assignments')
    vehicles, drivers = [], []
    for i, (reg, brand, model, owner, ins, fit, per, odo) in enumerate([
        ('TN46AB1234', 'Tata', 'Signa 2823.K 10-wheel Tipper', 'SELF', 210, 320, 150, 84250),
        ('TN46AC5678', 'Ashok Leyland', '2820 Tipper', 'SELF', 25, 190, 300, 126400),   # insurance due soon
        ('TN46AD2468', 'BharatBenz', '2828C Tipper', 'SELF', 140, 12, 260, 58900),       # fitness due soon
        ('TN46AE1357', 'Tata', 'LPK 2518 Tipper', 'SELF', 300, 280, 20, 142700),         # permit due soon
        ('TN46AF9876', 'Eicher', 'Pro 6028T Tipper', 'SELF', 180, 240, 200, 36500),
        ('TN46AG4321', 'Ashok Leyland', '1616 Tipper (attached)', 'HIRED', 90, 120, 90, 201300)]):
        v = api.post('/vehicles', {'code': reg, 'name': f'{reg} — {brand}', 'brand': brand, 'model': model,
                                   'ownerType': owner, 'ownerName': 'PKC Transport' if owner == 'SELF' else 'K. Mani (attached)',
                                   'chassisNumber': f'MAT{445000 + i * 7}', 'engineNumber': f'B6{880000 + i * 11}',
                                   'insuranceExpiryDate': (TODAY + timedelta(days=ins)).isoformat(),
                                   'fitnessExpiryDate': (TODAY + timedelta(days=fit)).isoformat(),
                                   'permitExpiryDate': (TODAY + timedelta(days=per)).isoformat(),
                                   'purchaseDate': (TODAY - timedelta(days=700 + i * 200)).isoformat(),
                                   'currentOdometerKm': odo, 'status': 'ACTIVE',
                                   'branchId': (tpl or {}).get('id') if i in (3, 4) else ho['id']})
        if v:
            v['odo'] = odo
            vehicles.append(v)
    for i, (name, lic, exp_days, phone, basic) in enumerate([
        ('K. Murugan', 'TN46 20110004512', 900, '9500100001', 0),
        ('P. Selvaraj', 'TN46 20130008841', 1400, '9500100002', 0),
        ('S. Ramesh', 'TN46 20150011237', 18, '9500100003', 0),     # licence expiring soon
        ('M. Karthik', 'TN46 20170015563', 1800, '9500100004', 6000),
        ('R. Arumugam', 'TN46 20090002219', 600, '9500100005', 6000),
        ('V. Senthil', 'TN46 20190019908', 2100, '9500100006', 0)]):
        dr = api.post('/drivers', {'code': f'PKC-D{i + 1:02d}', 'name': name, 'licenseNumber': lic,
                                   'licenseExpiryDate': (TODAY + timedelta(days=exp_days)).isoformat(),
                                   'phoneNumber': phone, 'status': 'ACTIVE'})
        if dr:
            api.post(f"/drivers/{dr['id']}/salary", {'basicSalary': basic, 'overtimeRate': 0, 'advanceTaken': 0})
            drivers.append(dr)
    for v, dr in zip(vehicles, drivers):
        api.post(f"/vehicles/{v['id']}/driver/{dr['id']}")

    # ------------------------------------------------------------------ 5. Suppliers, stores, parts, stock
    print('[5] Suppliers, warehouses, spare parts, stock')
    suppliers = []
    for i, (name, gstin, days, phone) in enumerate([
        ('Perambalur Auto Spares', '33AAPFP1111A1Z1', 30, '9443200001'), ('Sri Balaji Tyres', '33AAQFS2222B1Z2', 45, '9443200002'),
        ('Thanirpandal Diesel Works', '33AARFT3333C1Z3', 15, '9443200003'), ('Kannan Lathe & Engineering', '33AASFK4444D1Z4', 30, '9443200004'),
        ('Sri Vari Fuels (IOCL)', '33AATFS5555E1Z5', 7, '9443200005'), ('Ariyalur Hydraulics', '33AAUFA6666F1Z6', 30, '9443200006'),
        ('Trichy Truck Parts', '33AAVFT7777G1Z7', 30, '9443200007'), ('Vel Batteries', '33AAWFV8888H1Z8', 30, '9443200008'),
        ('Siva Welding Works', '33AAXFS9999J1Z9', 0, '9443200009'), ('Murugan Tarpaulins', '33AAYFM1010K1Z1', 15, '9443200010')]):
        s = api.post('/suppliers', {'code': f'PKC-S{i + 1:02d}', 'name': name, 'gstNumber': gstin, 'creditDays': days,
                                    'phone': phone, 'address': 'Perambalur', 'status': 'ACTIVE', 'companyId': cid})
        if s:
            suppliers.append(s)
    uoms = api.get('/spare-parts/uoms') or []
    uom_id = (uoms[0] if uoms else {}).get('id')
    wh_main = api.post('/warehouses', {'code': 'WH-PBR', 'name': 'Main Store — Perambalur', 'status': 'ACTIVE', 'branchId': ho['id']})
    wh_tpl = api.post('/warehouses', {'code': 'WH-TPL', 'name': 'Thanirpandal Yard Store', 'status': 'ACTIVE',
                                      'branchId': (tpl or ho)['id']})
    parts = {}
    for code, name, rate, reorder in [
        ('OF-01', 'Engine Oil Filter', 450, 6), ('DF-01', 'Diesel Filter', 380, 6), ('AF-01', 'Air Filter Element', 1250, 4),
        ('EO-15W40', 'Engine Oil 15W40 (litre)', 310, 40), ('BL-SET', 'Brake Liner Set', 3200, 4), ('CP-01', 'Clutch Plate', 7800, 2),
        ('TY-1020', 'Tyre 10.00-20 (Nylon)', 18500, 4), ('HH-01', 'Hydraulic Hose (tipper)', 1650, 4),
        ('BT-12V', 'Battery 12V 150Ah', 11200, 2), ('FB-01', 'Fan Belt', 520, 6), ('GR-01', 'Grease (kg)', 260, 10)]:
        p = api.post('/spare-parts', {'code': code, 'name': name, 'defaultUomId': uom_id, 'defaultRate': rate,
                                      'reorderLevel': reorder, 'status': 'ACTIVE'})
        if p:
            parts[code] = {'id': p['id'], 'rate': rate}
    wm = (wh_main or {}).get('id')
    wt = (wh_tpl or {}).get('id')
    # opening stock (valued) — reorder-level parts deliberately low for the reorder report
    for code, qty, w in [('OF-01', 10, wm), ('DF-01', 10, wm), ('AF-01', 4, wm), ('EO-15W40', 120, wm), ('BL-SET', 6, wm),
                         ('CP-01', 2, wm), ('TY-1020', 6, wm), ('HH-01', 3, wm), ('BT-12V', 2, wm), ('FB-01', 12, wm),
                         ('GR-01', 25, wt), ('OF-01', 4, wt), ('EO-15W40', 40, wt)]:
        if code in parts and w:
            api.post('/inventory/stock/opening-balance', {'warehouseId': w, 'sparePartId': parts[code]['id'],
                                                          'quantity': qty, 'unitRate': parts[code]['rate']})
    sup = {s['code']: s['id'] for s in suppliers}
    for code, qty, rate, supplier, mode, bill in [
        ('OF-01', 12, 460, 'PKC-S01', 'CREDIT', 'PAS/2627/0412'), ('DF-01', 12, 390, 'PKC-S01', 'CREDIT', 'PAS/2627/0412'),
        ('TY-1020', 4, 18200, 'PKC-S02', 'CREDIT', 'SBT-8841'), ('EO-15W40', 60, 305, 'PKC-S07', 'CASH', 'TTP-1109'),
        ('BL-SET', 4, 3150, 'PKC-S07', 'CREDIT', 'TTP-1123'), ('HH-01', 6, 1600, 'PKC-S06', 'BANK', 'AH-552'),
        ('BT-12V', 2, 11000, 'PKC-S08', 'CREDIT', 'VB-3021'), ('FB-01', 10, 510, 'PKC-S01', 'CASH', 'PAS/2627/0450'),
        ('GR-01', 20, 250, 'PKC-S04', 'CASH', 'KLE-771'), ('AF-01', 4, 1240, 'PKC-S03', 'CREDIT', 'TDW-2210')]:
        if code in parts and wm:
            api.post('/inventory/stock/receipt', {'warehouseId': wm, 'sparePartId': parts[code]['id'], 'quantity': qty,
                                                  'unitRate': rate, 'supplierId': sup.get(supplier), 'paymentMode': mode,
                                                  'referenceNumber': bill})

    # ------------------------------------------------------------------ 6. Bookings
    print('[6] Bookings')
    plan = [  # customer idx, material, qty, days ago, action
        (0, 'MSAND', 200, 68, 'APPROVE'), (1, 'BM20', 150, 66, 'APPROVE'), (2, 'GRAVEL', 300, 64, 'APPROVE'),
        (3, 'PSAND', 100, 60, 'APPROVE'), (4, 'BM20', 250, 58, 'APPROVE'), (5, 'BM40', 200, 55, 'APPROVE'),
        (6, 'MSAND', 80, 50, 'APPROVE'), (7, 'JALLI6', 60, 45, 'APPROVE'), (8, 'GRAVEL', 120, 40, 'APPROVE'),
        (9, 'MSAND', 90, 35, 'APPROVE'), (0, 'PSAND', 60, 20, 'APPROVE'), (4, 'MSAND', 150, 12, 'APPROVE'),
        (2, 'BM20', 100, 6, 'PENDING'), (6, 'GRAVEL', 40, 4, 'REJECT')]
    bookings = []
    for ci, mc, qty, ago, action in plan:
        if ci >= len(customers) or mc not in mats:
            continue
        c, m = customers[ci], mats[mc]
        b = api.post('/bookings', {'customer': {'id': c['id']}, 'bookingDate': d(ago), 'priority': 'MEDIUM',
                                   'deliverySite': {'id': c['site']['id']} if c.get('site') else None,
                                   'details': [{'material': {'id': m['id']}, 'quantity': qty, 'rate': m['rate'],
                                                'transportRate': m['tr'], 'royaltyRate': m['roy'], 'loadingCharge': m['load'],
                                                'gstPercentage': m['gst']}]})
        if not b:
            continue
        if action == 'APPROVE':
            api.post(f"/bookings/{b['id']}/approve")
        elif action == 'REJECT':
            api.post(f"/bookings/{b['id']}/reject")
        b.update({'ci': ci, 'mc': mc, 'qty': qty, 'ago': ago, 'action': action})
        bookings.append(b)

    # ------------------------------------------------------------------ 7. Trips (dispatch, complete, weighbridge)
    print('[7] Trips')
    trips = []
    loads = {'MSAND': 18, 'PSAND': 16, 'BM20': 20, 'BM40': 20, 'JALLI6': 15, 'GRAVEL': 22}
    qmap = {'MSAND': 2, 'PSAND': 2, 'BM20': 0, 'BM40': 1, 'JALLI6': 3, 'GRAVEL': 4}
    k = 0
    for b in [x for x in bookings if x['action'] == 'APPROVE']:
        per = loads[b['mc']]
        n = max(2, min(6, int(b['qty'] * (0.6 if b['ago'] > 20 else 0.4)) // per))
        for t in range(n):
            ago = max(1, b['ago'] - 2 - t * max(1, (b['ago'] - 3) // max(n, 1)))
            vi = k % len(vehicles)
            v, dr = vehicles[vi], drivers[vi]
            q = quarries[qmap[b['mc']] % len(quarries)] if quarries else None
            tr = api.post('/trips', {'booking': {'id': b['id']}, 'tripDate': d(ago), 'vehicle': {'id': v['id']},
                                     'driver': {'id': dr['id']}, 'quarry': {'id': q['id']} if q else None,
                                     'remarks': f"{b['mc']} to {customers[b['ci']]['name']}",
                                     'details': [{'material': {'id': mats[b['mc']]['id']}, 'quantity': per}]}, quiet=True)
            k += 1
            if not tr:
                continue
            recent = ago <= 2
            if recent and t % 2 == 0:
                trips.append({'id': tr['id'], 'status': 'PLANNED', 'ago': ago, 'b': b, 'v': v, 'dr': dr})
                continue
            api.post(f"/trips/{tr['id']}/dispatch")
            if recent:
                trips.append({'id': tr['id'], 'status': 'DISPATCHED', 'ago': ago, 'b': b, 'v': v, 'dr': dr})
                continue
            api.post(f"/trips/{tr['id']}/complete")
            loaded = per + 0.2
            delivered = round(per - (0.35 if (k % 5 == 0) else 0.1), 2)
            tr['details'][0].update({'loadedQuantity': loaded, 'deliveredQuantity': delivered})
            api.put(f"/trips/{tr['id']}", {'booking': {'id': b['id']}, 'tripDate': d(ago), 'vehicle': {'id': v['id']},
                                          'driver': {'id': dr['id']}, 'quarry': {'id': q['id']} if q else None,
                                          'details': tr['details']})
            trips.append({'id': tr['id'], 'status': 'COMPLETED', 'ago': ago, 'b': b, 'v': v, 'dr': dr, 'qty': delivered})
    # close one booking early (customer needs no more loads)
    closable = [b for b in bookings if b['action'] == 'APPROVE' and b['ci'] == 7]
    if closable:
        api.post(f"/bookings/{closable[0]['id']}/close", quiet=True)
    done = [t for t in trips if t['status'] == 'COMPLETED']
    print(f"    trips: {len(trips)} ({len(done)} completed)")

    # ------------------------------------------------------------------ 8. Fuel and expenses (linked to trips)
    print('[8] Fuel entries and expenses')
    odo = {v['id']: v['odo'] for v in vehicles}
    for i, t in enumerate(sorted(done, key=lambda x: -x['ago'])):
        if i % 2:
            continue
        vid = t['v']['id']
        prev = odo[vid]
        cur = prev + 180 + (i % 4) * 35
        odo[vid] = cur
        litres = round((cur - prev) / 3.6, 1)
        f = api.post('/fuel', {'vehicle': {'id': vid}, 'driver': {'id': t['dr']['id']}, 'trip': {'id': t['id']},
                               'fuelDate': d(t['ago']), 'fuelStation': 'Sri Vari Fuels (IOCL), Thanirpandal' if i % 3 else 'HP Petrol Bunk, Perambalur Bypass',
                               'fuelQuantity': litres, 'ratePerLitre': 92.4, 'totalAmount': round(litres * 92.4, 2),
                               'paymentMethod': 'CASH' if i % 4 else 'BANK_TRANSFER', 'invoiceNumber': f'FB-{7000 + i}',
                               'previousOdometer': prev, 'currentOdometer': cur})
        if f and i < len(done) - 3:
            api.post(f"/fuel/{f['id']}/approve")
    for i, t in enumerate(sorted(done, key=lambda x: -x['ago'])):
        cat, amt, desc = [('TOLL', 285, 'Thirumandurai toll plaza (NH-38)'), ('DRIVER_BATA', 300, 'Driver bata — trip'),
                          ('PARKING', 100, 'Site parking / weighbridge fee'), ('TOLL', 145, 'Samayapuram toll')][i % 4]
        if i % 3 == 2:
            continue
        e = api.post('/expenses', {'expenseDate': d(t['ago']), 'category': cat, 'vehicle': {'id': t['v']['id']},
                                   'driver': {'id': t['dr']['id']}, 'trip': {'id': t['id']}, 'amount': amt, 'gstAmount': 0,
                                   'totalAmount': amt, 'paymentMethod': 'CASH', 'description': desc})
        if e and i < len(done) - 2:
            api.post(f"/expenses/{e['id']}/approve")
    for ago, cat, amt, gst, desc, vi in [(50, 'INSURANCE', 48500, 8730, 'Vehicle insurance renewal — TN46AF9876', 4),
                                         (30, 'OFFICE', 3200, 576, 'Office stationery & printer cartridge', None),
                                         (25, 'OFFICE', 1800, 0, 'Office electricity bill — Perambalur', None),
                                         (15, 'VEHICLE_REPAIR', 2400, 432, 'Puncture & tube change on road', 1),
                                         (8, 'OFFICE', 1500, 0, 'Mobile recharge — dispatch phones', None)]:
        e = api.post('/expenses', {'expenseDate': d(ago), 'category': cat, 'amount': amt, 'gstAmount': gst,
                                   'totalAmount': amt + gst, 'paymentMethod': 'BANK_TRANSFER', 'description': desc,
                                   'vehicle': {'id': vehicles[vi]['id']} if vi is not None else None})
        if e and ago > 10:
            api.post(f"/expenses/{e['id']}/approve")

    # ------------------------------------------------------------------ 9. Invoices (multi-trip) and receipts
    print('[9] Invoices and receipts')
    by_customer = {}
    for t in done:
        if t['ago'] < 4:
            continue                      # keep the last few days unbilled (Unbilled trips report)
        by_customer.setdefault(t['b']['ci'], []).append(t)
    invoices = []
    for ci, ts in by_customer.items():
        ts = sorted(ts, key=lambda x: -x['ago'])
        for chunk in [ts[i:i + 3] for i in range(0, len(ts), 3)]:
            inv = api.post('/invoices/from-trips', {'tripIds': [x['id'] for x in chunk]})
            if not inv:
                continue
            inv_date = d(max(1, min(x['ago'] for x in chunk) - 1))
            inv['invoiceDate'] = inv_date
            inv['placeOfSupply'] = None
            upd = api.put(f"/invoices/{inv['id']}", inv) or inv
            invoices.append({'id': inv['id'], 'ci': ci, 'date': inv_date, 'net': upd.get('netAmount')})
    for n, inv in enumerate(invoices):
        if n % 7 == 6:
            continue                      # leave some drafts (Pending approvals report)
        api.post(f"/invoices/{inv['id']}/approve")
        inv['approved'] = True
    approved = [i for i in invoices if i.get('approved')]
    for n, inv in enumerate(approved):
        if n % 3 == 2:
            continue                      # unpaid invoices for the ageing report
        full = n % 3 == 0
        amount = round(float(inv['net'] or 0) * (1 if full else 0.5), 2)
        if amount <= 0:
            continue
        pay_ago = max(0, (TODAY - date.fromisoformat(inv['date'])).days - 7)
        r = api.post('/receipts', {'customerId': customers[inv['ci']]['id'], 'receiptDate': d(pay_ago),
                                   'amountReceived': amount, 'advanceAmount': 0,
                                   'paymentMethod': ['BANK_TRANSFER', 'UPI', 'CHEQUE', 'CASH'][n % 4],
                                   'referenceNumber': f'UTR{88100000 + n}', 'remarks': 'Payment against invoice',
                                   'allocations': [{'invoiceId': inv['id'], 'amount': amount}]})
        if r and n % 5 != 4:
            api.post(f"/receipts/{r['id']}/approve")

    # ------------------------------------------------------------------ 10. Driver advances and payroll
    print('[10] Driver advances and payroll')
    for i, dr in enumerate(drivers):
        adv = api.post('/driver-advances', {'driver': {'id': dr['id']}, 'amount': [2000, 1500, 3000, 1000, 2500, 1500][i],
                                            'advanceDate': d(45 - i * 3), 'paymentMethod': 'CASH',
                                            'remarks': 'Festival / family advance'})
    months = sorted({(date.fromisoformat(d(t['ago'])).year, date.fromisoformat(d(t['ago'])).month) for t in done
                     if date.fromisoformat(d(t['ago'])).replace(day=1) < TODAY.replace(day=1)})
    for (y, mth) in months:
        last = (mth == months[-1][1] and y == months[-1][0])
        for i, dr in enumerate(drivers):
            body = {'driverId': dr['id'], 'payYear': y, 'payMonth': mth, 'allowanceAmount': 500 if i % 2 == 0 else 0,
                    'advanceAdjustment': 500 if not last else 1000, 'description': f'Salary {mth:02d}/{y}',
                    'deductions': [{'deductionType': 'FINE', 'amount': 200, 'remarks': 'Late reporting at quarry'}] if i == 2 else []}
            p = api.post('/driver-payrolls/generate', body, quiet=True)
            if not p:
                body['advanceAdjustment'] = 0
                p = api.post('/driver-payrolls/generate', body)
            if not p:
                continue
            api.post(f"/driver-payrolls/{p['id']}/approve")
            api.post(f"/driver-payrolls/{p['id']}/post")
            if not last or i % 2 == 0:
                api.post(f"/driver-payrolls/{p['id']}/pay", {'paymentMethod': 'BANK_TRANSFER',
                                                              'paymentDate': min(TODAY, date(y, mth, 1) + timedelta(days=36)).isoformat(),
                                                              'paymentReference': f'SAL-{y}{mth:02d}-{i + 1}'})

    # ------------------------------------------------------------------ 11. Maintenance requests & work orders
    print('[11] Maintenance requests and work orders')
    issues = [('Brake noise on front axle', 'HIGH', 'BL-SET', 1, 1800), ('Engine oil & filter service due', 'MEDIUM', 'EO-15W40', 12, 600),
              ('Hydraulic hose leak — tipper not lifting', 'HIGH', 'HH-01', 1, 900), ('Clutch slipping on gradient', 'HIGH', 'CP-01', 1, 3500),
              ('Front tyre worn out', 'MEDIUM', 'TY-1020', 2, 400), ('Battery weak — hard starting', 'MEDIUM', 'BT-12V', 1, 200),
              ('Air filter choked — dust from quarry', 'LOW', 'AF-01', 1, 300), ('Fan belt squeal', 'LOW', 'FB-01', 1, 250),
              ('Diesel filter change', 'MEDIUM', 'DF-01', 1, 300), ('Grease all points', 'LOW', 'GR-01', 3, 400)]
    for i, (title, prio, part, qty, labour) in enumerate(issues):
        v = vehicles[i % len(vehicles)]
        mr = api.post('/maintenance-requests', {'vehicleId': v['id'], 'title': title, 'description': title + ' — reported by driver',
                                                'priority': prio})
        if not mr:
            continue
        if i == 9:
            continue                                   # stays OPEN
        api.post(f"/maintenance-requests/{mr['id']}/review", {})
        api.post(f"/maintenance-requests/{mr['id']}/approve")
        if i == 8:
            api.post(f"/maintenance-requests/{mr['id']}/cancel", {'cancellationReason': 'Fixed on the road by driver'})
            continue
        conv = api.post(f"/maintenance-requests/{mr['id']}/convert", {}) or {}
        wo_id = conv.get('workOrderId')
        if not wo_id:
            continue
        wo = api.post(f'/work-orders/{wo_id}/parts', {'sparePartId': parts[part]['id'], 'quantity': qty,
                                                       'unitRate': parts[part]['rate']}) if part in parts else None
        line = ((wo or {}).get('parts') or [{}])[-1].get('id')
        api.post(f'/work-orders/{wo_id}/labour', {'description': 'Mechanic labour', 'hours': 2, 'rate': labour / 2})
        if line and wm:
            api.post('/inventory/stock/issue', {'warehouseId': wm, 'workOrderId': wo_id, 'workOrderPartId': line, 'quantity': qty})
        api.post(f'/work-orders/{wo_id}/start', {})
        if i in (0, 6):
            continue                                   # stays IN_PROGRESS (open jobs)
        detail = api.get(f'/work-orders/{wo_id}') or {}
        parts_cost = float(detail.get('partsCostFromStock') or 0)
        api.post(f'/work-orders/{wo_id}/complete', {'completionNotes': 'Work completed and road tested',
                                                     'actualCost': round(parts_cost + labour + (1500 if i == 3 else 0), 2),
                                                     'outsidePaymentMode': ['CREDIT', 'CASH', 'BANK'][i % 3]})

    # ------------------------------------------------------------------ 12. Supplier bills and payments
    print('[12] Supplier bills and payments')
    for code, bno, ago, cat, amt, gst, vi in [('PKC-S09', 'SWW-118', 40, 'REPAIR', 6500, 1170, 2), ('PKC-S02', 'SBT-8902', 28, 'TYRES', 3200, 576, 0),
                                              ('PKC-S10', 'MT-455', 22, 'OTHER', 4800, 864, None), ('PKC-S04', 'KLE-802', 10, 'REPAIR', 2900, 522, 3)]:
        bill = api.post('/payables/bills', {'supplier': {'id': sup.get(code)}, 'supplierBillNo': bno, 'billDate': d(ago),
                                            'category': cat, 'taxableAmount': amt, 'gstAmount': gst,
                                            'vehicle': {'id': vehicles[vi]['id']} if vi is not None else None,
                                            'remarks': 'Demo bill'})
        if bill and ago > 12:
            api.post(f"/payables/bills/{bill['id']}/approve")
    for code, amt, ago, method in [('PKC-S01', 6000, 18, 'BANK_TRANSFER'), ('PKC-S02', 40000, 14, 'CHEQUE'),
                                   ('PKC-S09', 7670, 9, 'UPI'), ('PKC-S07', 5000, 5, 'BANK_TRANSFER')]:
        api.post('/payables/payments', {'supplierId': sup.get(code), 'paymentDate': d(ago), 'amount': amt,
                                        'paymentMethod': method, 'referenceNumber': f'PAY{ago}{amt}'})

    print('\nDone.')
    if api.errors:
        print(f"{len(api.errors)} step(s) were refused by the application (business rules) — see '!' lines above.")
    else:
        print('All steps succeeded.')


if __name__ == '__main__':
    main()
