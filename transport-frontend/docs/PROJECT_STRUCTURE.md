# FleetFlow ERP — Complete Project Structure Document

**System:** FleetFlow ERP (Transport Management)  
**Repositories:**
- Frontend: `D:\Mohan Programs\transport-frontend`
- Backend: `D:\Mohan Programs\transport-backend`

**Stack summary**

| Layer | Technology |
|-------|------------|
| Frontend | Angular 20, TypeScript 5.9, Tailwind CSS 4, Angular Material 20 |
| UI Framework | `@ff/ui` (in-repo shared-ui library) |
| Backend | Spring Boot 3.4.2, Java 21 |
| Security | Spring Security + JWT (stateless) |
| Persistence | Spring Data JPA, Flyway, PostgreSQL 17 |
| API Docs | springdoc-openapi / Swagger UI |
| Local proxy | Angular `proxy.conf.json` → `http://localhost:8080` |

**Default credentials:** `admin` / `Admin@123`  
**Dev URLs:** UI `http://localhost:4200` · API `http://localhost:8080` · Swagger `/swagger-ui.html`

---

## 1. High-level architecture

```
Browser (Angular :4200)
    │  /api/*  (proxy)
    ▼
Spring Boot API (:8080)
    │
    ├─ Security (JWT filter)
    ├─ Controllers → Services → Repositories
    └─ PostgreSQL (Flyway migrations)
```

```
Mohan Programs/
├── transport-frontend/     # Angular SPA + @ff/ui
└── transport-backend/      # Spring Boot REST API
```

---

## 2. Frontend structure (`transport-frontend`)

### 2.1 Root

```
transport-frontend/
├── angular.json              # Build / serve (proxyConfig)
├── package.json
├── proxy.conf.json           # /api → localhost:8080
├── tsconfig.json             # paths: @ff/ui → shared-ui
├── tsconfig.app.json
├── Dockerfile
├── nginx.conf
├── public/                   # Static assets (favicon, login_bg.jpg)
├── docs/
│   └── PROJECT_STRUCTURE.md  # This document
└── src/
    ├── index.html
    ├── main.ts
    ├── styles.css            # Global + FF tokens import
    ├── environments/
    └── app/
```

### 2.2 Application (`src/app`)

```
src/app/
├── app.ts / app.html / app.config.ts / app.routes.ts
├── auth/                     # Login & account screens
├── layout/                   # App shell (sidebar, theme toggle)
├── guards/                   # authGuard, setupGuard
├── interceptors/             # jwt.interceptor (skips public auth URLs)
├── services/                 # HTTP API clients
├── components/               # Feature screens / consoles
├── shared/                   # Legacy shared widgets
└── shared-ui/                # FleetFlow UI framework (@ff/ui)
```

### 2.3 Auth

| Path | Component | Purpose |
|------|-----------|---------|
| `/login` | `auth/login` | Sign in |
| `/forgot-password` | `auth/forgot-password` | Request reset |
| `/reset-password` | `auth/reset-password` | Apply reset token |
| `/access-denied` | `auth/access-denied` | 403 UI |
| `/profile` | `auth/profile` | User profile (inside shell) |

### 2.4 Layout & guards

| Item | Role |
|------|------|
| `layout/app-shell` | Sidebar nav, header search, dark/light via `FfThemeService` |
| `guards/auth.guard.ts` | Requires JWT session |
| `guards/setup.guard.ts` | Blocks dashboard until company setup complete |
| `interceptors/jwt.interceptor.ts` | Attaches `Bearer` token; **never** on login/refresh/forgot/reset |

### 2.5 Feature components & routes

| Route | Component folder | Domain |
|-------|------------------|--------|
| `/dashboard` | `dashboard` | KPI overview |
| `/setup` | `setup-wizard` | First-run company/branch/fleet setup |
| `/masters` | `master-management` | Generic master CRUD (branch, vehicle, driver, customer, etc.) |
| `/company-admin` | `company-administration` | Company, branches, FY, settings |
| `/users-roles` | `user-role-management` | Users, roles, permissions |
| `/vehicles` | `vehicle-details-console` | Documents, service logs, driver assignment |
| `/drivers` | `driver-details-console` | Driver Master (FF reference screen) |
| `/customers` | `customer-details-console` | Sites, contacts, documents |
| `/materials-quarries` | `material-quarry-console` | Materials, quarries, prices |
| `/bookings` | `booking-details-console` | Booking headers & lines |
| `/trips-planning` | `trip-details-console` | Trip allocation / dispatch |
| `/fuel-logs` | `fuel-details-console` | Fuel entries & requests |
| `/expense-logs` | `expense-details-console` | Expense vouchers |
| `/payment-logs` | `payment-details-console` | Receipts & customer ledger |
| `/billing-invoices` | `invoice-details-console` | Sales invoices |
| `/accounts-ledger` | `accounts-details-console` | Chart of accounts, journals |
| `/reports-bi` | `report-details-console` | Report templates & schedules |
| `/mobility-ai` | `mobility-details-console` | GPS + AI predictions |
| `/ui-playground` | `shared-ui/playground` | FF component playground |

