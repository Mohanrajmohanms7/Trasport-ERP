# Maintenance & inventory flow

Spare part (catalog, reorder level, photo) → Warehouse → Stock (opening balance / receipt) →
Maintenance request (OPEN → UNDER_REVIEW → APPROVED → CONVERTED; can be CANCELLED until converted) →
Work order (OPEN → IN_PROGRESS → COMPLETED / CANCELLED; parts + labour lines) →
Issue / return from a warehouse → Inventory transactions → Accounts.

## Stock rules
- Issue ≤ line quantity counting what is still out (issued − returned); a returned part can be issued again (V68).
- A return cannot exceed what was issued from that warehouse. Stock never goes negative.
- A work order holding issued parts cannot be cancelled until they are returned.
- Warehouse with stock cannot be deleted. Stock at/below reorder level shows "Reorder", zero "Out of stock".

## Valuation & accounting (V67)
Moving average cost per warehouse + part (`warehouse_stock.average_cost`), from priced receipts and opening balances.
| Event | Debit | Credit |
|---|---|---|
| Receipt with rate | 1200 Spare Parts Inventory | 2000 Accounts Payable |
| Opening balance with rate | 1200 Spare Parts Inventory | 3900 Opening Balance Equity |
| Issue / return | (quantity only; issue recorded at average cost, return at the line's issue cost) | |
| Work order completed | 5400 Repair & Maintenance | 1200 Inventory (net parts from stock) |
| | 5400 Repair & Maintenance | 2000 Accounts Payable (actual cost − parts from stock) |
Actual cost cannot be below the parts cost from stock. Stock issued before V67 carries no cost.

## Exports
Excel / PDF on Spare parts, Warehouses, Stock (with avg cost, value, status), Inventory transactions, Maintenance requests, Work orders.

## Receipt payment mode & costing existing stock (V69)
- Receipt **payment mode**: CREDIT (supplier bill → Cr 2000 Accounts Payable; supplier required when a rate is entered),
  CASH (→ Cr 1000 Cash), BANK (→ Cr 1010 Bank). Default: CREDIT when a supplier is chosen, else CASH.
- **Set cost** on the Stock screen for stock that has no cost yet (entered before costing): one time only,
  posts Dr 1200 Inventory / Cr 3900 Opening Balance Equity for quantity × cost. After that, receipts update the average.

## Brand
`public/brand/`: `transaflow-mark.svg` (app icon / favicon), `transaflow-logo-dark.svg`, `transaflow-logo-light.svg`,
`transaflow-banner.svg|png` (1600×900), `transaflow-icon-1024.png`.
