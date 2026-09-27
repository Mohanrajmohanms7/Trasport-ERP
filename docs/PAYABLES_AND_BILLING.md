# Supplier bills & payments, multi-trip invoicing, trip profitability (V70)

## Accounts payable (`/payables`, `/api/v1/payables`)
- **Bills**: manual (DRAFT → APPROVED → posted: Dr expense by category + Dr 1210 GST Input Credit / Cr 2000 Accounts Payable),
  or automatic and already posted: credit stock receipts (PARTS_STOCK) and credit workshop jobs (outside cost of a work order).
- **Due date** = bill date + supplier `credit_days` when not entered.
- **Payments**: Dr 2000 / Cr Cash or Bank; allocated to chosen bills, else oldest due first. Cancel reverses the entry and re-opens bills.
- Bill cancel only while nothing is paid (manual bills; their posting is reversed). Attach supplier invoice copies to bills.
- Work order completion asks how the outside cost was paid: Credit (workshop bill → payable), Cash or Bank.

## Multi-trip invoicing
`POST /api/v1/invoices/from-trips {"tripIds":[...]}` — one draft invoice for several completed, unbilled trips of one customer
(Invoices → Ready for billing → tick trips / "select all of this customer"). Same checks as single-trip invoices; a trip can never be billed twice.

## New reports
Supplier outstanding & ageing, Supplier bill register, Supplier payment register, Trip profitability
(revenue − fuel − trip expenses − driver day-pay share, per trip), Unlinked costs (fuel/expenses not linked to a trip).

## Security fix
Method security (`@EnableMethodSecurity`) was not enabled, so `@PreAuthorize` role rules on payroll, advances, pay slabs,
payables, fuel requests and vehicle assignments were not enforced. They are now.