### 2.6 Frontend services (`src/app/services`)

| Service | Typical API prefix |
|---------|-------------------|
| `auth.service.ts` | `/api/v1/auth` |
| `setup.service.ts` | `/api/v1/setup` |
| `dashboard.service.ts` | `/api/v1/dashboard` |
| `master.service.ts` | masters (vehicles, drivers, customers, …) |
| `company-admin.service.ts` | companies, branches, FY, settings |
| `user-role.service.ts` | users, roles, permissions |
| `vehicle-mgmt.service.ts` | vehicles + nested docs/service/driver |
| `driver-mgmt.service.ts` | drivers + docs/attendance/salary |
| `customer-mgmt.service.ts` | customers + sites/contacts/docs |
| `material-mgmt.service.ts` | materials, quarries, prices |
| `booking-mgmt.service.ts` | `/api/v1/bookings` |
| `trip-mgmt.service.ts` | `/api/v1/trips` |
| `fuel-mgmt.service.ts` | `/api/v1/fuel` |
| `expense-mgmt.service.ts` | `/api/v1/expenses` |
| `payment-mgmt.service.ts` | receipts, ledger |
| `invoice-mgmt.service.ts` | `/api/v1/invoices` |
| `accounts-mgmt.service.ts` | accounts, journal |
| `report-mgmt.service.ts` | reports |
| `mobility-mgmt.service.ts` | GPS, AI |

### 2.7 Legacy shared (`src/app/shared`)

Older presentational helpers still used by some consoles:

- `data-table`, `page-header`, `kpi-stat-card`, `empty-state`
- `detail-drawer`, `sectioned-form-card`, `document-upload-tile`
- `status-pill`, `skeleton-loader`, `confirmation-dialog`

> Prefer `@ff/ui` for new work (Driver Master is the reference).

---

## 3. FleetFlow UI Framework (`@ff/ui`)

**Root:** `src/app/shared-ui/`  
**Import:** `import { … } from '@ff/ui'`  
**Alias:** `tsconfig.json` → `src/app/shared-ui/public-api.ts`

```
shared-ui/
├── public-api.ts
├── docs/README.md
├── foundation/          # Tokens, themes, animations, icons, utilities
├── infrastructure/      # Services, validators, pipes, directives, enums
├── frameworks/          # Theme / permission / validation / business-rule hooks
├── base/                # Form & primitive controls
├── advanced/            # Grid, sidebar, tabs, cards, dialogs
├── layout/              # Page chrome, toolbar, toast, empty-state
└── playground/          # /ui-playground
```

### 3.1 Base controls

| Selector | Purpose |
|----------|---------|
| `ff-textbox` | Text / email / tel; `[digitsOnly]` for phone |
| `ff-number` | Numeric-only (blocks letters); decimal / negative flags |
| `ff-dropdown` | Searchable outlined select |
| `ff-textarea`, `ff-password`, `ff-searchbox` | Text variants |
| `ff-checkbox`, `ff-switch`, `ff-radio` | Boolean / choice |
| `ff-datepicker`, `ff-datetime`, `ff-timepicker` | Dates/times |
| `ff-autocomplete` | Typeahead |
| `ff-button`, `ff-badge`, `ff-chip`, `ff-avatar` | Actions / display |
| `ff-file-upload`, `ff-image-upload` | Uploads |

### 3.2 Advanced & layout

| Selector | Purpose |
|----------|---------|
| `ff-grid` | Data grid + paging/actions |
| `ff-sidebar` | Right drawer forms (Add Driver pattern) |
| `ff-card`, `ff-dashboard-card` | Surfaces / KPIs |
| `ff-tabs`, `ff-stepper`, `ff-expansion-panel` | Structure |
| `ff-confirm-dialog` | Confirmations |
| `ff-page-header`, `ff-page-container`, `ff-breadcrumb` | Page chrome |
| `ff-toolbar`, `ff-filter-panel`, `ff-empty-state` | List tooling |
| `ff-toast`, `ff-loading-overlay`, `ff-status-badge`, `ff-page-footer` | Feedback |

