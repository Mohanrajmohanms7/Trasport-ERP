# TRANSAFLOW TRANSPORT ERP — BUSINESS & FUNCTIONAL CONTEXT FOR CLAUDE UI/UX DESIGN

> **DOCUMENT TYPE**: Business & Functional Context Specification  
> **TARGET AUDIENCE**: Claude (UI/UX Designer Agent)  
> **PRIMARY REPOSITORY**: `Mohanrajmohanms7/Trasport-ERP`  
> **PRIMARY WORKSPACE**: `d:\Mohan Programs`  
> **LAST AUDITED**: 2026-09-19  

---

## SECTION 1 — PRODUCT OVERVIEW

- **Product Name**: TransaFlow Transport ERP & SaaS Logistics Platform
- **Product Purpose**: An end-to-end, multi-tenant enterprise software platform engineered to digitize heavy transport, fleet operations, aggregate/sand quarry dispatching, driver payroll, customer billing, and financial accounting.
- **Business Domain**: Heavy Transport, Sand & Aggregate Mining Logistics, Fleet Asset Management, Commercial Driver Operations, Double-Entry Financial Accounting.
- **Organization Types**: Transport Contractors, Fleet Owners, Mining & Quarry Logistics Managers, Material Suppliers, Haulage Logistics Operators.
- **Main Business Problems Solved**:
  1. Manual dispatch bottlenecks and vehicle-driver assignment errors.
  2. Loss of revenue due to unbilled trip dispatches or inaccurate weight/density conversions (CFT ↔ Unit ↔ Ton).
  3. Lack of real-time visibility into vehicle utilization and maintenance status.
  4. Leakage in fuel log tracking and driver advance deductions.
  5. Delayed customer billing and uncollected accounts receivable.
  6. Financial reporting discrepancies between operational dispatches and double-entry accounting ledgers.
- **Product Maturity**: Production-ready, fully verified multi-tenant SaaS ERP (Steps 1–13 locked, Flyway V50 complete, 95/95 backend tests passing, 100% frontend production build success).

---

## SECTION 2 — USERS & PERSONAS

| Role | Business Persona | Main Responsibilities | Main Screens / Areas | Important Actions |
| :--- | :--- | :--- | :--- | :--- |
| `TENANT_ADMIN` / `ADMIN` | Company Administrator / Executive Manager | Overall business management, system setup, user access control, financial approval, company settings. | All Consoles, Dashboard, User Management, Setup Wizard. | Approve bookings, review profit margins, manage users, configure master settings. |
| `OWNER` | Fleet Owner / Investor | Fleet yield analysis, ROI monitoring, monthly profitability oversight, asset management. | Executive Dashboard, Fleet Console, Profit & Loss. | Review profit trends, monitor fleet utilization percentage, analyze cost centers. |
| `OPERATIONS` / `DISPATCHER` | Logistics Operations Manager / Quarry Dispatcher | Customer booking entry, vehicle-driver dispatch pairing, loading queue management, POD tracking. | Booking Console, Trip Planning Console, Quarry Console. | Create bookings, dispatch trips, update trip status, upload Proof of Delivery (POD). |
| `FLEET_MANAGER` / `VEHICLE` | Workshop Maintenance Controller / Fleet Officer | Fleet vehicle availability, driver assignment, vehicle service/repair logs, permit renewal tracking. | Vehicle Roster Console, Service Logs, Driver Console. | Assign drivers to vehicles, log maintenance work, track insurance/permit expiries. |
| `ACCOUNTANT` | Chief Accountant / Billing Clerk | Sales invoice billing, customer payment receipts, expense logging, payroll processing, JV posting. | Invoice Console, Payment Console, Expense Console, Accounts Console, Financial Reports. | Generate tax invoices, allocate receipts, post Journal Vouchers, export financial reports. |
| `DRIVER` | Commercial Vehicle Operator / Hauler | Trip execution, fuel station logging, duty attendance, salary slip review. | Driver Console, Fuel Entry Console, Salary Slips. | Log fuel entries, check assigned trips, download monthly salary slips (PDF). |
| `SUPER_ADMIN` | SaaS Platform Administrator | Platform tenant provisioning, subscription renewal, multi-tenant system health monitoring. | Platform Admin, Renewal Console. | Provision tenant companies, manage SaaS subscriptions. |

---

## SECTION 3 — COMPLETE BUSINESS MODULE MAP

