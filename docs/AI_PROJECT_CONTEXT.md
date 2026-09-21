# PERMANENT PROJECT CONTEXT & CURSOR AI HANDOFF SPECIFICATION

> **PROJECT**: TransaFlow Transport ERP / SaaS ERP  
> **REPOSITORY**: `Mohanrajmohanms7/Trasport-ERP`  
> **PRIMARY REPOSITORY PATH**: `d:\Mohan Programs`  
> **LAST UPDATED**: 2026-09-19  

---

# SECTION 1 — PROJECT OVERVIEW

- **Project Name**: TransaFlow Transport ERP (SaaS Multi-Tenant Transport & Logistics Management System)
- **Product Purpose**: Production-ready, enterprise-grade multi-tenant SaaS ERP engineered for transport operators, fleet owners, sand/aggregate quarry logistics, dispatchers, accountants, and drivers.
- **Main Business Domain**: Heavy transport dispatching, fleet management, material quarry billing, driver payroll, fuel tracking, expense logging, customer credit management, formal double-entry journal voucher accounting, financial reporting, and document management.
- **Target Users**:
  - `TENANT_ADMIN` / `ADMIN`: Company Administrators & Executive Managers
  - `OWNER`: Fleet Owners & Financial Investors
  - `OPERATIONS` / `DISPATCHER`: Logistics Operations Managers & Quarry Dispatchers
  - `FLEET_MANAGER` / `VEHICLE`: Workshop Managers & Fleet Maintenance Controllers
  - `ACCOUNTANT`: Chief Accountants & Billing Clerks
  - `DRIVER`: Vehicle Operators & Haulers
  - `SUPER_ADMIN`: SaaS Platform Administrator
- **Major Modules**:
  1. *Master Management*: Materials (CFT/UNIT/TON/BAG/KG), Vehicles, Drivers, Customers, Delivery Sites, Chart of Accounts, UOM Master & Material-Based UOM Conversions.
  2. *Booking & Order Management*: Customer Bookings, Site Deliveries, Rate Agreements, Priority Scheduling.
  3. *Dispatch & Trip Management*: Vehicle-Driver Pairing, Loading Queues, Trip Dispatches, Delayed Trip Tracking, Proof of Delivery (POD) Documents.
  4. *Quarry & Material Operations*: Sand/Aggregate Loading, Density-Based Conversions (CFT ↔ Unit ↔ Ton).
  5. *Fuel & Expense Management*: Fuel Station Logs, Lube/Maintenance Logs, Driver Advances, Direct Operating Expenses.
  6. *Driver Payroll*: Basic Salary, Allowance, Deductions, Advance Adjustments, Salary Slips (PDF + Print).
  7. *Sales Billing & Invoicing*: Automated GST Sales Invoices, Subtotal/Tax/Discount Calculations, Multi-item Invoice Lines.
  8. *Customer Ledger & Receipts*: Customer Payment Receipts, Invoice Allocation, Credit Balance Tracking, Ledger Statements (PDF + CSV + Print).
  9. *Double-Entry Accounting & Financial Reports*: Journal Vouchers (JV), Trial Balance, General Ledger (GL), Profit & Loss (P&L), Balance Sheet, Financial Year Protection (Closed-period locking).
  10. *Native Document Exports & Reporting*: Apache POI Native `.xlsx` Exports across all 13 modules, OpenPDF PDF Generators, Browser Print Engine.

- **Current Project Maturity/Status**:
  - Backend: **95/95 Unit & Integration Tests PASS**
  - Frontend: **100% SUCCESS Production Build (`npm run build`)**
  - Database: **Flyway V50 Schema Complete**
  - Roadmap: **Steps 1 through 13 FULLY LOCKED**

---

# SECTION 2 — TECHNOLOGY STACK

### Frontend
- **Framework**: Angular v20.3.0 (Standalone Components architecture)
- **TypeScript**: v5.9.2
- **Node.js**: v20+
- **Build Engine**: `@angular/build` v20.3.10 / Vite / esbuild
- **UI Frameworks**: Angular Material v20.2.14 (`@angular/material`, `@angular/cdk`), Tailwind CSS v4.3.3 (`@tailwindcss/postcss` v4.3.3, PostCSS v8.5.23)
- **State Management**: Angular Signals (`signal`, `computed`, `inject`)
- **Reactive Stream**: RxJS v7.8.0

