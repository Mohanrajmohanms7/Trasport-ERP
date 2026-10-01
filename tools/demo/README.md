# PKC Transport demo data

`seed_pkc_demo.py` loads a complete, connected demo dataset (Perambalur / Thanirpandal) for **PKC Transport**
through the TransaFlow API, logged in as PKC's Company Admin. Because it uses the application itself, all numbering,
GST, stock valuation, accounting entries and business rules are applied exactly as in normal use.

```
python3 tools/demo/seed_pkc_demo.py --url https://<backend-host>/api/v1 --username <pkc admin username> --password <password>
```

- Needs Python 3.8+ only (no extra packages). Takes 1–3 minutes.
- Touches only the company of the login used; refuses to run with the platform admin login.
- Runs once: stops if customer `PKC-C01` already exists.
- Dates are relative to the day you run it (last ~70 days), so reports show recent data.

Creates: 2 branches, pay slabs, 6 materials, 5 quarries, 5 loading points, 10 customers + delivery sites,
6 vehicles + 6 drivers (assigned), 10 suppliers, 2 stores, 11 spare parts with opening stock and 10 purchases,
14 bookings in **Units** (PKC orders by the Unit, rates per Unit), ~47 trips (dispatched, completed, loaded/delivered Units), ~25 fuel entries, ~36 expenses, ~17 invoices,
10 receipts, 10 driver advances, 10 payrolls, 10 maintenance requests, ~10 work orders, 10 supplier bills, 10 supplier payments.
Some items are deliberately left pending (draft invoices, unpaid invoices, open jobs, unbilled trips, expiring documents)
so the pending / outstanding / compliance reports have content.
