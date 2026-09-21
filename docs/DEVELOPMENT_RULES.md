# DEVELOPMENT_RULES.md

Rules for humans and AI coding agents working on this Transport ERP.

Canonical context: `PROJECT_CONTEXT.md`, `BUSINESS_FLOW.md`, `ARCHITECTURE.md`, `DATABASE_DESIGN.md`. If those files disagree with new code, **update the docs in the same change** after the code is correct — do not “fix” docs to invent features.

---

## General

1. **Read existing code first.** Search controllers, services, entities, and Angular consoles before adding files.
2. **Reuse existing patterns.** Controller → Service → Repository; Angular standalone console + `*-mgmt.service.ts`; `ApiResponse<T>`; `BaseEntity`.
3. **Keep diffs focused.** One feature or one bug. Do not mix UI restyles with payroll/schema work.
4. **Do not modify unrelated modules.** If a booking change does not require trip edits, leave trips alone.
5. **Do not rewrite working code** for taste (renames, framework swaps, MapStruct introduction, NgRx).
6. **Distinguish Exists / Planned / Recommended** in comments and docs. Never implement the planned driver-settlement status machine by silently renaming payroll statuses.
7. **Tenant isolation is non-negotiable.** Use `TenantAccessService.resolveCompanyId`. Never trust client `companyId` for company users.
8. **String statuses:** grep the whole repo before adding or renaming values (`PLANNED`, `IN_TRANSIT`, `POD`, etc.).
9. **If unknown:** write `Not currently identified in the codebase` rather than guessing.
10. **Do not change production data.** Do not run destructive SQL against `transport_erp` unless the user explicitly asks and the environment is clearly local/dev.

---

## Backend

1. Follow existing package layout under `com.transport.erp`.
2. Keep controllers thin: authz, tenant resolve, call service, wrap `ApiResponse`.
3. Put status machines and posting in **services** (see `DriverPayrollService`).
4. Use **DTOs** for write operations that are not simple entity CRUD. Do not add MapStruct unless asked (`pom.xml` already has it unused).
5. Return `ApiResponse.success(data, message)` / `ApiResponse.error(errors, message)`.
6. **`@Transactional`** on approve/pay/cancel, receipt allocation, invoice paid-amount updates, and any multi-table financial write.
7. **No queries inside loops.** Prefetch collections; use batch `findAllById`.
8. Avoid duplicate DB/API work in one request (N+1, repeated `resolveCompanyId` side effects are fine; repeated full-table loads are not).
9. Validate with Bean Validation **and** service-level `BusinessValidationException` for business rules.
10. Let `GlobalExceptionHandler` format errors. Do not invent a second error JSON shape.
11. Use `@PreAuthorize` consistently with neighboring endpoints. Roles seen in code include `SUPER_ADMIN`, `COMPANY_ADMIN`, `ADMIN`, `BRANCH_MANAGER`, `ACCOUNTANT`, `FLEET_MANAGER`.
12. After `findById`, call `tenantAccess.assertOwned(entity.getCompanyId())` (or equivalent).
13. Soft-delete: set `is_deleted`; do not hard-delete financial history.
14. Document numbers: use the existing numbering service/tables (V37). Do not concatenate random strings.
15. Hibernate `ddl-auto` is **validate**. Schema = Flyway only.
16. Logging: use existing audit helpers for financial actions (`DRIVER_PAYROLL_APPROVED`, etc.).
17. GPS/AI controllers are stubs. Do not expand them as if they were production tracking unless that is the task.
18. Fitness KPI vs PM due: **do not reuse** `maintenanceDueCount` for preventive maintenance. Use `maintenanceDueDashboard`.

---

## Frontend

1. Standalone components. Match neighboring consoles (page header, grid, filters, drawer/form).
2. Reuse `shared-ui` `ff-*` controls and existing `shared/` widgets.
3. Follow current Tailwind + Material + `ff-tokens.css` theming. Do not add another CSS framework.
4. HTTP only through existing service classes; attach JWT via `jwtInterceptor` (do not duplicate Bearer logic).
5. **Avoid duplicate API calls** on init (forkJoin / shareReplay / single load method).
6. Handle loading and error states the way the current console does.
7. Validate forms before submit; still handle backend `errors[]`.
8. Do not introduce NgRx, new HTTP clients, or extra chart libraries without an explicit request.
9. SUPER_ADMIN sees Platform Admin, not company menus — keep that split.
10. `setupGuard` and `subscriptionGuard` must remain on the routes that already use them.
11. Environment URLs: dev uses `http://localhost:8080/api/v1`; do not hard-code other hosts.
12. After UI changes, verify the affected console in the browser (or say what could not be verified).

