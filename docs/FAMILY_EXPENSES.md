# Family Expenses (optional add-on, V77)

A personal register for the owner's family spending (groceries, school fees, medical …), **completely separate from the
transport business**. Nothing here touches business expenses, trips, vehicles, fuel, drivers, maintenance, driver
settlement, invoices, cash / bank balances, accounts, P&L, the dashboard or any business report.

## Switching it on
- Feature code `family-expenses` is an **opt-in add-on**: OFF for every plan and client unless switched on
  (`FeatureCatalog.OPT_IN`; every other feature is ON unless switched off).
- Platform Admin → **Feature Access** → per client (or per plan) → *Personal → Family Expenses*.
  Inner switches: **Add**, **Edit**, **Delete**, **Reports**, **Export**, **Categories**.
- Switched off: menu hidden, API answers `FEATURE_DISABLED`; data is kept and reappears when switched on again.

## Who can use it
- Only **COMPANY_ADMIN / ADMIN** of the signed-in company (`@PreAuthorize` on the controller; always the user's own
  company). Accountant, branch manager, operator, viewer, driver → 403. **The platform admin cannot read it** either.
- Bills attached to a family expense (attachment type `FAMILY_EXPENSE`) follow the same rule.
- Audit-log lines are generic ("Family expense FX-2627/00003 added") — no amounts or descriptions.

## Data
`family_expenses` (own table, BaseEntity columns): number `FX-2627/00001` (per financial year), date (not future),
category (dropdown type `FAMILY_EXPENSE_CATEGORY`), amount (> 0), payment mode (dropdown `PAYMENT_METHOD`, Credit
excluded), description, family member (optional), reference (optional). Delete = soft delete.

Categories: the first time a company opens the module it gets Food / Groceries, Education, Medical, House Rent,
Electricity, Travel, Shopping, Household, Insurance, Other. They can be renamed, added, made inactive; a category with
expenses cannot be deleted (make it inactive — old entries keep it).

## Screens
**Personal → Family Expenses**: *Expenses* (this-month cards: total, cash, bank/UPI, highest category; month / category /
mode filters; list; Add / Edit with bill attachment; Excel / PDF), *Reports* (Daily, Monthly, Category-wise, Payment mode,
Yearly by financial year, Category × month comparison, All entries — each Excel / PDF), *Categories*.
Not shown on the business dashboard.

## API (`/api/v1/family-expenses`)
`GET ?from&to&category&mode` · `GET /{id}` · `POST` · `PUT /{id}` · `DELETE /{id}` · `GET /options` · `GET /summary?month=YYYY-MM`
· `GET|POST /categories` · `PUT|DELETE /categories/{id}` · `GET /reports/{register|daily|monthly|category|mode|yearly|comparison}`
· `GET /export/{key}?format=xlsx|pdf`.

## Not in v1 (possible later)
Posting to the books as owner's drawings (if family bills are paid from business cash/bank), recurring entries,
category budgets, refunds as negative entries, Excel bulk upload.