| Module | Business Purpose | Main Users | Related Modules |
| :--- | :--- | :--- | :--- |
| **Master Management** | Central repository for foundational business entities (Materials, Vehicles, Drivers, Customers, Delivery Sites, Chart of Accounts, UOM Master). | Admin, Fleet Manager, Accountant | All Modules |
| **Booking Management** | Captures customer haulage orders, delivery site destinations, material rates, and priority dispatch queues. | Operations, Admin, Customer | Trips, Masters, Invoices |
| **Trip & Dispatch Management** | Manages vehicle-driver dispatch assignments, live trip lifecycle tracking, loading queues, and Proof of Delivery (POD). | Operations, Fleet Manager, Driver | Bookings, Vehicles, Drivers, POD |
| **Quarry & Material Operations** | Manages sand/aggregate quarry dispatches, density conversions, and volumetric weight tracking. | Operations, Admin | Materials, Trips, Invoices |
| **Fuel & Expense Management** | Tracks vehicle fuel station fills, driver cash advances, workshop service costs, and direct operating expenses. | Fleet Manager, Accountant, Driver | Vehicles, Drivers, Accounting |
| **Driver Payroll System** | Computes monthly driver basic salary, trip allowances, deductions, advance adjustments, and salary slips. | Accountant, Driver, Admin | Drivers, Trips, Accounting |
| **Sales Invoicing & Billing** | Generates official tax invoices from completed trip dispatches with GST, subtotal, and net payable figures. | Accountant, Admin | Bookings, Trips, Customers, Receipts |
| **Customer Receipts & Ledger** | Captures customer payments (Cash/Bank), allocates receipts to unpaid invoices, and maintains running customer ledgers. | Accountant, Admin | Invoices, Customers, Accounting |
| **Double-Entry Accounting (JV)** | Maintains Chart of Accounts, processes formal Journal Vouchers, and enforces balanced debit/credit posting. | Accountant, Admin | Receipts, Invoices, Expenses, Payroll |
| **Financial Reporting** | Generates formal Trial Balance, General Ledger (GL), Profit & Loss (P&L), and Balance Sheet statements. | Accountant, Owner, Admin | Accounting, All Financial Modules |
| **Native Document Exports** | Generates formatted POI Excel (`.xlsx`), OpenPDF vector PDFs, and browser print layouts across list/detail screens. | All Users | All Modules |

---

## SECTION 4 — COMPLETE SCREEN INVENTORY

| Screen Name | Module | Screen Purpose | Primary Users | Screen Type | Related Screens |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `dashboard` | Executive Telemetry | Role-tailored operational KPIs, utilization gauges, trend charts, system alarms, and quick task routing. | All 6 Roles | Dashboard / Overview | All Consoles |
| `master-management` | Master Data | Management of materials, rate cards, delivery sites, UOM master, and COA. | Admin, Accountant | Master List / Form | Bookings, Vehicles |
| `customer-details-console` | Customer Master | Customer directory, credit limit enforcement, contact info, and customer ledger statements. | Accountant, Admin | Console (List/Detail) | Invoices, Receipts |
| `vehicle-details-console` | Fleet Roster | Vehicle inventory, chassis/engine numbers, permit/insurance expiries, and driver pairings. | Fleet Manager, Admin | Console (List/Detail) | Drivers, Trips, Expenses |
| `driver-details-console` | Driver Management | Driver roster, license expiries, assigned vehicles, attendance, and salary slips. | Fleet Manager, Accountant | Console (List/Detail) | Vehicles, Driver Payroll |
| `booking-details-console` | Bookings | Customer haulage order entries, priority scheduling, and dispatch queue status. | Operations, Admin | Console (List/Detail) | Trips, Customers, Sites |
| `trip-details-console` | Trip Dispatch | Live dispatch execution, vehicle-driver assignment, POD document uploads, and trip completion. | Operations, Driver | Console (List/Detail) | Bookings, POD, Fuel |
| `material-quarry-console` | Quarry Operations | Quarry loading logs, material density conversions (CFT ↔ Unit ↔ Ton). | Operations, Admin | Console / Form | Materials, Trips |
| `fuel-details-console` | Fuel Management | Fuel station fills, quantity (litres), rate per litre, total cost, and odometer readings. | Driver, Fleet Manager | Console (List/Detail) | Vehicles, Drivers, Expenses |
| `expense-details-console` | Operating Expenses | Direct trip expenses, vehicle maintenance costs, driver advances, and category expense logging. | Accountant, Fleet Manager | Console (List/Detail) | Vehicles, Drivers, Accounting |
| `payment-details-console` | Receipts & Allocation | Customer receipt entry (Cash/Bank), advance tracking, and receipt-to-invoice allocation. | Accountant, Admin | Console (List/Detail) | Invoices, Customers, Ledger |
| `invoice-details-console` | Sales Invoicing | Sales invoice generation from trips, tax calculations, net amount, and PDF tax invoice printing. | Accountant, Admin | Console (List/Detail) | Customers, Bookings, Receipts |
| `accounts-details-console` | General Ledger / JV | Chart of Accounts setup, double-entry Journal Voucher creation, posting, and JV printing. | Accountant, Admin | Console (List/Detail) | Financial Reports |
| `report-details-console` | Financial Reports | Presentation and export of Trial Balance, GL, P&L, and Balance Sheet (PDF + CSV + Print). | Accountant, Owner, Admin | Report Console | Accounts Console |
| `company-administration` | System Admin | Tenant company profiles, branch setups, user management, and system preferences. | Tenant Admin | Admin Form / List | Setup Wizard |

---

## SECTION 5 — COMPLETE FIELD CATALOG

Below is the complete inventory of business fields across TransaFlow ERP modules:

