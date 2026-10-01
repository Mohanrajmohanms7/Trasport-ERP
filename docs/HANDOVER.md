# TransaFlow — Developer Handover (read this first)

TransaFlow is a multi-tenant SaaS **transport ERP for quarry-to-site haulage** (M-Sand, blue metal, gravel…): bookings,
trips with weighbridge weights, fuel, expenses, GST invoicing, receipts, driver daily-slab payroll, maintenance, spare-parts
stock, supplier payables, accounts and 40 reports. First live client: **PKC Transport, Perambalur** (single Company Admin login).

This file is the up-to-date summary (updated with client feature access, migration **V75**). Older baseline docs from before
2026-09-22 (`AI_PROJECT_CONTEXT.md`, `PROJECT_CONTEXT.md`, `ARCHITECTURE.md`, `DEVELOPMENT_RULES.md`, the lower-case module
notes) are still useful background, but **where they disagree with this file or the newer docs listed below, this file wins.**

---

## 1. Stack & layout

| Part | Tech | Path |
|---|---|---|
| Backend | Spring Boot 3.4 / Java 17, Spring Security (JWT), JPA/Hibernate, Flyway, Apache POI, OpenPDF | `transport-backend/` (`com.transport.erp`: `controller`, `service`, `repository`, `model`, `dto`, `security`, `config`, `util`, `exception`) |
| Database | PostgreSQL 16, Flyway migrations `V1…V74` | `transport-backend/src/main/resources/db/migration` |
| Frontend | Angular 20 standalone components + signals, Tailwind, Angular Material, IBM Plex Sans | `transport-frontend/src/app` (`components/`, `shared/`, `shared-ui/`, `services/`, `layout/`, `guards/`, `interceptors/`) |
| Help / brand | Static user guide pages, logo | `transport-frontend/public/help`, `public/brand` |
| Tools | Demo data loader, report catalog generator/tester | `tools/demo`, `tools/reports` |
| Checks | CI workflow + end-to-end smoke | `.github/workflows/verify.yml`, `.ci/` (see `.ci/README.md`) |
| Deploy | Render (`render.yaml`: backend, frontend, Postgres) — auto-deploys on push to `main` | |

Deployment note: a **Vercel** project is also connected and has failed on every deploy since 2026-09-27; it is not used by
Render. Fix or disconnect it.

## 2. How to work on this repo

1. Branch from `main` (`feature/…`, `fix/…`).
2. Follow the existing patterns below; reuse services (never write parallel logic).
3. New DB change = new Flyway file `V75__…sql` (never edit an applied migration). Keep entities in sync (`ddl-auto=validate`).
4. Add checks for the change to `.ci/smoke.sh`; open a PR → **verify** must be green (Gate step).
5. Update the user guide when screens/rules change: `docs/user-guide/USER_GUIDE_EN.md` + `USER_GUIDE_TA.md`, then regenerate
   `transport-frontend/public/help/user-guide-*.html` (Python `markdown`, see git history of those files).
6. Merge to `main` → Render deploys. Check the Render dashboard (Render does not report back to GitHub).
7. Never commit secrets or tokens. Use a fine-grained GitHub token limited to this repo.

## 3. Core patterns (use them)

**Tenant isolation (critical).** Every business table has `company_id` (+ `branch_id`). Always go through
`security/TenantAccessService`:
- `resolveCompanyId(requested)` for reads/creates; `assertOwned(companyId)` / `assertCompanyAccess` on every loaded record
  (get, update, delete, approve, cancel…).
- Branch scope: `assertBranchAccess`, `resolveBranchId`. **COMPANY_ADMIN / ADMIN are company-wide**
  (`isCompanyWideAdmin`) — they see and work in every branch of their own company; other roles are limited to their branch.
