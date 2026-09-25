# Exports, attachments and photos (V64)

## Excel / PDF export
`GET /api/v1/exports/{module}?format=xlsx|pdf` (company-scoped). UI: `<app-export-buttons module="...">`.

| Screen | module |
|---|---|
| Vehicle / Driver / Customer / Material masters | vehicles, drivers, customers, materials |
| Bookings, Trips, Fuel, Expenses | bookings, trips, fuel, expenses |
| Invoices, Payments (receipts) | invoices, receipts |
| Work orders, Maintenance requests, Spare parts, Stock, Inventory transactions | work-orders, maintenance-requests, spare-parts, stock, inventory-transactions |
| Accounts (day book), Chart of accounts, Suppliers | journal, chart-of-accounts, suppliers |
| Driver payroll, Driver advances | payroll, driver-advances |

Finance lists (invoices, receipts, payroll, advances, journal, chart of accounts, expenses) need
COMPANY_ADMIN / ADMIN / ACCOUNTANT / BRANCH_MANAGER / VIEWER. PDFs are landscape A4 with repeated headers and page numbers.
Existing single-document PDFs (invoice, receipt, salary slip, customer ledger, financial reports) are unchanged.

## File storage
Uploads are stored in PostgreSQL (`stored_files`), stamped with the uploader's company; downloads check it.
Older files on disk are still served with the previous ownership checks. Allowed: PDF (10 MB), JPG/PNG/WEBP (5 MB).

## Attachments
`/api/v1/attachments` (list, upload multipart, delete). Up to 20 per record; uploader or admin can remove.

| Record | Typical documents | Screen |
|---|---|---|
| INVOICE | signed copy, e-way bill, POD | Invoice editor |
| BOOKING | customer PO, quotation | Booking editor |
| RECEIPT | cheque copy, UPI screenshot, bank slip | Receipt editor |
| EXPENSE / FUEL_ENTRY | bills, pump slips | Expense / Fuel editors |
| WORK_ORDER | garage bills, parts invoices, photos | Work order detail |
| MAINTENANCE_REQUEST | damage photos (drivers: own requests only) | Request detail |
| DRIVER_PAYROLL | signed salary voucher, bank proof | Payroll detail |
| DRIVER_ADVANCE, SUPPLIER, JOURNAL_VOUCHER | supported by the API | — |

Vehicle, driver, customer and trip documents keep their existing dedicated document modules (with expiry dates).

## Photos
`POST|DELETE /api/v1/photos/{vehicles|drivers|spare-parts}/{id}` (images only). UI: `<app-entity-photo>`
in Vehicle Master (selected vehicle), Driver Master (operations drawer) and the Spare parts catalog.
Company logo and user avatar already existed.
