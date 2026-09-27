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
- COMPANY_ADMIN / ADMIN are **company-wide**: every branch of their own company (`TenantAccessService.isCompanyWideAdmin`);
  they may record for any branch of their company. Other roles stay limited to their branch. Company isolation unchanged.
- `REQUIRE_SEPARATE_APPROVER` only applies when the company has 2+ active staff logins (non-driver, non-viewer).
- Nobody can delete or deactivate their own login, and the last active COMPANY_ADMIN of a company cannot be deleted,
  deactivated or demoted.
- Accounting duplicate/reversal checks look up postings per company (document numbers repeat across companies).
