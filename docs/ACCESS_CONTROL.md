# Access control (API)

Enforced in `SecurityConfig` + `DriverScopeAuthorizationManager` (server side; the Angular menu is not a security boundary).

| Who | Can |
|---|---|
| Not signed in | login, refresh, forgot/reset password, public plans |
| `SUPER_ADMIN` | everything, including `/api/v1/platform-admin/**` |
| `COMPANY_ADMIN`, `ADMIN` | everything in their company; the only roles that may create/change/delete users, roles, permissions, company, branches, settings, financial years, setup |
| `ACCOUNTANT` (and admins) | manual journals `/api/v1/journal/**` and chart of accounts `/api/v1/accounts/**` writes |
| Any other staff role (`BRANCH_MANAGER`, `OPERATOR`, custom) | all other operational APIs |
| Only `VIEWER` | read-only (GET), plus own auth endpoints |
| Only `DRIVER` (driver app) | `/auth/**`, `/maintenance-requests/**`, `/driver-payrolls/my/**`, `/files/**`; read-only `/vehicles`, `/drivers`, `/lookups`, `/uoms` |

Role assignment rules (`UserService`, `RoleService`):
- Roles are re-read from the database; a role must belong to the user's company or be global.
- Only a platform super admin can grant `SUPER_ADMIN`; tenants cannot create a role with that code.
- Users cannot change their own roles. Tenants only see their own and global roles (never `SUPER_ADMIN`).

Accounting dates: every auto-posted JV uses the document date (invoice, receipt, expense, fuel, service, payroll);
no posting may be dated after today (`FinancialYearPeriodValidationService`).

## Second-person approval (maker-checker)

Company setting `REQUIRE_SEPARATE_APPROVER` = `true` → the user who created an invoice, receipt, expense, fuel entry
or driver payroll cannot approve it (`ApprovalPolicyService`). Default `false` (single-person offices).

## Menu and landing by role (UI convenience; the API enforces the real rules)

- Driver-only login lands on Maintenance Requests and sees only Maintenance Requests and My Salary.
- User & Roles and System Settings are shown only to COMPANY_ADMIN / ADMIN.

## Platform Admin (SUPER_ADMIN)

Sections are routed (`/platform-admin/{section}`) and listed in the main sidebar:
Overview (Dashboard) · Tenants (Clients & Companies, Subscriptions & Plans, Licenses, Billing Invoices) ·
Access & Security (Users & Sessions, Audit Logs) · Support (Tickets, Announcements) · System (Settings, Backup & Data Export).

- **Company short name** (`companies.short_name`, 2–6 letters/digits/&): entered on onboarding / edit, upper-cased; blank = derived
  from the name ("PKC Transport" → PKC). Returned at login (`companyShortName`) and by `GET /api/v1/auth/tenant-brand`;
  shown in amber above "TransaFlow" in the sidebar (and as a pill in the phone header). Platform operators see "PLATFORM".
- **Backups**: the app records checkpoints only; real database backups come from the PostgreSQL host.
  `GET /api/v1/platform-admin/companies/{id}/data-export` downloads a ZIP of 18 Excel lists for one tenant.

## Subscription notices & enforcement
- From 3 days before `companies.subscription_end_date` every user of that company sees an amber notice under the header
  ("ends in N days / tomorrow / today"). Company admins get **Renew now** (early renewal extends from the current end date);
  other users are told to ask their admin. Closing it hides it until the next day.
- Platform operators see "N client subscriptions end soon" with a link to Clients & Companies.
- After the end date: only the company admin can sign in (to renew); every other API call returns SUBSCRIPTION_EXPIRED.
  Renewal endpoints (plans, renew, tenant-brand, profile, logout) stay open. (Fixed: this check was silently skipped before.)
- Source: `AuthService.subscriptionInfo` / `expiringClients`, `GET /api/v1/auth/tenant-brand`.

## First login after onboarding
Platform Admin onboarding provisions company, head office, financial year, roles, admin user and masters and sets
`SETUP_COMPLETED=true` for the new company (V71 does the same for existing provisioned companies). The client admin's
first login therefore opens `/dashboard`. The Setup Wizard (`/setup`) is only offered to a company admin of a company
that was never provisioned (no flag and no business data); other roles are never redirected there.