### 3.3 Infrastructure services

- `FfThemeService` — light/dark (`data-ff-theme` + `.dark` + localStorage)
- `FfNotificationService`, `FfLoadingService`, `FfDialogService`
- `FfPermissionService`, `FfBusinessRuleService`, `FfStorageService`

---

## 4. Backend structure (`transport-backend`)

### 4.1 Root

```
transport-backend/
├── pom.xml
├── mvnw / mvnw.cmd
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/transport/erp/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── application-test.yml
│   │       └── db/
│   │           ├── migration/     # Flyway V1–V28
│   │           └── demo/          # Optional demo seed SQL
│   └── test/java/...
```

### 4.2 Java package layout

```
com.transport.erp/
├── TransportBackendApplication.java
├── config/
│   ├── SecurityConfig.java          # CORS, CSRF off, JWT chain, permitAll rules
│   ├── OpenApiConfig.java
│   └── DataInitializer.java         # Bootstrap admin / company
├── security/
│   ├── JwtAuthenticationFilter.java # Skips public auth + OPTIONS
│   ├── JwtUtil.java
│   └── CustomUserDetailsService.java
├── controller/                      # REST endpoints (42 controllers)
├── service/                         # Business logic
├── repository/                      # Spring Data JPA
├── model/                           # Entities (extend BaseEntity)
├── dto/
│   ├── ApiResponse.java             # { success, message, data, errors }
│   └── SetupStatusResponse.java
└── exception/
    └── GlobalExceptionHandler.java
```

### 4.3 Layering convention

```
Controller  →  Service  →  Repository  →  PostgreSQL
     ↓
 ApiResponse<T>
```

Entities share `BaseEntity` (audit / soft-delete / status columns as applicable).

---

## 5. API map (backend controllers)

| Controller | Base path |
|------------|-----------|
| AuthController | `/api/v1/auth` |
| SetupController | `/api/v1/setup` |
| DashboardController | `/api/v1/dashboard` |
| CompanyController | `/api/v1/companies` |
| BranchController | `/api/v1/branches` |
| LookupValueController | `/api/v1/lookups` |
| UserController | `/api/v1/users` |
| RoleController | `/api/v1/roles` |
| PermissionController | `/api/v1/permissions` |
| FinancialYearController | `/api/v1/financial-years` |
| AppSettingController | `/api/v1/settings` |
| VehicleController | `/api/v1/vehicles` |
| VehicleDocumentController | `/api/v1/vehicles/{id}/documents` |
| VehicleServiceLogController | `/api/v1/vehicles/{id}` (service logs) |
| VehicleDriverAssignmentController | `/api/v1/vehicles/{id}/driver` |
| DriverController | `/api/v1/drivers` |
| DriverDocumentController | `/api/v1/drivers/{id}/documents` |
| DriverAttendanceController | `/api/v1/drivers/{id}/attendance` |
| DriverSalaryController | `/api/v1/drivers/{id}/salary` |
| CustomerController | `/api/v1/customers` |
| CustomerContactController | `/api/v1/customers/{id}/contacts` |
| CustomerDeliverySiteController | `/api/v1/customers/{id}/delivery-sites` |
| CustomerDocumentController | `/api/v1/customers/{id}/documents` |
| SupplierController | `/api/v1/suppliers` |
| MaterialController | `/api/v1/materials` |
| MaterialPriceController | `/api/v1/material-prices` |
| QuarryController | `/api/v1/quarries` |
| LoadingLocationController | `/api/v1/loading-locations` |
| BookingController | `/api/v1/bookings` |
| TripController | `/api/v1/trips` |
| FuelEntryController | `/api/v1/fuel` |
| FuelRequestController | `/api/v1/fuel/request` |
| ExpenseController | `/api/v1/expenses` |
| CustomerReceiptController | `/api/v1/receipts` |
| CustomerLedgerController | `/api/v1/customer-ledger` |
| SalesInvoiceController | `/api/v1/invoices` |
| ChartOfAccountController | `/api/v1/accounts` |
| JournalVoucherController | `/api/v1/journal` |
| ReportTemplateController | `/api/v1/reports` |
| ScheduledReportController | `/api/v1/reports/schedule` |
| GpsTrackingController | `/api/v1/gps` |
| AiPredictionController | `/api/v1/ai` |