- Repository search methods must keep the company filter inside parentheses (derived `…AndNameContainingOrCodeContaining`
  queries leaked other tenants' data — they are now `@Query` with parentheses).
- Accounting lookups by reference are per company (`findByCompanyIdAndReferenceNumberAndIsDeletedFalse`), because document
  numbers repeat across companies.

**Security.** `config/SecurityConfig` (URL rules: admin-only writes for users/roles/companies/branches/settings; accounts-only
writes for journals/accounts; VIEWER read-only), `@EnableMethodSecurity` is on (method `@PreAuthorize` rules are enforced),
`config/DriverScopeAuthorizationManager` (DRIVER logins: maintenance requests, own salary slips, files/attachments only),
`security/JwtAuthenticationFilter` (subscription expiry block; temporary-password block `PASSWORD_CHANGE_REQUIRED`).
Access denied must surface as HTTP 403 (controllers re-throw `AccessDeniedException`; `GlobalExceptionHandler` maps it).

**Client feature access.** `features/FeatureCatalog` (modules / tabs / actions → routes + API rules),
`FeatureAccessService` (plan + client overrides), `FeatureAccessFilter` (API 403 `FEATURE_DISABLED`), frontend `FeatureService`,
`featureGuard`, `*ffFeature`. New module or tab = one catalog line + hide it in the screen. See `docs/ACCESS_CONTROL.md`.

**Errors.** Business-rule failures throw `exception/BusinessValidationException(title, CODE, message, userAction)` → shown to
users verbatim. Write messages a transport clerk understands ("Customer owes ₹… — collect payment or raise the limit").

**Numbering.** `DocumentNumberService` — sequential per company per financial year, e.g. `INV-2627/00001`.

**Dates.** Transactions may be back-dated but never future-dated; accounting postings validated by
`FinancialYearPeriodValidationService` (open FY, not future). Postings use the document date.

**Accounting (double entry via `JournalVoucherService`, idempotent by reference).** Key accounts: 1000 Cash, 1010 Bank,
1100 Customer Receivables, 1150 Driver Advances, 1200 Spare Parts Inventory, 1210 GST Input, 2000 Accounts Payable,
2050 Driver Salary Payable, 2200 GST Liability, 3900 Opening Balance Equity, 4000 Freight Income, 4900 Driver Recoveries,
5150 Driver Salary, 5400 Repair & Maintenance. Every cancel reverses its postings.

**Approvals.** Documents are Draft/Submitted → Approve/Post (posts to accounts, then locked). Optional maker-checker
`REQUIRE_SEPARATE_APPROVER` (company setting) — ignored when the company has only one active staff login.

**Frontend.** Standalone components with signals; HTTP via `HttpClient` (JWT added by `interceptors/jwt.interceptor.ts`);
shared building blocks: `shared/export-buttons` (Excel/PDF), `shared/attachments-panel`, `shared/entity-photo`,
`shared/master-forms/master-form-dialog` (vehicle/customer create/edit), `shared/bulk-upload/bulk-upload-dialog`.
Menu in `layout/app-shell/app-shell.ts` (see `docs/MENU_STRUCTURE.md`). Show server messages
(`err.error.errors[0] || err.error.message`).

## 4. Business flow & rules decided so far

Masters (branches with GSTIN, materials/quarries, customers + delivery sites, vehicles, drivers, suppliers with credit days)
→ **Booking** (approve; credit limit enforced: dues + booking value ≤ limit, 0 = no limit; can be back-dated; auto-COMPLETED
when fully delivered; Close early) → **Trip** (approved booking only; PLANNED → DISPATCHED (vehicle+driver) → COMPLETED;
quantity ≤ booked + tolerance %; weighbridge loaded/delivered, bill on delivered; vehicle with open work order blocked;
locked once invoiced) → **Fuel / Expenses** (approve; link to trip for trip profit) → **Invoice** (from one or many completed
trips of one customer; GST CGST+SGST vs IGST by branch/customer GSTIN state; discount before GST; approve posts) →
**Customer receipt** (allocate to invoices, rest = advance; approve; **Apply advance** later; receipt voucher PDF/print) →
**Driver settlement** (advances; monthly payroll = one slab amount per working day + basic + allowance − deductions −
advance recovery; approve → post → pay; drivers see own slips) → **Reports**.
Maintenance: request → review → approve → work order → parts issued from stock (moving average cost) → complete (parts cost
from inventory; outside cost as Credit supplier bill / Cash / Bank). Stock receipts with payment mode; credit receipts
create supplier bills automatically. Supplier bills/payments with allocation.

Details per area: `docs/ACCESS_CONTROL.md`, `MENU_STRUCTURE.md`, `MAINTENANCE_INVENTORY.md`, `PAYABLES_AND_BILLING.md`,
`REPORTS.md`, `EXPORTS_ATTACHMENTS_PHOTOS.md`, `sales_billing_gst.md`, `trip_planning_dispatch.md`, `driver_management.md`,
and the end-user view in `docs/user-guide/`.

Platform (SUPER_ADMIN): Platform Admin sections (`/platform-admin/<section>`), onboarding creates company + head office +
FY + roles + one COMPANY_ADMIN (+ `SETUP_COMPLETED`), company short name/initials shown in the header, subscription
notice 3 days before the end date and block after it (admin may still log in to renew), temporary passwords must be changed
at first login (admin-chosen onboarding passwords are not forced).

## 5. Features added in this phase (quick index)

| Area | Where |
|---|---|
| Reports hub (40 reports, filters, totals, Excel/PDF) | `service/ReportHubService`, `resources/reports/report-catalog.json` (generated by `tools/reports/generate_catalog.py`, SQL tested with `test_catalog_sql.py`), `components/reports-hub` |
| Exports for list screens | `controller/ExportController`, `service/XlsxExportService`, `util/TablePdfGenerator` |
| Attachments, photos, DB file storage | `service/AttachmentService`, `PhotoService`, `FileStorageService` (files in table `stored_files`) |
| Excel bulk upload (customers, vehicles, drivers, suppliers, materials, spare parts, opening stock) | `service/BulkImportService`, `controller/BulkImportController`, `shared/bulk-upload` |
| Stock valuation & inventory accounting | `service/InventoryValuationService` |
| Payables | `service/PayablesService`, `components/payables-console` |
| Settlement dashboard, apply advance | `CustomerReceiptService.getSettlementDashboard / applyAdvance / applyCustomerAdvances`, `components/payment-details-console` |
| Branch Master, Supplier Master, Dropdown Lists | `components/branch-master`, `supplier-master`, `master-management` (dropdowns) |
| Subscription notice, tenant brand | `AuthService.subscriptionInfo / tenantBrand`, `layout/app-shell` |
| PKC demo data | `tools/demo/seed_pkc_demo.py` (API-based; run once per environment) |

## 6. Open items / next steps

1. **Delete the old GitHub token** used during development (full-account scope) and create a repo-scoped one.
2. Confirm Render deployed the latest `main`; fix or disconnect Vercel.
3. Load PKC demo data in Dev: `python3 tools/demo/seed_pkc_demo.py --url https://<backend>/api/v1 --username <pkc admin> --password <pwd>`.
4. Review customer credit limits (now enforced at booking approval).
5. Native Tamil review of `docs/user-guide/USER_GUIDE_TA.md`.
6. Ideas not built: supplier payment due reminders; "force password change" action per user in Platform Admin UI (API exists);
   trip-wise fuel auto-linking; e-way bill / e-invoice (IRN) integration.

## 7. Starting prompt for a new AI session

> This is TransaFlow, a transport ERP (Angular 20 + Spring Boot 3 + PostgreSQL) in Mohanrajmohanms7/Trasport-ERP.
> Read docs/HANDOVER.md first, then the docs it links. Follow the existing patterns (TenantAccessService, BusinessValidationException,
> DocumentNumberService, JournalVoucherService, shared UI components). Every change: new Flyway migration if needed, checks added
> to .ci/smoke.sh, PR to main with a green "verify" run, user guide (EN + TA) updated when behaviour changes.