### Backend
- **JDK**: Java 17 LTS
- **Framework**: Spring Boot v3.4.2
- **ORM / Data Access**: Spring Data JPA, Hibernate 6 (`jackson-datatype-hibernate6`)
- **Security**: Spring Security 6 (JWT, Dynamic Role/Tenant Authorities, `@PreAuthorize`)
- **JSON Processing**: Jackson Databind with custom LocalDateTime/LocalDate serializers
- **Document Generators**:
  - Apache POI OOXML v5.2.5 (`org.apache.poi:poi-ooxml`) for Native `.xlsx` Export Engine
  - OpenPDF v1.3.30 (`com.lowagie:text`) for Vector PDF Generation
- **Mapping & Utilities**: Lombok v1.18.36, MapStruct v1.5.5.Final
- **Build Tool**: Apache Maven (Wrapper included: `.\mvnw.cmd`)

### Database
- **Database Engine**: PostgreSQL 14+ (Local dev: `localhost:5432/transport_erp`, User: `transport_admin`)
- **Database Migration Framework**: Flyway (`flyway-core`, `flyway-database-postgresql` v50)
- **Database Conventions**: Snake_case table & column naming, strict Foreign Key constraints, logical soft-deletes (`is_deleted = false`), optimistic locking (`version`), immutable auditing fields (`created_by`, `created_date`, `updated_by`, `updated_date`).

---

# SECTION 3 — PROJECT STRUCTURE

```text
Mohan Programs/
├── transport-backend/                      # Spring Boot Java 17 Backend
│   ├── src/main/java/com/transport/erp/
│   │   ├── config/                         # Security & Web MVC Configurations
│   │   ├── controller/                     # REST API Controllers (14 Controllers)
│   │   ├── dto/                            # Request & Response DTOs
│   │   ├── exception/                      # GlobalExceptionHandler & Custom Exceptions
│   │   ├── model/                          # JPA Entities (Inheriting BaseEntity)
│   │   ├── repository/                     # Spring Data JPA Repositories
│   │   ├── security/                       # TenantAccessService, JWT Filters, Security User
│   │   ├── service/                        # Authoritative Business & Accounting Services
│   │   └── util/                           # OpenPDF & POI Export Utility Generators
│   └── src/main/resources/
│       ├── application.properties          # Server & Datasource Configuration
│       └── db/migration/                   # Flyway Migration Scripts (V1__... to V50__...)
│
├── transport-frontend/                     # Angular 20 Frontend Single Page Application
│   ├── src/app/
│   │   ├── components/                     # Feature Console Components (20 Consoles)
│   │   │   ├── dashboard/                  # Executive & Operational Telemetry Dashboard
│   │   │   ├── booking-details-console/    # Customer Bookings Console
│   │   │   ├── trip-details-console/       # Dispatch & Trip Management Console
│   │   │   ├── invoice-details-console/    # Sales Invoice Console
│   │   │   ├── customer-details-console/   # Customer & Ledger Console
│   │   │   ├── vehicle-details-console/    # Fleet & Vehicle Roster Console
│   │   │   ├── driver-details-console/     # Driver Roster & Payroll Console
│   │   │   ├── fuel-details-console/       # Fuel Logs & Fulfillment Console
│   │   │   ├── expense-details-console/    # Operating Expense Logs Console
│   │   │   ├── payment-details-console/    # Customer Receipts & Allocation Console
│   │   │   ├── accounts-details-console/   # COA & Journal Voucher Console
│   │   │   ├── report-details-console/     # Financial Reports Presentation Console
│   │   │   └── master-management/          # Materials, Sites & Master Data Console
│   │   ├── services/                       # Injectable Angular Services (HttpClient callers)
│   │   ├── shared-ui/                      # Reusable Form & Layout Base Components
│   │   └── app.routes.ts                   # Angular Router Configuration with setupGuard
│   └── package.json                        # Dependencies & Scripts
│
└── docs/                                   # Project Documentation Directory
    └── AI_PROJECT_CONTEXT.md               # Cursor AI Master Reference Context
```

### Where Developers / AI Agents Should Look:
- **UI & Layout Changes**: `transport-frontend/src/app/components/<feature-console>/`
- **Frontend REST Calls**: `transport-frontend/src/app/services/<feature>.service.ts`
- **Backend API Endpoints**: `transport-backend/src/main/java/com/transport/erp/controller/<Feature>Controller.java`
- **Authoritative Business Rules**: `transport-backend/src/main/java/com/transport/erp/service/<Feature>Service.java`
- **Tenant & Role Security Enforcement**: `TenantAccessService.java` & Spring Security annotations
- **Database Schema Changes**: `transport-backend/src/main/resources/db/migration/V<N>__<description>.sql`