**Public (no JWT):** `/api/v1/auth/login`, `refresh`, `forgot-password`, `reset-password` (+ selected master paths in `SecurityConfig` for local/dev).

---

## 6. Database (Flyway)

### 6.1 Migrations (`src/main/resources/db/migration`)

| Version | Focus |
|---------|--------|
| V1 | Init schema |
| V2 | Core masters |
| V3 | Auth / logging |
| V4 | Company administration |
| V5 | Vehicle management |
| V6 | Driver management |
| V7 | Customer management |
| V8 | Material / quarry |
| V9 | Booking |
| V10 | Trip / dispatch |
| V11 | Fuel |
| V12 | Expense |
| V13 | Payments / ledger |
| V14 | Sales invoices |
| V15 | Accounts / GL |
| V16 | Reports / dashboards |
| V17 | GPS / AI mobility |
| V18–V28 | Base-entity / column fixes & system lookup settings |

### 6.2 Demo scripts (`db/demo`)

Optional seed helpers: company, branch, roles, users, lookups, masters, fleet, ABC demo users/scenarios, transactions, plus `reset_db.sql` / `reset_sequences.sql` / `seed_demo_data.sql`.

---

## 7. Domain module map (FE ↔ BE)

| Business area | Frontend | Backend packages |
|---------------|----------|------------------|
| Auth / profile | `auth/*`, `auth.service` | `AuthController`, `AuthService`, JWT |
| Setup | `setup-wizard` | `SetupController` / `SetupService` |
| Company admin | `company-administration` | Company, Branch, FY, Settings |
| Users & roles | `user-role-management` | User, Role, Permission |
| Masters hub | `master-management` | vehicles/drivers/customers/… controllers |
| Vehicle ops | `vehicle-details-console` | Vehicle* controllers |
| Driver master | `driver-details-console` | Driver* controllers |
| Customer ops | `customer-details-console` | Customer* controllers |
| Materials | `material-quarry-console` | Material, Quarry, Price |
| Operations | booking / trip / fuel / expense consoles | Booking, Trip, Fuel*, Expense |
| Finance | payment / invoice / accounts | Receipt, Ledger, Invoice, CoA, Journal |
| BI / mobility | report / mobility consoles | Reports, GPS, AI |

---

## 8. Configuration & runbooks

### Frontend

```bash
cd transport-frontend
npm install
npm start                 # ng serve + proxy → :8080
```

- Proxy: `proxy.conf.json`
- Theme FOUC script: `src/index.html` (`ff-theme` + `theme` + `data-ff-theme` + `.dark`)

### Backend

```bash
cd transport-backend
# JAVA_HOME = JDK 21
./mvnw spring-boot:run
```

- Profiles: `dev` (default), `prod`, `test`
- Bootstrap admin password: `app.bootstrap.admin-password` in `application.yml`

### Docker

Both repos include `Dockerfile` (frontend also has `nginx.conf` for SPA hosting).

---

## 9. Cross-cutting concerns

| Concern | Implementation |
|---------|----------------|
| Auth | JWT in `localStorage` (`token`, `refreshToken`, roles, company/branch) |
| API envelope | `ApiResponse<T>` on backend; mirrored types in FE services |
| Theme | `FfThemeService` + app-shell toggle; CSS tokens in `ff-tokens.css` |
| Forms UX | Driver Master pattern: `ff-sidebar` + `ff-textbox` / `ff-dropdown` / `ff-number` / `ff-datepicker` |
| Number input | `ff-number` sanitizes; phones use `ff-textbox [digitsOnly]="true"` |
| Validation | Bean Validation (BE) + Angular reactive forms + FF control validators |
| Soft delete / audit | `BaseEntity` + `AuditService` / `audit_logs` |
| OpenAPI | `/v3/api-docs`, UI `/swagger-ui.html` |

---

## 10. Counts (approximate)

| Area | Count |
|------|-------|
| FE feature consoles | 18 |
| FE HTTP services | 19 |
| FF base components | 19 |
| FF advanced + layout | ~18 |
| BE controllers | 42 |
| BE services | ~40+ |
| BE repositories / models | ~45 each |
| Flyway migrations | 28 |

---

## 11. Related docs

- `transport-frontend/src/app/shared-ui/docs/README.md` — `@ff/ui` inventory  
- `transport-backend/HELP.md` — Spring Boot starter notes  
- Swagger UI — live endpoint reference when backend is running  

---

*Generated for FleetFlow ERP · Frontend Angular 20 · Backend Spring Boot 3.4 / Java 21*