---

## Database

1. **Next migration: V54+** under `transport-backend/src/main/resources/db/migration/`.
2. Never edit old Flyway files on shared databases. Dev `repair-on-migrate` is not a license to rewrite history.
3. Include full `BaseEntity` columns on new tables (`code`, `name`, `status`, tenant, audit, `is_deleted`, `version`).
4. Use PostgreSQL types consistent with neighbors: `NUMERIC(12,2)` money, `TEXT` long strings, `VARCHAR` statuses.
5. Add **FKs and unique constraints** for financial identity (`payroll_number`, period uniqueness).
6. Add **indexes** for tenant + status + FK lookup patterns (see V46, V41).
7. No JSONB unless there is a proven need (none exists today).
8. Prefer string statuses over PostgreSQL ENUM types (matches the rest of the schema).
9. Consider query performance: company_id first in composite indexes.
10. Do not create the “proposed” tables in `DATABASE_DESIGN.md` unless the user asked to implement that module.
11. Never modify production data directly.
12. `ddl-auto: validate` must still pass after your migration.

---

## Financial modules

Applies to **driver salary, payroll/settlement, expenses, invoices, receipts, customer ledger, journal vouchers**.

1. **Auditability:** who/when (`created_by`, `approved_by`, audit log). Do not update amounts in place after posting.
2. **Preserve history.** Cancel = reversing JV + status change, not DELETE of posted rows.
3. **No silent financial edits.** DRAFT is editable; APPROVED/PAID are not (payroll pattern).
4. **Separate calculation from posting.** Compute net pay in DRAFT; post GL on APPROVE/PAY.
5. **Advances and recoveries** must remain visible (`advance_taken` vs `advance_adjustment`). If you add real advance vouchers, they must have their own rows and ledger lines.
6. **Driver ledger:** do not fake one by overwriting `driver_salaries.advance_taken` without a transaction table.
7. **Customer ledger** already exists — extend it carefully; keep running_balance consistent with debit/credit.
8. **Journal vouchers** are one debit account + one credit account. Do not insert unbalanced rows. Do not add voucher lines without a planned migration.
9. Payroll unique period `(driver_id, pay_year, pay_month)` must remain true.
10. Expense `DRIVER_BATA` is an **expense**, not payroll, unless you explicitly integrate them.
11. Invoice payment status must stay consistent with allocations and `paid_amount`.
12. If implementing the **planned** settlement flow (`CALCULATED` → `SUBMITTED` → `POSTED`), create **new** entities as in `DATABASE_DESIGN.md` Proposed section — do not overload `driver_payrolls.status`.

---

## Testing

Before calling a feature done:

1. Backend: `.\mvnw.cmd test` (or the compile/test the change requires). Maven may not be on PATH; use the wrapper.
2. Frontend: `ng build` / `ng test` when UI changed.
3. Hit the affected APIs (tenant, branch, SUPER_ADMIN empty company, negative paths).
4. Exercise the UI flow (create → validate → status change), not only a screenshot.
5. Confirm Flyway migrated and `ddl-auto: validate` still starts.
6. Regression-check neighboring modules (e.g. payroll must not break journal listing).
7. **Never claim a test or build passed unless it actually passed** in this session.

If browser tools are unavailable, say so and use the closest substitute.

---

## Git

- **`main` is stable.** Do not develop new features on `main`.
- Work on `feature/<feature-name>` (current example: `feature/transport-erp-development`).
- Meaningful, focused commits (why, not file laundry lists).
- Do not force-push unless the user explicitly requires it.
- Do not commit `.env`, passwords, or `application-dev.yml` secrets into new files. Dev yml already contains local credentials — do not copy them into docs or tickets.
- Do not commit or push unless the user asks.
- Do not `git reset` or mix unrelated staged files (e.g. UI restyle + feature) into one commit.

Recommended flow:

```text
main
 ↓
feature/<feature-name>
 ↓
Development
 ↓
Testing
 ↓
Pull Request
 ↓
Review
 ↓
main
```

---

## AI agent checklist (every non-trivial change)

- [ ] Searched for an existing implementation
- [ ] Tenant-scoped reads/writes
- [ ] Financial posting transactional and reversible
- [ ] Flyway only for schema
- [ ] Angular reuses consoles/shared-ui
- [ ] Docs updated if behavior/status/tables changed
- [ ] Builds/tests actually run
- [ ] Unrelated dirty files left untouched
