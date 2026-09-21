# PROJECT_CONTEXT.md

Transport ERP — project overview for developers and AI coding agents.

This file describes **what the product is**, **what already exists in the repository**, and **what is still planned**. It does not invent modules that are not in the code.

---

## 1. Project purpose

This is a **personal Transport ERP** being built for a family transport business.

The business mainly transports **construction materials** such as:

- M-Sand
- Gravel
- Jalli
- Other construction aggregates

The ERP should eventually cover the full commercial lifecycle from customer demand through delivery, driver settlement, invoicing, payment, accounts, and reports.

**Product name in code:** Transport ERP (`transport-frontend`, `transport-backend`).

---

## 2. Business domain

| Area | Meaning in this business |
|------|--------------------------|
| Customer | Construction companies / buyers who need material delivered |
| Material | Sold/transported goods (M-Sand, gravel, jalli, etc.) |
| Quarry | Loading source / supplier location |
| Booking | Customer order for material + transport |
| Vehicle / Driver | Fleet that executes trips |
| Trip / Dispatch / Delivery | Movement of material from quarry/loading point to delivery site |
| POD | Proof of delivery (document attached to a trip) |
| Expense / Fuel | Operating costs of vehicles, trips, and drivers |
| Invoice / Receipt | Customer billing and collections |
| Accounts | Chart of accounts + journal vouchers |
| Driver payroll | Monthly salary calculation and payment (current implementation) |

This is **not** a generic logistics marketplace. It is an internal ERP for one (or a few) transport companies, with **SaaS multi-tenant** company/branch isolation already in the backend.

---

## 3. Target users

Identified from roles, guards, and UI:

| User | Exists today | Typical use |
|------|----------------|-------------|
| Company admin / ADMIN | Yes | Full company operations |
| Branch manager | Yes | Branch-scoped operations (some screens) |
| Accountant | Yes | Payroll, invoices, receipts, journal |
| Fleet manager | Partial | Vehicle–driver assignment APIs |
| SUPER_ADMIN | Yes | Platform admin (SaaS), not company ERP menus |
| Operational clerk | Implied via authenticated users | Bookings, trips, fuel, expenses |
| Driver (self-service app) | **Planned / Not implemented yet** | Not identified as a separate login experience |

---

## 4. Main business activities

**Intended end-to-end flow (product goal):**

```text
Customer
→ Material Requirement
→ Booking
→ Vehicle & Driver Assignment
→ Quarry / Loading
→ Trip
→ Dispatch
→ Delivery
→ POD
→ Expenses
→ Driver Settlement
→ Invoice
→ Payment
→ Accounts
→ Reports
```

**What the live application actually supports today** (simplified):

```text
Masters (Customer, Material, Quarry, Vehicle, Driver)
→ Booking (material lines)
→ Trip (vehicle + driver on trip; optional vehicle–driver assignment master)
→ Trip documents (including POD as a document type)
→ Fuel + Expenses
→ Sales Invoice (can reference trips)
→ Customer Receipt + allocations + customer ledger
→ Journal vouchers / chart of accounts
→ Driver salary master + monthly payroll (DRAFT → APPROVED → PAID)
→ Dashboards / reports
```

Quarry, loading location, dispatch, delivery, and POD are **not separate operational modules**. They appear as masters, fields on trip details, trip status strings, or document types. See `BUSINESS_FLOW.md`.

---

## 5. Technology stack

| Layer | Actual stack |
|-------|----------------|
| Frontend | Angular **20.3** (standalone components), Angular Material, Tailwind CSS 4, RxJS 7.8 |
| Backend | Java **17**, Spring Boot **3.4.2**, Spring Web, Spring Security, Spring Data JPA, Validation |
| API docs | springdoc-openapi 2.5.0 (`/swagger-ui.html`) |
| Database | PostgreSQL (dev: database `transport_erp`) |
| Schema | Flyway (`V1` … `V53` under `transport-backend/src/main/resources/db/migration`) |
| Auth | JWT (stateless), BCrypt passwords, refresh tokens |
| Multi-tenancy | `companyId` / `branchId` on `BaseEntity`; `TenantAccessService.resolveCompanyId` |
| Version control | Git + GitHub |
| Dev tool | Cursor |

