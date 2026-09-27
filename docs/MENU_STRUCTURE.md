# Menu structure (tenant users)

| Group | Items | Where each thing is created |
|---|---|---|
| Masters | Branch Master, Customer Master, Vehicle Master, Driver Master, Material & Quarry, Supplier Master | Each master only in its own screen |
| Operations | Bookings, Trips & Dispatch, Fuel, Expenses | Daily transactions |
| Maintenance & Stores | Maintenance Requests, Work Orders, Spare Parts, Warehouses, Stock, Inventory Transactions | Fleet upkeep and parts |
| Finance | Invoices, Customer Receipts, Supplier Bills & Payments, Driver Payroll, Accounts | Money in / out |
| Reports | Reports, Financial Statements | Analysis and export |
| Admin | Users & Roles, Dropdown Lists, System Settings | Admins only |

- **Branch Master** (`/masters`): branch/office/yard information only — code, name, GSTIN (decides CGST+SGST vs IGST),
  manager, contact, address, status — with counts of users, vehicles, drivers and stores per branch. No vehicle/driver/
  customer editing here. A branch with users, vehicles, drivers, customers, stores or transactions cannot be deleted
  (deactivate it); the last branch cannot be deleted.
- **Dropdown Lists** (`/lookup-values`): vehicle types, fuel types, expense categories, units, etc. (admin only).
- **System Settings**: company profile, financial years, numbering and other settings; branches moved to Branch Master.
- New vehicles/drivers always belong to a branch (chosen, the user's branch, or head office).
- Search on masters is always limited to the user's company (fixed a leak where a code search could match other tenants).