### 1. Material Master & UOM
- `code` (Text, Required, Read-Only after creation): Unique material identifier (e.g., `MAT-001`).
- `name` (Text, Required): Name of material (e.g., `Blue Metal 20mm`, `M-Sand`).
- `category` (Dropdown, Required): Material classification (e.g., `AGGREGATE`, `SAND`, `CEMENT`, `BRICKS`).
- `defaultUom` (Dropdown, Required): Standard Unit of Measurement (e.g., `CFT`, `UNIT`, `TON`, `BAG`, `KG`, `LITRE`).
- `defaultRate` (Currency Number, Required): Standard rate per default UOM.
- `density` (Numeric, Optional): Specific gravity/density (Ton/m³ or Ton/CFT) for weight-volume conversion.
- `status` (Badge, System): Operational status (`ACTIVE`, `INACTIVE`).

### 2. Vehicle Master
- `code` / `registrationNumber` (Text, Required): Vehicle registration number (e.g., `TN-38-AB-1234`).
- `name` / `model` (Text, Required): Vehicle make and model (e.g., `TATA Prima 2830.K`).
- `chassisNumber` & `engineNumber` (Text, Required): Technical chassis and engine numbers.
- `capacity` (Dropdown, Required): Volumetric / weight capacity (e.g., `2-UNIT`, `3-UNIT`, `10-WHEELER`, `16-TON`).
- `ownerType` (Dropdown, Required): Ownership category (`OWNED`, `LEASED`, `ATTACHED`).
- `insuranceExpiryDate` & `permitExpiryDate` (Date, Required): Compliance expiry dates.
- `status` (Badge, System): Vehicle operational state (`AVAILABLE`, `ON_TRIP`, `IN_SERVICE`, `INACTIVE`).

### 3. Driver Master & Payroll
- `code` & `name` (Text, Required): Driver identifier and full legal name.
- `licenseNumber` & `licenseExpiryDate` (Text & Date, Required): Driving license number and expiry date.
- `phoneNumber` (Phone Text, Required): Driver contact number.
- `basicSalary` (Currency Number, Required): Fixed monthly basic salary.
- `allowanceAmount` (Currency Number, Optional): Allowance per trip or monthly allowance.
- `deductionAmount` (Currency Number, System/Editable): Payroll deductions.
- `advanceAdjustment` (Currency Number, System/Editable): Auto-adjusted cash advance balance.
- `netSalaryPayable` (Currency Number, Calculated): `basicSalary + allowanceAmount - deductionAmount - advanceAdjustment`.
- `status` (Badge, System): Driver status (`ACTIVE`, `ON_LEAVE`, `TERMINATED`).

### 4. Customer Master & Ledger
- `code` & `name` (Text, Required): Customer business code and legal trade name.
- `gstNumber` & `panNumber` (Tax Text, Optional): GSTIN and PAN details.
- `creditLimit` (Currency Number, Required): Maximum allowed outstanding credit threshold.
- `totalInvoiced` (Currency Number, Calculated): Sum of all billable invoices.
- `totalCollected` (Currency Number, Calculated): Sum of all customer receipts.
- `outstandingBalance` (Currency Number, Calculated): `totalInvoiced - totalCollected`.
- `status` (Badge, System): Customer account status (`ACTIVE`, `CREDIT_HOLD`, `INACTIVE`).