**Declared but unused:** MapStruct is in `pom.xml`. **No `*Mapper.java` files were found** in the backend source.

---

## 6. Current project status (from the repository)

The product is a **working multi-module ERP**, not a greenfield skeleton.

- Frontend and backend are separate projects in one Git repo.
- Flyway migrations exist through **V53** (vehicle odometer + maintenance rules).
- Core operations (booking, trip, fuel, expense, invoice, receipt, accounts) have controllers, entities, and Angular consoles.
- SaaS platform admin, subscriptions, setup wizard, and JWT tenant isolation exist.
- Fleet preventive-maintenance **due engine** exists (read-time; statuses `UNKNOWN` / `NOT_DUE` / `DUE_SOON` / `DUE` / `OVERDUE`).
- GPS tracking and AI prediction tables/APIs exist as **early/stub mobility features**, not a complete GPS product.
- Driver money is handled as **monthly payroll**, not as a full trip-wise settlement + driver ledger.

**Do not treat the intended lifecycle diagram as fully implemented.** Use the Existing vs Planned tables below.

---

## 7. Existing modules / features (discovered)

Grouped by what is actually in routes, controllers, and tables.

### Platform / tenancy

- Login, refresh, forgot/reset password
- Company, branch, users, roles, permissions
- Setup wizard (`/setup`)
- Subscription / renewal (`/renewal`)
- Platform admin (`/api/v1/platform-admin`, SUPER_ADMIN only)
- SaaS plans, licenses, billing invoices, announcements, support tickets, backups (platform entities)

### Masters

- Vehicle (documents, odometer readings, service logs, driver assignment, fitness/insurance/permit dates)
- Driver (documents, salary master, attendance)
- Customer (contacts, delivery sites, documents, ledger)
- Material, material prices, UOM + conversions
- Quarry
- Loading locations
- Lookups
- Suppliers
- Chart of accounts, financial years, app settings

### Operations

- Booking + booking details (material, qty, rates, royalty, loading, GST)
- Trip + trip details (dispatch/arrival timestamps on lines)
- Trip documents (`POD`, `DELIVERY_CHALLAN`, `WEIGHBRIDGE_SLIP`, photos, etc.)
- Fuel requests + fuel entries
- Expenses (categories include `DRIVER_BATA`)

### Finance

- Sales invoices + invoice lines (optional `trip_id`)
- Customer receipts + allocations to invoices
- Customer ledger (debit/credit running balance)
- Journal vouchers (single debit account + credit account + amount)
- Driver payroll (monthly; posts accrual and payment JVs)
- Financial reports / report templates / scheduled reports
- Dashboards (role-based; includes maintenance-due dashboard on admin/vehicle views)

### Fleet maintenance (recent)

- `vehicles.current_odometer_km`
- `vehicle_odometer_readings`
- `maintenance_rules` + `vehicle_maintenance_baselines`
- Read-time due calculation (not a work-order engine)
- `vehicle_services` (VehicleServiceLog) is **service history / accounting**, not the PM due engine

### UI

- App shell with grouped menus (Masters / Operations / Finance / Admin)
- Shared UI kit under `transport-frontend/src/app/shared-ui/` (`ff-*` components)
- Angular Material + Tailwind tokens

---

## 8. Planned modules / features

These are **product goals** or **recommended next modules**. They are **not** first-class entities/screens today unless noted.

| Area | Status |
|------|--------|
| Dedicated Material Requirement document (before booking) | Planned / Not implemented yet. Booking lines currently hold material demand. |
| Dedicated Dispatch module | Planned / Not implemented yet. Trip `status` + `trip_details.dispatch_time` exist. |
| Dedicated Delivery module | Planned / Not implemented yet. `trip_details.arrival_time` + delivery site on booking exist. |
| Dedicated POD workflow (signed, verified, blocking invoice) | Planned / Not implemented yet. POD exists only as `trip_documents.doc_type = 'POD'`. |
| Driver settlement engine (trip/KM/ton, bata, incentive, fine) | Planned / Not implemented yet. See §10. |
| Driver ledger (running balance like customer ledger) | Planned / Not implemented yet. |
| Per-trip / per-KM / per-ton pay configuration | Planned / Not implemented yet. |
| Driver self-service / mobile app | Planned / Not implemented yet. |
| GPS as operational tracking product | Tables/API stubs exist (`GpsTracking`); full product **not** identified. |
| AI predictions as operational product | Table/API stub exists (`AiPrediction`); full product **not** identified. |
| Multi-line journal (more than one debit/credit pair per voucher) | Planned / recommended. Current JV is two accounts + one amount. |
| Work orders for maintenance | `vehicle_odometer_readings.work_order_id` column exists; work-order module **not** identified. |