---

# SECTION 4 — CORE BUSINESS FLOW

```text
[Master Management]
(Materials + Rates + UOM + Vehicles + Drivers + Customers + Delivery Sites)
                        │
                        ▼
[Customer Booking Order]  ──►  [Quarry Loading Queue]
                        │
                        ▼
[Trip Dispatch Assignment]  ──►  [Fuel & Operating Expenses]
(Vehicle + Driver Pairing)       (Fuel Station Logs, Driver Advance)
                        │
                        ▼
[Proof of Delivery (POD)]  ──►  [Driver Payroll Generation]
(Weighbridge Slips / Photos)     (Allowance, Salary Slip PDF)
                        │
                        ▼
[Sales Invoice Generation]
(Subtotal + Tax + Discounts)
                        │
                        ▼
[Customer Receipt Payment]
(Cash / Bank Allocation)
                        │
                        ▼
[Customer Ledger Posting]
(Debit / Credit / Running Balance)
                        │
                        ▼
[General Ledger & Financial Year Locking]
(Journal Vouchers, Trial Balance, P&L, Balance Sheet)
```

---

# SECTION 5 — MULTI-TENANT / SECURITY MODEL

- **Tenant Isolation Authority**: [`TenantAccessService.java`](file:///d:/Mohan%20Programs/transport-backend/src/main/java/com/transport/erp/security/TenantAccessService.java) is the single authoritative gateway for multi-tenant data boundaries.
- **Tenant Scoping Method**: `tenantAccess.resolveCompanyId(suppliedCompanyId)` validates the requested `companyId` against the authenticated session token. If a user attempts to access or tamper with data belonging to another tenant (`companyId`), an `AccessDeniedException` (HTTP 403) is thrown immediately.
- **Branch Scoping**: User context tracks `branchId`. Operations and reporting resolve branch parameters securely from DB-backed user context.
- **Authentication**: JWT Bearer Token validation in `JwtAuthenticationFilter.java` and Spring Security Context `SecurityContextHolder`.

> [!CAUTION]
> **CRITICAL MANDATE FOR AI AGENTS**: AI agents must NEVER bypass `TenantAccessService` or write custom un-scoped SQL/JPA queries that omit `companyId` filtering (`findByCompanyIdAndIsDeletedFalse`). Frontend-supplied IDs must NEVER be trusted without backend tenant validation.

---

# SECTION 6 — ROLES & PERMISSIONS

1. `TENANT_ADMIN` / `ADMIN`: Full operational and financial management access across company entities.
2. `OWNER`: Executive oversight, financial investor metrics, P&L views, fleet margin analytics.
3. `OPERATIONS` / `DISPATCHER`: Booking creation, trip dispatch scheduling, quarry loading queues, POD uploads.
4. `FLEET_MANAGER` / `VEHICLE`: Fleet vehicle maintenance logs, vehicle-driver pairing, permit expiry tracking.
5. `ACCOUNTANT`: Sales invoice generation, customer receipts, expense entries, payroll approvals, JV postings, financial reports.
6. `DRIVER`: Assigned trip schedules, fuel entry logs, attendance tracking, salary slip downloads.
7. `SUPER_ADMIN`: Cross-tenant platform maintenance and SaaS administration.

---

# SECTION 7 — COMPLETED ROADMAP

- **Step 1**: Multi-Tenant Security & Tenant Isolation Engine
- **Step 2**: Master Data Infrastructure (Materials, Customers, Drivers, Vehicles)
- **Step 3**: Material Rate Agreements & Delivery Sites
- **Step 4**: Customer Booking Lifecycle & Status Engine
- **Step 5**: Trip Dispatch & Vehicle-Driver Assignment Lifecycle
- **Step 6**: Fuel Logging & Fuel Request Fulfillment System
- **Step 7**: Operating Expense Tracking & Vehicle Service Log Integrity
- **Step 8**: Sales Invoice & Automated Tax Billing Engine
- **Step 9**: Customer Payment Receipts & Invoice Allocation Engine
- **Step 10**: Driver Payroll & Advance Adjustment Engine (Lock Commit: `ed9fa6f`)
- **Step 11**: Double-Entry Chart of Accounts & Journal Voucher System
- **Step 12**: Vehicle Driver Assignment & Fleet Pairing Lifecycle (Lock Commit: `7c9453a7...`)
- **Step 13**: Financial Year Integrity & Closed-Period Protection (Lock Commit: `2fa88422...`)

---

# SECTION 8 — LOCKED — DO NOT MODIFY WITHOUT EXPLICIT APPROVAL

> [!IMPORTANT]
> **THE FOLLOWING COMPLETED STEPS AND COMMITS ARE FULLY LOCKED AND PRODUCTION-VERIFIED. FUTURE DEVELOPMENT MUST NOT SILENTLY REFACTOR, MODIFY, OR ALTER THEIR BUSINESS LOGIC.**

If a user prompt or task appears to require changes to a locked step or feature, the AI agent **MUST STOP AND REPORT THE CONFLICT** rather than silently changing the code.

### Verified Lock Commits:
- **Step 10 (Driver Payroll Integrity)**: Commit [`ed9fa6f65792348d8b87519e86d35a618a0ad798`](file:///d:/Mohan%20Programs)
- **Step 12 (Vehicle Driver Assignment)**: Commit [`7c9453a7ceaf05a5e40c608f6718dd9476cc0058`](file:///d:/Mohan%20Programs)
- **Step 13 (Financial Year Closed-Period Protection)**: Commit [`2fa8842dd1c8c923f002f0db472968de07404b21`](file:///d:/Mohan%20Programs)

---

# SECTION 9 — IMPORTANT COMPLETED FEATURES

| Feature Name | Description | Key Files | Commit Hash | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Financial Reports PDF/CSV/Print & V50** | Formal Trial Balance, GL, P&L, Balance Sheet exports & Flyway V50 `trip_documents` schema fix | `FinancialReportController.java`, `FinancialReportPdfGenerator.java`, `V50__...sql` | `554433fa0ac745b0fd345e6add8f1e3ddd5ca338` | **LOCKED & VERIFIED** |
| **Native XLSX Export Engine** | Production-ready POI `.xlsx` exports across 13 ERP modules with cell formatting & tenant security | `XlsxExportService.java`, `XlsxExportTest.java` | `21fbd786333fe3f94a5aee076ed54f566df28a3a` | **LOCKED & VERIFIED** |
| **Driver Salary Slip PDF + Print** | Vector PDF generator and browser print engine for driver monthly salary slips | `DriverPayrollController.java`, `DriverSalarySlipPdfGenerator.java` | `2bbfc44c57f5e706db22cf15d302184e5eceeaf9` | **LOCKED & VERIFIED** |
| **UOM Master & Material Conversions** | Material-based Unit of Measurement Master & Density conversion engine (1 Unit = 100 CFT ≈ 4.53 Tons) | `UomMaster.java`, `UomService.java`, `V51__...sql` | `4aad355d440430bf707b51a5ad66e3b54c064537` | **LOCKED & VERIFIED** |
| **Customer Ledger PDF/CSV/Print** | Statement of Customer Account exports with debit/credit running balance presentation | `CustomerLedgerController.java`, `CustomerLedgerPdfGenerator.java` | `5ddbe31724636c5f5c0336c73d100ff9ef2dc174` | **LOCKED & VERIFIED** |
| **Sales Invoice PDF + Print** | Official tax invoice PDF generator with GST breakdown and company branding | `SalesInvoiceController.java`, `SalesInvoicePdfGenerator.java` | `263e78b6d0e5b93d74e525ce2c9f4b4bd367aeec` | **LOCKED & VERIFIED** |

---

# SECTION 10 — ACCOUNTING / FINANCIAL RULES

1. **Read-Only Reporting**: All financial reports, ledger statements, and export generators are **100% READ-ONLY**. Backend database values are authoritative. Frontend components must NEVER perform accounting calculations.
2. **Immutability of Financial Records**: Financial transactions (Invoices, Receipts, Expenses, JVs) cannot be hard-deleted. Corrections must be executed via formal cancellation or reversal transactions.
3. **Receipt Allocation Independence**: Cancelling an invoice does NOT automatically reverse a customer receipt; receipt allocation entries must be handled independently.
4. **Closed-Period Posting Protection**: Transactions dated within a closed Financial Year or locked accounting period are blocked from creation, modification, or reversal.
5. **Double-Entry Journal Balancing**: Every Journal Voucher must satisfy `Total Debits == Total Credits` prior to posting.

---

# SECTION 11 — DATABASE CONVENTIONS

- **BaseEntity**: All JPA entities extend `BaseEntity.java` which encapsulates:
  - `id`: `BIGSERIAL` primary key.
  - `companyId`: `BIGINT NOT NULL` (Tenant boundary).
  - `branchId`: `BIGINT` (Branch boundary).
  - `code`, `name`, `description`, `status`: Base master properties.
  - `isDeleted`: `BOOLEAN DEFAULT FALSE` (Soft deletion flag).
  - `version`: `@Version INT` (Optimistic locking).
  - `createdBy`, `createdDate`, `updatedBy`, `updatedDate`: Audit trail.
- **Flyway Migrations**: SQL files located in `src/main/resources/db/migration/` using strict versioning `V1__...`, `V2__...`, `V50__...`. Migrations must be idempotent (`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`).

---

# SECTION 12 — BACKEND CODING PATTERNS

- **Service Layer Scoping**: Service methods fetching lists or counts must resolve `companyId` via `tenantAccess.resolveCompanyId(suppliedCompanyId)` and invoke repository methods containing `findByCompanyIdAndIsDeletedFalse`.
- **Response Format**: Controller endpoints return standardized `ApiResponse<T>` wrappers:
  ```java
  return ApiResponse.success(data, "Message");
  ```
- **Read-Only Transactions**: Read operations are explicitly annotated `@Transactional(readOnly = true)`.
- **Batch Processing**: Database queries fetching bulk data for export generators use `PageRequest.of(0, 5000)` to safeguard memory while fetching complete tenant datasets.

---

# SECTION 13 — FRONTEND CODING PATTERNS

- **Standalone Components**: All Angular components use `standalone: true` and declare explicit imports (`CommonModule`, Material Modules).
- **Reactive State via Signals**: Components use Angular Signals (`signal<T>()`, `computed()`) for reactive rendering rather than manual subscription state where applicable.
- **Binary File Downloads**: Blob HTTP requests set `responseType: 'blob'` and trigger browser save downloads via standard Blob object URLs.
- **Styling & Layout**: Tailwind CSS utility classes combined with custom dark-mode selectors (`dark:bg-slate-900`, `dark:border-slate-800`).

---

# SECTION 14 — PERFORMANCE RULES

1. **Zero N+1 Queries**: Avoid querying database entities inside Java loops. Use repository `@Query` joins or bulk ID list lookups (`findAllById`).
2. **Bounded Auto-Column Sizing**: In POI Excel export generators, column widths must be bounded (e.g., min 3,000, max 12,000 width units) to prevent expensive auto-sizing performance overhead.
3. **No Unnecessary Polling**: Background tasks notify execution automatically. Do NOT write looping `while(true)` polling loops in code.

---

# SECTION 15 — AI AGENT DEVELOPMENT RULES

All future Cursor or Antigravity AI agents MUST adhere to these 18 rules:

1. **Inspect Before Mutating**: Thoroughly read authoritative files before attempting edits.
2. **Reuse Existing Architecture**: Reuse `TenantAccessService`, existing repositories, and DTOs instead of creating parallel frameworks.
3. **Preserve Business Logic**: Never alter existing validation rules or calculations unless explicitly instructed by the user.
4. **Respect Locked Features**: Never modify locked Steps 1–13 or locked features (`4aad355`, `2bbfc44`, `21fbd78`, `554433f`).
5. **No Invented API Fields**: Never add non-existent fields to DTOs or endpoints without checking backend schemas.
6. **No Invented DB Columns**: Never reference database columns without verifying Flyway migration files.
7. **No Invented Permissions**: Stick strictly to established Spring Security roles.
8. **No Invented Accounting Formulas**: Accounting calculations belong strictly in authoritative backend services.
9. **No Duplicate Helper Services**: Audit existing services before creating new utility classes.
10. **Avoid Unnecessary Refactoring**: Keep edits minimal, clean, and tightly targeted to the user's specific request.
11. **Avoid Unnecessary Dependencies**: Do not introduce new npm or Maven packages unless explicitly requested.
12. **Prevent N+1 Database Calls**: Collect IDs and execute bulk repository operations.
13. **Preserve Tenant Scoping**: Always pass `companyId` to backend data access methods.
14. **Preserve RBAC Authorization**: Enforce `@PreAuthorize` annotations on REST controllers.
15. **Preserve API Response Wrappers**: Maintain standard `ApiResponse<T>` contracts.
16. **Gather Empirical Runtime Verification**: Run `mvn test` and `npm run build` to verify changes before declaring completion.
17. **Audit Git Diff**: Inspect `git status` and `git diff` before completing tasks.
18. **No Unauthorized Commits/Pushes**: NEVER commit or push code unless the user explicitly gives a command to do so.

---

# SECTION 16 — CURRENT DASHBOARD REDESIGN WORKFLOW

The ongoing UI/UX modernization workflow follows a strict 5-stage pipeline:

```text
Current Angular Dashboard Component (dashboard.ts, dashboard.html)
                       │
                       ▼
Stage 1: Antigravity Technical & Functional Audit (15-Phase Analysis)
                       │
                       ▼
Stage 2: Claude Visual Design Specification (Design System & Mockup Guidelines)
                       │
                       ▼
Stage 3: Dashboard Mockup & Visual Alignment
                       │
                       ▼
Stage 4: Antigravity / Cursor UI-Only Implementation (Tailwind HTML/CSS Edits)
                       │
                       ▼
Stage 5: Functional Verification (Signals, REST Endpoints & Build Check)
```

> **CRITICAL RULE**: Claude's design specification controls **VISUAL PRESENTATION ONLY**. Existing backend REST endpoints, active signal states (`activeRole()`, `metrics()`), role-switching logic, and quick action routing paths remain authoritative and MUST BE 100% PRESERVED.

---

# SECTION 17 — CLAUDE DASHBOARD DESIGN RULES

When implementing visual improvements for the Enterprise Dashboard, follow these core design principles:
- **Design System**: Use modern Tailwind CSS palette (Dark slate `#0F172A` cards, Slate 200/800 subtle borders, Blue `#2563EB` primary accent).
- **KPI Cards**: Elevate KPI cards with distinct translucent icons, numerical typography (`text-2xl font-extrabold`), subtexts, and trend indicators.
- **Charts & Gauges**: Upgrade custom SVG polyline trend charts with smooth SVG areas, data points, and clear axis labels. Preserve circular SVG donut utilization gauges.
- **Action Hub**: Group quick task buttons logically with distinct color badges and hover states.
- **Feed Lists**: Design clean vertical timelines for Critical System Alarms and Live Operation Audit Logs.

---

# SECTION 18 — CURRENT GIT STATE

- **Current Branch**: `main`
- **HEAD Commit**: [`554433fa0ac745b0fd345e6add8f1e3ddd5ca338`](file:///d:/Mohan%20Programs) (`feat: Implement Financial Reports PDF CSV Print and V50 migration`)
- **Working Tree Status**: **CLEAN** (0 modified files, 0 untracked files prior to creating this context document).
- **Recent Locked Commits**:
  - `554433f` - Implement Financial Reports PDF CSV Print and V50 migration
  - `21fbd78` - Implement native XLSX export across ERP
  - `2bbfc44` - Implement Driver Salary Slip PDF and Print functionality
  - `4aad355` - Implement UOM master and material-based conversion engine
  - `2fa8842` - Finalize ERP step 13 financial year integrity
  - `7c9453a` - Finalize ERP step 12 vehicle driver assignment integrity

---

# SECTION 19 — KNOWN RISKS & WARNINGS

1. **Tenant Data Leakage**: Omitting `companyId` filtering in custom database queries risks exposing tenant data across multi-tenant boundaries.
2. **Accounting Ledger Discrepancies**: Bypassing `FinancialReportService` or `CustomerLedgerService` when building export renderers can cause discrepancies between displayed totals and formal General Ledger records.
3. **Flyway Migration Collisions**: Adding out-of-order Flyway migrations or modifying already-applied migration SQL files will break database schema validation on application startup.
4. **Breaking API Contracts**: Renaming DTO properties breaks Angular service typings and requires cascading edits across frontend services.

---

# SECTION 20 — HOW CURSOR SHOULD START FUTURE TASKS

```text
1. Read `/docs/AI_PROJECT_CONTEXT.md` to establish project context.
2. Run `git status` to verify current working tree state.
3. Inspect relevant Angular components (`/transport-frontend`) or Spring Boot services (`/transport-backend`).
4. Identify if any requested change impacts a LOCKED feature (Steps 1–13, UOM, Driver Salary Slip, XLSX, Financial Reports). If so, notify the user immediately.
5. Implement the smallest safe, targeted code edit.
6. Verify runtime integrity (`mvn test` for backend, `npm run build` for frontend).
7. Review `git diff` to ensure zero unintended side effects.
8. Report changes and verification results to the user cleanly.
9. WAIT for explicit user instruction before staging, committing, or pushing.
```