### 5. Booking Order
- `bookingNumber` (Text, Auto-Generated): Unique booking reference (e.g., `BKG-2026-0089`).
- `bookingDate` (Date, Required): Order placement date.
- `customer` (Lookup Reference, Required): Selected customer.
- `deliverySite` (Lookup Reference, Required): Destination site address.
- `priority` (Dropdown, Required): Priority level (`HIGH`, `MEDIUM`, `LOW`).
- `status` (Badge, System): Booking lifecycle state (`DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).

### 6. Trip Dispatch & POD
- `tripNumber` (Text, Auto-Generated): Unique trip reference (e.g., `TRP-2026-0412`).
- `tripDate` (Date, Required): Dispatch date.
- `booking` (Lookup Reference, Required): Parent booking reference.
- `vehicle` (Lookup Reference, Required): Assigned vehicle.
- `driver` (Lookup Reference, Required): Assigned driver.
- `status` (Badge, System): Dispatch state (`PLANNED`, `ALLOCATED`, `LOADING`, `DISPATCHED`, `IN_TRANSIT`, `UNLOADING`, `COMPLETED`, `DELAYED`, `CANCELLED`).
- `podDocument` (File Upload, Optional): Uploaded weighbridge slip or delivery challan image/PDF.

### 7. Sales Invoice
- `invoiceNumber` (Text, Auto-Generated): Unique tax invoice number (e.g., `INV-2026-0104`).
- `invoiceDate` & `dueDate` (Date, Required): Invoice issuance and payment due dates.
- `customer` (Lookup Reference, Required): Billed customer.
- `subtotal` (Currency Number, Calculated): Sum of trip material charges.
- `taxAmount` (Currency Number, Calculated): GST tax total (CGST + SGST or IGST).
- `discount` (Currency Number, Optional): Invoice discount.
- `netAmount` (Currency Number, Calculated): `subtotal + taxAmount - discount`.
- `paidAmount` (Currency Number, Calculated): Total receipts allocated to invoice.
- `paymentStatus` (Badge, System): Payment state (`UNPAID`, `PARTIAL`, `PAID`).
- `status` (Badge, System): Invoice state (`DRAFT`, `APPROVED`, `CANCELLED`).

### 8. Customer Receipt & Payment Allocation
- `receiptNumber` (Text, Auto-Generated): Unique receipt voucher number (e.g., `RCT-2026-0055`).
- `receiptDate` (Date, Required): Receipt date.
- `customer` (Lookup Reference, Required): Paying customer.
- `amountReceived` (Currency Number, Required): Total payment received.
- `paymentMethod` (Dropdown, Required): Mode of payment (`CASH`, `BANK_TRANSFER`, `CHEQUE`, `UPI`).
- `referenceNumber` (Text, Optional): Bank transaction ref or cheque number.
- `unallocatedAmount` (Currency Number, Calculated): Amount remaining unassigned to invoices.

### 9. Journal Voucher & Financial Report Data
- `voucherNumber` (Text, Auto-Generated): Double-entry JV number (e.g., `JV-2026-0034`).
- `voucherDate` (Date, Required): Posting date.
- `accountCode` & `accountName` (Lookup Reference, Required): Chart of Account entry.
- `debitAmount` & `creditAmount` (Currency Number, Required): Double-entry amounts.
- `runningBalance` (Currency Number, Calculated): Cumulative GL / Ledger balance.

---

## SECTION 6 — FIELD RELATIONSHIPS & DEPENDENCIES

```text
Material Master (defaultUom, density)
   │
   ▼
Customer Booking (customer, deliverySite, material, agreedRate)
   │
   ▼
Trip Dispatch (booking, vehicle, driver) ◄── Vehicle & Driver Pairing
   │
   ├──► Fuel Entry (vehicle, driver, quantity, rate)
   ├──► Expense Log (vehicle, driver, expenseCategory)
   └──► Proof of Delivery (POD Image / Weighbridge Slip)
   │
   ▼
Sales Invoice (customer, trips, subtotal, tax, netAmount)
   │
   ▼
Customer Receipt (customer, amountReceived, paymentMethod)
   │
   ▼
Payment Allocation (receipt, invoice, allocatedAmount)
   │
   ▼
Customer Ledger (debitAmount, creditAmount, runningBalance)
   │
   ▼
Double-Entry Journal Voucher (accountCode, debit, credit)
   │
   ▼
Financial Reports (Trial Balance ➔ GL ➔ P&L ➔ Balance Sheet)
```

---

## SECTION 7 — COMPLETE ACTION CATALOG

| Screen Name | Action | Purpose | Available When | Not Available When | Business Effect |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `booking-details-console` | `Create Booking` | Enters a new haulage order. | Always | Screen read-only mode | Creates `DRAFT` booking order. |
| `booking-details-console` | `Approve Booking` | Authorizes booking for trip dispatch. | Booking is `SUBMITTED` | Booking is `DRAFT`, `APPROVED`, or `CANCELLED` | Changes state to `APPROVED`, enables trip creation. |
| `booking-details-console` | `Reject / Cancel` | Rejects or cancels booking order. | Booking is not completed | Booking is `COMPLETED` or `CANCELLED` | Marks booking `CANCELLED`, frees delivery capacity. |
| `trip-details-console` | `Dispatch Trip` | Dispatches vehicle and driver on route. | Trip is `PLANNED` / `ALLOCATED` | Vehicle or Driver unavailable | Sets trip to `DISPATCHED`, vehicle to `ON_TRIP`. |
| `trip-details-console` | `Complete Trip` | Confirms delivery completion. | Trip is `DISPATCHED` / `UNLOADING` | Trip is `COMPLETED` or `CANCELLED` | Sets trip to `COMPLETED`, vehicle to `AVAILABLE`. |
| `trip-details-console` | `Upload POD` | Attaches weighbridge slip or POD photo. | Trip is `DISPATCHED` or `COMPLETED` | Trip is `CANCELLED` | Attaches POD file metadata to trip. |
| `invoice-details-console` | `Generate Invoice` | Bills completed trips for customer. | Trips are `COMPLETED` & unbilled | Trips are already billed | Creates `APPROVED` Sales Invoice. |
| `invoice-details-console` | `Cancel Invoice` | Cancels a sales invoice. | Invoice is `UNPAID` | Invoice has allocated receipts | Marks invoice `CANCELLED`. (Does NOT touch receipts). |
| `payment-details-console` | `Receive Payment` | Records customer payment. | Always | Screen read-only mode | Creates Customer Receipt, updates customer balance. |
| `payment-details-console` | `Allocate Payment` | Maps receipt funds to invoice. | Unallocated receipt amount > 0 | Invoice is fully `PAID` | Decreases invoice outstanding, updates ledger. |
| `report-details-console` | `Export PDF/CSV` | Generates official report statement. | Always | Never | Downloads formatted `.pdf` or `.csv` statement. |
| `report-details-console` | `Print Report` | Opens browser print window. | Always | Never | Renders formatted print DTO layout. |

---

## SECTION 8 — COMPLETE STATUS & LIFECYCLE CATALOG

### 1. Booking Order Lifecycle
```text
[DRAFT] ──► (Submit) ──► [SUBMITTED] ──► (Approve) ──► [APPROVED] ──► (Dispatch Trips) ──► [COMPLETED]
                                 │                            │
                                 └──► (Reject) ──► [REJECTED] └──► (Cancel) ──► [CANCELLED]
```

### 2. Trip Dispatch Lifecycle
```text
[PLANNED] ──► [ALLOCATED] ──► [LOADING] ──► [DISPATCHED] ──► [IN_TRANSIT] ──► [UNLOADING] ──► [COMPLETED]
     │              │             │              │               │               │
     └──────────────┴─────────────┴──────────────┴───────────────┴───────────────┴──► [CANCELLED] / [DELAYED]
```

### 3. Sales Invoice Lifecycle
```text
[DRAFT] ──► (Approve) ──► [APPROVED] ──► (Payment Allocation) ──► [PARTIAL] ──► [PAID]
                                 │
                                 └──► (Cancel - if unpaid) ──► [CANCELLED]
```

---

## SECTION 9 — COMPLETE BUSINESS RULE CATALOG

| Rule ID | Rule Trigger | Business Condition | Allowed Action | Blocked Action | Business Reason |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `BR-001` | Booking Approval | Customer outstanding balance exceeds `creditLimit`. | Require Admin Credit Hold Override | Automatic Booking Approval | Prevents bad debt accumulation for over-limit accounts. |
| `BR-002` | Trip Assignment | Vehicle `status != AVAILABLE` or Driver `status != ACTIVE`. | Select available vehicle/driver | Assign busy/maintenance vehicle | Prevents double-booking vehicles or drivers. |
| `BR-003` | Invoice Cancellation | Sales Invoice has associated payment allocations (`paidAmount > 0`). | Cancel invoice AFTER unallocating receipts | Direct Invoice Cancellation | Preserves financial audit integrity; receipts must be unallocated first. |
| `BR-004` | Closed-Period Protection | Posting date falls within a closed Financial Year. | View historical records | Create, Edit, or Reverse transactions | Guarantees compliance with audited financial periods. |
| `BR-005` | JV Double-Entry Balance | Total JV Debit Amount != Total JV Credit Amount. | Save as Draft | Final Posting to General Ledger | Enforces double-entry accounting balancing. |
| `BR-006` | Material Conversion | Volume to Weight dispatch transformation. | Calculate using `density` multiplier | Hardcode arbitrary weight | Standardizes CFT ↔ Unit ↔ Ton transformations. |

---

## SECTION 10 — ROLE & PERMISSION CONTEXT

- **Tenant Isolation**: All screens strictly isolate data to the user's authenticated `companyId`. Cross-tenant data viewing or editing is strictly prohibited.
- **Role Permission Scoping**:
  - `ADMIN`: Full access (View, Add, Edit, Delete, Approve, Cancel, Print, Export).
  - `ACCOUNTANT`: Financial access (Invoices, Receipts, Expenses, Payroll, JVs, Reports). Cannot alter vehicle maintenance or dispatch rules.
  - `OPERATIONS`: Operational access (Bookings, Trips, POD Uploads, Quarry logs). Cannot approve payroll or alter Chart of Accounts.
  - `FLEET_MANAGER`: Vehicle and driver roster access (Service logs, Driver pairings). Cannot generate sales invoices.
  - `DRIVER`: Read-only access to assigned trips and salary slips; write access only for fuel entries.

---

## SECTION 11 — CORE ERP BUSINESS WORKFLOW

1. **Setup & Master Data**: Admin configures Chart of Accounts, Materials, Delivery Sites, Vehicles, and Drivers.
2. **Order Placement**: Operations logs a Customer Booking with agreed material rates and delivery site addresses.
3. **Dispatch Execution**: Operations pairs an available Vehicle and Driver to create a Trip Dispatch.
4. **Quarry Loading & POD**: Vehicle arrives at quarry, loads material (converted via density rules), completes route, and uploads Proof of Delivery (POD).
5. **Operating Costs**: Fuel fills and driver advances are logged against the vehicle/trip.
6. **Sales Invoicing**: Accountant generates a Sales Invoice for completed trips. GST and net payable amounts are auto-computed.
7. **Customer Receipt & Allocation**: Customer pays via Cash/Bank. Accountant logs a Customer Receipt and allocates funds to unpaid invoices.
8. **General Ledger & Financial Reporting**: Ledger entries post to COA accounts via Journal Vouchers. Accountant/Owner generates Trial Balance, P&L, and Balance Sheet statements.

---

## SECTION 12 — CROSS-MODULE WORKFLOWS

### Customer-to-Cash Workflow
```text
Customer Master ──► Order Booking ──► Trip Dispatch ──► Sales Invoice ──► Receipt Allocation ──► Customer Ledger
```

### Fleet & Cost Tracking Workflow
```text
Vehicle Master ──► Driver Pairing ──► Trip Dispatch ──► Fuel & Expense Logging ──► Workshop Maintenance Log
```

### Payroll & Settlement Workflow
```text
Driver Roster ──► Trip Logs ──► Advance Adjustments ──► Payroll Computation ──► Driver Salary Slip PDF
```

---

## SECTION 13 — FINANCIAL BUSINESS CONTEXT

- **Immutable Financial Audit Trail**: Financial transactions (Invoices, Receipts, Expenses, JVs) are permanent audit records. Corrections require explicit cancellation or reversal entries.
- **Invoice Cancellation vs Payment Reversal**:
  - **Crucial Rule**: Cancelling an invoice does **NOT** automatically reverse or refund a customer receipt.
  - Receipts remain recorded in the customer's account as unallocated credit balance until explicitly refunded or allocated to another invoice.
- **Running Customer Balance**: `Running Balance = Previous Balance + Debit (Invoices) - Credit (Receipts)`.

---

## SECTION 14 — IMPORTANT CALCULATIONS

| Calculation Name | Business Meaning | Formula / Logic | Displayed On |
| :--- | :--- | :--- | :--- |
| `netAmount` (Invoice) | Final billable amount after tax and discount. | `subtotal + taxAmount - discount` | Invoice List / PDF / Detail |
| `outstandingBalance` | Customer total uncollected credit. | `totalInvoiced - totalCollected` | Customer List / Dashboard |
| `netSalaryPayable` | Final net salary payable to driver. | `basicSalary + allowanceAmount - deductionAmount - advanceAdjustment` | Driver Payroll / Salary Slip |
| `vehicleUtilization` | Percentage of active fleet currently dispatched. | `(runningVehicles * 100) / totalVehicles` | Executive Dashboard |
| `monthlyProfit` | Net monthly company operating profit. | `monthlyRevenue - (monthlyFuelCost + monthlyExpenses + monthlyPayroll)` | Owner Dashboard / P&L |
| `materialConversion` | Volumetric to weight dispatch conversion. | `Weight (Tons) = Volume (CFT) * density` | Quarry Console / Dispatch |

---

## SECTION 15 — SEARCH, FILTER & DATA DISCOVERY

Every list and console screen supports comprehensive data discovery:
- **Global Text Search**: Instant filtering by Code, Reference Number, Customer Name, Registration Number, or Driver Name.
- **Date Range Filters**: Custom start date and end date filtering (`startDate`, `endDate`, `asOfDate`).
- **Status Filters**: Multi-select or dropdown status filters (e.g., `ACTIVE`, `PENDING`, `APPROVED`, `COMPLETED`, `PAID`).
- **Entity Dropdown Filters**: Scoped dropdown filters for Customer, Vehicle, Driver, Site, and Branch.
- **Pagination**: Server-side and client-side pagination controls (Standard 20 rows per page; exports fetch up to 5,000 matching records).

---

## SECTION 16 — TABLE / LIST BUSINESS CONTEXT

All primary console screens render structured data tables displaying:
1. **Header Toolbar**: Search box, filter dropdowns, date pickers, "Export XLSX", "Export CSV", "Print", and "Create New" primary action buttons.
2. **Data Columns**: Formatted text, currency values (`₹#,##0.00`), dates (`dd-MMM-yyyy`), and color-coded status badges.
3. **Row Action Items**: Action icon buttons (`View Detail`, `Edit`, `Approve`, `Cancel`, `Download PDF`, `Print`).
4. **Summary Footer**: Total counts, aggregated totals (Total Subtotal, Total Tax, Total Net Amount), and pagination bar.

---

## SECTION 17 — DETAIL SCREEN BUSINESS CONTEXT

When inspecting a specific record in detail view:
- **Record Identity**: Displays Code, Reference Number, Creation Timestamp, and Audit User.
- **Status Banner**: Color-coded header banner displaying current lifecycle status (`APPROVED`, `IN_PROGRESS`, `PAID`, etc.).
- **Summary Cards**: Key figures highlighted at the top (e.g., Net Amount, Allocated Amount, Balance).
- **Tabbed / Sectioned Views**:
  - *Main Information*: Core fields and reference details.
  - *Line Items / Sub-records*: Associated trip list, invoice line items, or receipt allocations.
  - *Document Attachments*: Uploaded POD images, weighbridge slips, or contract files.
  - *Audit Log*: Timestamped history of status changes and user edits.

---

## SECTION 18 — FORMS & TRANSACTIONS

Transaction creation/edit forms enforce strict validation before submission:
- **Required Fields**: Demarcated with indicator flags.
- **Dynamic Lookups**: Dropdown lookups populate dynamically based on tenant company context.
- **Validation Rules**: Real-time validation preventing submission of invalid dates, negative amounts, or un-balanced double entries.
- **Confirmation Prompts**: Modal confirmation required prior to irreversible operations (e.g., Approval, Cancellation, Reversal).

---

## SECTION 19 — AUDIT, HISTORY & LIFECYCLE INFORMATION

Every major entity tracks business-visible audit history:
- `createdBy` & `createdDate`: User and timestamp when record was created.
- `updatedBy` & `updatedDate`: User and timestamp of latest modification.
- `statusHistory`: Log of status transitions (e.g., `DRAFT ➔ APPROVED by John Admin at 10:30 AM`).

---

## SECTION 20 — ERROR, WARNING & CONFIRMATION CONTEXT

| Action | Condition | User-Facing Warning / Error | Business Consequence |
| :--- | :--- | :--- | :--- |
| Booking Submission | Customer credit limit exceeded | `WARNING: Customer credit limit exceeded (Limit: ₹500,000, Current: ₹620,000). Admin approval required.` | Blocked unless Admin overrides. |
| Invoice Cancellation | Receipts are allocated | `ERROR: Cannot cancel invoice with active payment allocations. Unallocate receipts first.` | Action blocked. |
| JV Posting | Debits do not equal credits | `ERROR: Journal Voucher is unbalanced (Debit: ₹15,000, Credit: ₹14,500). Difference: ₹500.` | Posting blocked. |
| Closed Period Edit | Date in locked financial year | `ERROR: Financial Year 2024-25 is closed. Posting is prohibited.` | Action blocked. |

---

## SECTION 21 — DOCUMENTS, PRINT & EXPORTS

- **Native Excel Exports (`.xlsx`)**: Genuine Apache POI OOXML spreadsheets formatted with dark navy headers, formatted currency/numeric cells, ISO date formats, and bounded column auto-sizing.
- **CSV Exports (`.csv`)**: RFC-4180 compliant UTF-8 CSV exports with proper comma and quote escaping.
- **Vector PDF Downloads (`.pdf`)**: OpenPDF vector PDF documents with company letterhead logos, tax registration details, itemized tables, and summary footers.
- **Browser Print Engine**: Pop-up print windows with dedicated CSS print stylesheets formatted for A4 document printing.

---

## SECTION 22 — DASHBOARD BUSINESS CONTEXT

The Executive Dashboard provides 6 role-tailored telemetry perspectives:
1. **ADMIN VIEW**: Real-time dispatches, running trips, today's revenue, uncollected receivables, alerts feed.
2. **OWNER VIEW**: Monthly net profit, gross revenue, total expense breakdown, fleet utilization percentage ring gauge.
3. **OPERATIONS VIEW**: Scheduled loading queues, delayed trip counts, pending dispatches, quarry arrival queue status.
4. **VEHICLE MANAGER VIEW**: Available fleet roster, vehicles in service (workshop repairs), permit expiry alerts (<30 days).
5. **ACCOUNTANT VIEW**: Today's collections, total outstanding balance, pending invoice approvals, monthly income summary.
6. **DRIVER VIEW**: Active assigned trip dispatches, completed trip history, upcoming loading schedule, duty attendance rating.

---

## SECTION 23 — BUSINESS USER JOURNEYS

### Journey 1: Logistics Dispatcher Workflow
```text
Login ──► Open Dashboard ──► Check Pending Bookings ──► Create Trip Dispatch ──► Pair Available Vehicle & Driver ──► Monitor Loading Queue ──► Upload POD on Completion
```

### Journey 2: Accountant Billing & Receipt Workflow
```text
Login ──► Review Unbilled Trips ──► Generate Sales Invoice ──► Receive Customer Bank Payment ──► Allocate Receipt to Invoice ──► Verify Customer Ledger Statement
```

### Journey 3: Fleet Maintenance Workflow
```text
Login ──► View Vehicle Roster ──► Inspect Permit Expiry Alerts ──► Flag Vehicle for Workshop Service ──► Log Repair Expenses ──► Restore Vehicle Status to Available
```

---

## SECTION 24 — BUSINESS GLOSSARY

| Term | Business Meaning | Where Used |
| :--- | :--- | :--- |
| `UOM` | Unit of Measurement (CFT, Unit, Ton, Bag, Litre). | Material Master, Rate Cards, Invoices |
| `CFT` | Cubic Feet (Volumetric material measure for sand/metal). | Quarry Operations, Dispatch |
| `POD` | Proof of Delivery (Weighbridge slip, delivery challan, photo). | Trip Management, Billing |
| `GSTIN` | Goods and Services Tax Identification Number. | Customer Master, Invoices, Receipts |
| `JV` | Journal Voucher (Double-entry accounting transaction). | Accounts Console, General Ledger |
| `COA` | Chart of Accounts (Account categories: Asset, Liability, Equity, Revenue, Expense). | Accounting, JVs, Reports |
| `Unallocated Receipt` | Customer payment funds not yet mapped to a specific sales invoice. | Payment Console, Receipts |
| `Closed Period` | A locked Financial Year where postings/edits are legally blocked. | Accounting, Financial Reports |

---

## SECTION 25 — RULES A UI/UX DESIGNER MUST NEVER VIOLATE

> [!CAUTION]
> **MANDATORY RULES FOR CLAUDE UI/UX DESIGN**:
> 1. **Do NOT invent fake fields**: Only use business fields specified in Section 5.
> 2. **Do NOT invent non-existent statuses**: Stick strictly to lifecycles in Section 8.
> 3. **Do NOT imply automatic receipt cancellation when an invoice is cancelled**: Receipts remain as unallocated credit balance.
> 4. **Do NOT remove mandatory validation indicators**: Required fields must be clear.
> 5. **Do NOT hide critical financial figures**: Subtotals, tax, discounts, net totals, and balances must remain prominent.
> 6. **Do NOT remove tenant isolation or branch filters**: Multi-tenant context controls must be accommodated.
> 7. **Do NOT alter role perspective switching**: The 6 dashboard role perspectives must remain easily selectable.

---

## SECTION 26 — WHAT CLAUDE MAY CHANGE

Claude is encouraged to completely redesign and modernize:
- Visual layout structure, visual hierarchy, and component positioning.
- Typography scale, font sizes, text weights, and readability contrast.
- Color system usage (Tailwind CSS Slate palette, status color badges, accents).
- KPI card presentation, sparkline integration, and metric visualizers.
- Table design, column layouts, hover states, action button styles, and status badge visuals.
- Form layout, field grouping, input styling, validation feedback, and modal dialogs.
- Responsive stacking and mobile/tablet drawer adaptations.

---

## SECTION 27 — WHAT CLAUDE MUST NOT CHANGE

Claude **MUST NOT** change:
- Authoritative business rules, validation criteria, or lifecycle transition logic.
- Business field definitions, field data types, or underlying calculations.
- Status values or permission boundaries.
- Underlying REST API contracts or Angular service signal bindings.
- Multi-tenant company isolation boundaries.

---

## SECTION 28 — DESIGN PHILOSOPHY

TransaFlow ERP should be designed as a **high-density, professional enterprise SaaS platform**:
1. **Clear Visual Hierarchy**: Essential operational KPIs and status alerts must stand out immediately.
2. **Data Density without Clutter**: Maximize information visibility while maintaining clean spacing and alignment.
3. **Operational Speed**: Key actions (Dispatch, Invoice, Receipt, Export) must be accessible within minimal clicks.
4. **Financial Clarity**: Bold, legible currency formatting and unambiguous debit/credit distinctions.
5. **No Marketing Gimmicks**: Avoid excessive marketing gradients, decorative illustrations, or bloated white space that hinders daily enterprise usage.

---

## SECTION 29 — REUSABLE SCREEN DESIGN INPUT TEMPLATE

Claude can use this standardized template for designing any screen in TransaFlow ERP:

```text
================================================================================
CLAUDE SCREEN DESIGN SPECIFICATION INPUT TEMPLATE
================================================================================

SCREEN NAME: [Screen Name]
MODULE: [Module Name]
BUSINESS PURPOSE: [Brief business description]
PRIMARY USERS: [User roles using this screen]

BUSINESS WORKFLOW:
[Stage 1] ──► [Stage 2] ──► [Stage 3]

FIELDS TO DISPLAY:
- Field 1 (Type, Required/Optional, Display Rule)
- Field 2 (Type, Required/Optional, Display Rule)

PRIMARY ACTIONS:
- Action 1 (Purpose, Available When)
- Action 2 (Purpose, Available When)

STATUS VALUES & BADGES:
- Status 1 (Meaning, Badge Color)
- Status 2 (Meaning, Badge Color)

KEY CALCULATIONS:
- [Calculation Name] = [Formula]

SEARCH & FILTERS:
- Search Box Fields: [Fields searchable]
- Filter Dropdowns: [Dropdown options]

TABLE / LIST STRUCTURE:
- Columns: [Col 1, Col 2, Col 3...]
- Row Actions: [Action buttons per row]

UI/UX DESIGN GOAL:
[Specific visual presentation and layout objectives for Claude]
================================================================================
```

---

## SECTION 30 — COMPLETENESS AUDIT

- [x] All 11 ERP modules documented
- [x] All 15 major screens inventoried
- [x] All business fields cataloged across 9 core entity categories
- [x] Field relationships and dependency graphs documented
- [x] Action catalog documented with business conditions
- [x] Lifecycles and status transition diagrams documented
- [x] Business rules and validation criteria cataloged
- [x] Role and permission context specified
- [x] Core ERP end-to-end business journey detailed
- [x] Cross-module workflows (Customer-to-Cash, Fleet, Payroll) documented
- [x] Financial business context & invoice-receipt independence specified
- [x] Key calculations cataloged with exact formulas
- [x] Search, filter, and pagination behaviors documented
- [x] Table/List, Detail View, and Form contexts specified
- [x] Audit trail and lifecycle history requirements documented
- [x] Error, warning, and confirmation context specified
- [x] PDF, CSV, XLSX, and Print export capabilities documented
- [x] 6 Executive Dashboard perspectives specified
- [x] Typical business user journeys documented
- [x] TransaFlow business glossary completed
- [x] UI/UX designer constraints & forbidden changes specified
- [x] Design philosophy and reusable screen input template provided
- [x] **0 Code/Configuration Files Modified** (Read-only documentation creation)