---

## 9. Important business concepts

| Concept | How it works in this ERP |
|---------|--------------------------|
| Company | Tenant. Almost all business rows carry `company_id`. |
| Branch | Optional scope (`branch_id`). Some dashboards filter by branch for BRANCH_MANAGER. |
| Soft delete | `is_deleted` on `BaseEntity`. Prefer filtering deleted rows; do not assume hard deletes. |
| Document numbers | Dedicated numbering (tenant-scoped). Do not invent ad-hoc codes. |
| Lookups | Status/type/category lists live in lookup tables, not only Java enums. |
| Status fields | Mostly **strings**, not JPA enums (booking, trip, invoice, expense, payroll). |
| Booking | Customer demand + commercial rates per material line. |
| Trip | Execution of a booking with vehicle/driver; one booking can have trips. |
| Invoice | Customer bill; lines may reference trips. |
| Receipt | Customer payment; allocations settle invoices; updates customer ledger. |
| Journal voucher | GL posting: one debit COA + one credit COA + amount. |
| Fitness vs PM due | Dashboard `maintenanceDueCount` is **fitness expiry**. PM due uses **`maintenanceDueDashboard`**. Do not mix them. |

---

## 10. Driver financial management

Mark every item as **Exists** or **Planned**.

| Item | Status | Where it lives today |
|------|--------|----------------------|
| Monthly salary | **Exists** | `driver_salaries.basic_salary`; copied onto `driver_payrolls.basic_salary` |
| Overtime rate | **Exists (rate only)** | `driver_salaries.overtime_rate`. A full overtime-hours calculation engine was **not currently identified**. |
| Per trip payment | Planned / Not implemented yet | — |
| Per KM payment | Planned / Not implemented yet | — |
| Per ton payment | Planned / Not implemented yet | — |
| Bata | **Partial** | Expense category `DRIVER_BATA` on `expenses`. Not a driver-settlement line type. |
| Allowance | **Exists (lump sum)** | `driver_payrolls.allowance_amount` |
| Trip incentive | Planned / Not implemented yet | — |
| Bonus | Planned / Not implemented yet | — |
| Driver advance | **Partial** | `driver_salaries.advance_taken` (stored amount). No `driver_advances` table. |
| Advance recovery | **Exists (lump sum)** | `driver_payrolls.advance_adjustment` |
| Deduction | **Exists (lump sum)** | `driver_payrolls.deduction_amount` |
| Fine | Planned / Not implemented yet | — |
| Adjustment | **Partial** | Covered by allowance / deduction / advance_adjustment on payroll |
| Salary payment | **Exists** | Payroll status `PAID` + payment journal voucher |
| Attendance | **Exists** | `driver_attendance`: `PRESENT`, `ABSENT`, `LEAVE`, `HALF_DAY` |
| Driver ledger | Planned / Not implemented yet | Customer ledger exists; driver ledger table does **not** |

### Driver settlement (current vs intended)

**Current implementation (`DriverPayrollService`):**

```text
DRAFT → APPROVED (posts accrual JV) → PAID (posts payment JV)
         ↘ cancel APPROVED/PAID with reversal JVs
```

- Only `DRAFT` can be edited or deleted.
- Approve posts an **accrual** journal voucher (`accrual_jv_number`).
- Pay posts a **payment** journal voucher (`payment_jv_number`).
- Cancel (non-draft) posts reversal JVs (`cancellation_jv_number`).
- Unique period: one payroll per `(driver_id, pay_year, pay_month)`.

**Intended future settlement lifecycle (product goal, not in code):**

```text
DRAFT → CALCULATED → SUBMITTED → APPROVED → POSTED → PAID
```

Do **not** implement the future statuses by renaming existing payroll statuses without an explicit feature design. They are different workflows.

