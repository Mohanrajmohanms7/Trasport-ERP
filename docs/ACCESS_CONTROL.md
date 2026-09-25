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