## One-person companies (single Company Admin)
- Onboarding creates exactly one login with COMPANY_ADMIN; that is enough to run the whole application.
- COMPANY_ADMIN / ADMIN are **company-wide**: every branch of their own company (`TenantAccessService.isCompanyWideAdmin`). Lists and record
  pages use one rule, `listBranchScope()` / `assertBranchVisible()`: company admins see every branch, other roles only their
  branch (bookings, trips, invoices, receipts, expenses, fuel, work orders, maintenance requests, ready-for-billing, reports,
  **dashboard**: company admins see the whole company or pick one branch (`?branchId=`); branch users always get their own
  branch's figures). Checked by the flow-review smoke blocks;
  they may record for any branch of their company. Other roles stay limited to their branch. Company isolation unchanged.
- `REQUIRE_SEPARATE_APPROVER` only applies when the company has 2+ active staff logins (non-driver, non-viewer).
- Nobody can delete or deactivate their own login, and the last active COMPANY_ADMIN of a company cannot be deleted,
  deactivated or demoted.
- Accounting duplicate/reversal checks look up postings per company (document numbers repeat across companies).

## Subscription / client feature access (V75)
Platform Admin → **Feature Access** decides, per **plan** and per **client**, which modules, inner tabs and actions are included.
- Catalog: `features/FeatureCatalog.java` — every module (menu), tab and action with its screen route(s) and API rules.
  To add a module/tab later: add one line there; hide the tab in its screen with `features.has('<code>')` (or `*ffFeature`).
- Effective access = client override (`company_features`) → else plan setting (`plan_features`) → else allowed.
  A feature works only if it and all its parents are on. Core (always on): Dashboard, Users & Roles, System Settings.
- Enforcement: `features/FeatureAccessFilter` (in the security chain) answers 403 `FEATURE_DISABLED` for API calls of switched-off
  features (most specific rule wins); report categories are checked in `ReportHubService`. Platform admins are never limited.
  Master data (customers, vehicles, drivers, materials, suppliers, parts, warehouses, branches, dropdowns): when switched off,
  their read APIs stay open because other screens need them for dropdowns; the screen is hidden and changes are blocked.
- Screens: `FeatureService` (`/api/v1/auth/features`) hides menu items and tabs; `featureGuard` blocks direct URLs;
  shared components (export buttons, attachments, photos, bulk upload) follow their features.
- Roles still apply on top (feature access decides what the client bought; roles decide what each user may do).

## Subscription plans & limits (V81)
Plans (codes kept from before): TRIAL (Growth features), BASIC = **Starter**, STANDARD = **Growth**, PROFESSIONAL = **Professional**,
PREMIUM = **Enterprise**. Their excluded modules are stored in `plan_features` (seeded by V81; edit in Feature Access → Per plan).
- Limits per plan: active trucks, staff logins (driver logins not counted), active branches; 0 = unlimited. Copied to the client
  when a plan is applied (Platform Admin → **Client Plans** → Preview → Apply) and enforced by `PlanLimitService`
  (create / re-activate vehicle, user, branch). Inactive records don't count.
- `companies.plan_enforced`: existing clients stay on full access and no limits until a plan is applied; newly onboarded clients get
  their plan immediately. Every change is recorded in `plan_change_history` (APPLIED / UPGRADE / DOWNGRADE).
- Dependencies (`FeatureCatalog.REQUIRES`): e.g. Payables needs Supplier Master, Stock needs Spare Parts + Warehouses — switching one
  off switches the dependent off.
- Client overrides carry a **reason** and optional **valid until** date (add-ons, trials); expired overrides stop applying.
- Downgrade preview warns about trucks/users/branches over the new limit and open work (open work orders keep trucks blocked).
- Not built yet: online subscription payment; add-on price catalogue; downgrade grace mode.

## Plan pricing (V82)
Each plan: monthly price (`price`), yearly price (default 10 × monthly), one-time setup fee, price per extra truck / login / branch
per month, included trucks / logins / branches (0 = unlimited). Edit in Platform Admin → Subscriptions & Plans; changing included
limits updates every client on that plan. Per client (Client Plans → Manage): extra trucks / logins / branches and billing cycle
(monthly / yearly); limits = plan + extras; the screen shows what the client pays per cycle. Payment collection is manual for now.
Seeded starting prices (INR/month): Starter 1,499 · Growth 3,499 · Professional 7,999 · Enterprise from 14,999 · Trial free.


## Login & session protection (V83)
- 5 wrong passwords in a row lock the login for 15 minutes (`LoginAttemptService`); an admin password reset unlocks it.
- Sessions carry a version (`app_users.token_version`, JWT claim `tv`). Force logout, admin password reset, deactivation or
  role loss bump it, so existing logins stop at once; refresh tokens are removed too.
- Suspended / deleted clients: login refused and every request answers 403 `COMPANY_SUSPENDED` (platform admins excluded).
- `/auth/reset-password` refuses resets without an emailed token (none exist yet) — admins reset passwords instead.
  `/auth/forgot-password` never reveals whether an email is registered.
- Platform Admin cannot deactivate or demote themselves, the last active platform admin is protected, and the company that
  holds platform admin logins cannot be suspended or deleted.