See `BUSINESS_FLOW.md` for status meanings.

### Accounts integration (drivers)

**Exists:** payroll approve/pay/cancel writes `journal_vouchers` and stores JV numbers on the payroll row.

**Does not exist:** automatic posting of bata, trip incentives, or a driver sub-ledger account per driver. Expense `DRIVER_BATA` follows the **expense** posting path, not payroll.

---

## 11. Accounts integration (customers and GL)

| Flow | Exists? | Notes |
|------|---------|--------|
| Sales invoice | Yes | Header + lines; payment status `UNPAID` / `PARTIALLY_PAID` / `PAID` |
| Customer receipt + allocation | Yes | Allocations to invoices |
| Customer ledger | Yes | Debit/credit + running balance; links invoice and/or receipt |
| Chart of accounts | Yes | `ASSET`, `LIABILITY`, `EQUITY`, `INCOME`, `EXPENSE` (string) |
| Journal voucher | Yes | Two-sided single amount |
| Expense posting | Yes | Expense module with statuses including `SUBMITTED` / `APPROVED` / `PAID` |
| Fuel as GL | Partial | Fuel entries exist; do not assume every fuel row posts a JV unless the service does so (verify in code before changing) |

---

## 12. Important project assumptions

1. **One Git repository**, two apps: `transport-frontend` and `transport-backend`.
2. **PostgreSQL is the source of truth.** Hibernate `ddl-auto` is `validate`. Schema changes go through **Flyway only**.
3. **Tenant isolation is mandatory.** Never trust client-supplied `companyId` for non–SUPER_ADMIN users. Use `TenantAccessService.resolveCompanyId`.
4. **`main` is the stable branch.** Feature work belongs on `feature/...` branches.
5. **Do not confuse fitness alerts with PM due.** Different APIs and meanings.
6. **VehicleServiceLog ≠ preventive maintenance due.** Service logs are history/cost; due engine uses rules + baselines + odometer.
7. **Status values are strings.** Extending a status (e.g. trip `IN_TRANSIT`) may already be used in dashboards even if an old migration comment lists fewer values. Search the code before changing status sets.
8. **POD is a document type**, not a separate table.
9. **GPS/AI folders are not proof of a finished product.** Treat as stubs unless a feature explicitly completes them.
10. **Existing frontend docs** live under `transport-frontend/docs/` and `transport-backend/docs/` (lock notes, deploy). This `docs/` folder at repo root is the **canonical product context** for AI agents. Do not duplicate these five files elsewhere.

---

## 13. Repository map (high level)

```text
Mohan Programs/
├── docs/                          ← this documentation set
├── transport-frontend/            ← Angular 20 app
│   ├── src/app/components/        ← feature consoles
│   ├── src/app/services/          ← HTTP services
│   ├── src/app/shared-ui/         ← ff-* design system
│   ├── src/app/guards/
│   └── src/app/interceptors/
└── transport-backend/             ← Spring Boot API
    ├── src/main/java/com/transport/erp/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── model/
    │   ├── dto/
    │   ├── security/
    │   ├── exception/
    │   └── config/
    └── src/main/resources/db/migration/   ← Flyway V1–V53
```

---

## 14. AI coding-agent instructions

When changing this project:

1. Read this file, then `ARCHITECTURE.md` and `DEVELOPMENT_RULES.md`.
2. Search existing controllers/services/components **before** adding new ones.
3. Follow `Controller → Service → Repository → Entity` and Angular console + `*-mgmt.service.ts` patterns.
4. Wrap API results in `ApiResponse<T>` (`success`, `message`, `data`, `errors`).
5. Scope every query by tenant (`companyId`); use `assertOwned` after load-by-id.
6. Use transactions for payroll, receipts, invoices, and journal posting.
7. Never silently rewrite financial history. Prefer new reversing entries.
8. Do not treat planned driver-settlement statuses as if they already exist.
9. Do not modify production data. Do not change schema except via a new Flyway version.
10. Do not mix unrelated UI restyles with feature commits.
11. Never claim tests passed unless they were actually run.
12. Do not commit or push unless the user explicitly asks.

If something is unclear from the repo, write **Not currently identified in the codebase** rather than guessing.
