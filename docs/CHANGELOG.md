# TransaFlow — Change history

Every merged change, newest first: date, pull request, database migration (if any) and what changed for users.
Add one entry **with every pull request** (see `.github/pull_request_template.md`). Detail lives in the PR and in the
module documents listed in [`README.md`](README.md).

| Next Flyway migration | **V88** |
|---|---|
| Latest migration on `main` | V87 (GST registration status) |

## 2026-10-25
- **New invoice: choosing a customer fills lines from their completed, uninvoiced trips; a trip is invoiced whole and once** (no migration)
  `GET /api/v1/invoices/billable-lines?customerId=&upTo=` (period billing: only trips on or before the invoice date, oldest first, delivery site shown; later trips counted for the next invoice) builds lines exactly like invoice-from-trip (nothing saved); invoiced trips left out; removing a line removes its whole trip; the server refuses an invoice with only part of a trip's materials (the rest could never be billed).
- **Trip form: booking materials still to dispatch auto-filled with their pending quantity** (no migration)
  Booking balance adds `pending` per line (booked − on trips, without tolerance); lines fully on trips are left out (before, the tolerance re-added them); quantity pre-filled; message when nothing is left.
- **Fix: vehicle / user limit of a client on an unlimited plan can be edited** (no migration)
  The number entered is that client's own cap (0 = unlimited), no extras; previously refused with "Plan Is Unlimited" while the onboarding number (e.g. 5) stayed enforced.

## 2026-10-23
- **Platform Admin: refused saves (e.g. a client limit edit) pop up with the reason** (frontend only)
  Errors also show as a notification, not only in the page banner (the client edit form had none); the save result / over-use warning too.

## 2026-10-21
- **Fuel odometer rules + per-invoice Custom PDF** (no migration)
  Fuel: current below previous refused (save and approve); previous below the lorry's last reading (on or before the fuel date) refused unless an admin confirms a meter replacement / reset (`odometerReset`, request only). Fuel screen shows the full reason. Invoice List row action **Custom PDF** for one invoice (same marked read-only custom export).
- **Tests: flow coverage — fuel entry, receipt cancellation reversal, accounts integrity over the whole test run** (tests only)
  Fuel posting / odometer / trip match; receipt cancel makes the invoice unpaid and nets its entries to zero; every journal entry positive, not same-account, not cross-company; every approved invoice has its entry; trial balance balances per company.
- **Documentation: change history (this file), docs index with what-to-update rules, PR checklist; HANDOVER up to date** (docs only)
  `docs/CHANGELOG.md`, `docs/README.md`, `.github/pull_request_template.md`; HANDOVER: next migration V88, GST status, trip driver vs Vehicle Master, custom invoice export.
- **Driver flow: open-trip lorry for driver logins; trip form pre-fills regular driver; fuel / expense fill from trip** (PR #35)
  Driver login may act (e.g. report a breakdown) for the lorry on their open trip; trip form pre-fills the regular driver; fuel / expense fill vehicle + driver from the chosen trip (new entries only).

## 2026-10-20
- **Invoice List: Custom Excel / Custom PDF with adjusted amounts (file only; official amounts shown)** (PR #34)
  Statement of the listed invoices with editable amounts for one file only; nothing saved; official amounts and difference always printed; read-only `POST /api/v1/exports/invoices/custom`.

## 2026-10-18
- **GST registration status at onboarding: GSTIN optional unless Registered (V87)** (PR #33 · **V87**)
  Status REGISTERED / UNREGISTERED / PENDING; GSTIN required (format checked) only when Registered, NULL otherwise; existing clients keep their GSTIN; unrelated updates never erase it.
- **Trips: another driver than the vehicle's regular driver after confirmation (Vehicle Master unchanged)** (PR #32)
  Vehicle Master driver = regular driver, not a restriction; another driver needs confirmation (`confirmDriverOverride`); vehicle / driver must belong to the trip company; Vehicle Master never changed.

## 2026-10-17
- **Fix: editing a client's vehicle / user limits after onboarding is saved** (PR #31)
  Platform Admin limit edit was silently ignored for plan clients; now saved as plan + extras; below-plan refused; over-use warned, nothing removed.

## 2026-10-13
- **Invoice PDF Design A (Classic GST) + bank / UPI / terms and HSN (V86)** (PR #30 · **V86**)
  New invoice PDF layout; company bank / UPI / terms and material HSN (all optional); amount in words (Lakh / Crore); DRAFT / CANCELLED watermark.

## 2026-10-11
- **Client billing basis: Delivered (default) or Ordered per client, delivered quantity always shown (V85)** (PR #29 · **V85**)
  Per client (Platform Admin): bill delivered (default) or ordered quantity; delivered quantity always kept and printed; shared rule `billed_qty()` / `BillingQuantityPolicy`.
- **Rename 'Freight' to 'Transport charge' (labels only)** (PR #28)
- **Booking rate parts: freight, royalty, loading on the booking line (suggested from last booking), flowing to trip and invoice** (PR #27)
  Freight (transport charge), royalty and loading per unit on the booking line, suggested from the customer's last booking; copied to trip and invoice.

## 2026-10-10
- **App confirmation dialog instead of browser confirm / alert / prompt** (PR #26)

## 2026-10-08
- **Business rules: one trip at a time, trip-matched costs, inactive records, duplicate GSTIN / supplier bill, payroll-locked trip dates** (PR #25)
  One dispatched trip per lorry / driver; fuel / expenses must match the trip; inactive records refused on new entries; unique customer GSTIN and supplier bill number; payroll-locked trip dates.
- **Security: password-reset hole closed, lockout, real force logout, suspended clients blocked (V84)** (PR #24 · **V84**)
  Password-reset hole closed, 5-try lockout, real force logout (token version), suspended clients blocked.

## 2026-10-07
- **+ Add New on Dropdown-List dropdowns (admins), driver/customer document type fixes** (PR #23)

## 2026-10-06
- **Render: database settings from the dashboard (move to Neon)** (PR #20)
- **Responsive UI: pinned row actions, touch targets, drawers, tabs, dashboard, filters + CI responsive audit** (PR #19)
- **Dashboard by branch (branch users: own branch; admins: all or pick one) + fuel branch checks** (PR #18)
- **Flow review 2: booking edit/close branch check; work orders + maintenance lists company-wide for admins** (PR #17)

## 2026-10-05
- **Flow review: branch isolation fixes (bookings, trips, invoices, expenses, fuel, receipts) + role/branch test matrix** (PR #16)
- **Speed: batch fetching, gzip, lazy screens, indexes, keep-alive; fix company-admin billing across branches** (PR #15 · **V83**)
- **Plan pricing: monthly / yearly price, setup fee, extra truck / login / branch prices, client extras, billing estimate (merged without PR)** (no PR · **V82**)

## 2026-10-03
- **Help & Support phase 3: notification bell + critical banner (V80)** (PR #14 · **V80**)
- **Help & Support phase 2: ticket files in the database, internal files, auto-close, 10 MB uploads (V79)** (PR #13 · **V79**)
- **Help & Support phase 1: client issue reporting + platform admin ticket management** (PR #12 · **V78**)
- **Quick Create phase 3: material, delivery site, quarry, expense category + work order / vehicle service / opening balance** (PR #11)
- **Subscription plans: Trial / Starter / Growth / Professional / Enterprise with module sets and limits (merged without PR)** (no PR · **V81**)

## 2026-10-02
- **Dropdowns phase 2: Quick Create (+ Create new …) for customer, vehicle, driver, supplier, spare part** (PR #10)
- **Report filters: server-side search (no 500 limit)** (PR #9)
- **Dropdowns phase 1b: searchable work orders, maintenance, inventory, payables** (PR #7)
- **Dropdowns phase 1: server search, saved value shown, inactive hidden; receipt booking link fixed** (PR #6)
- **Trip form auto-fills from the selected booking (balance API, safe blank quantity)** (PR #5)
- **Family Expenses add-on (opt-in per client, separate from business)** (PR #4 · **V77**)

## 2026-10-01
- **Order units of measure (Unit default, per-client UOM settings, no conversion)** (PR #3 · **V76**)
- **Client feature access: module / tab / action catalog, plan defaults + per-client overrides (merged without PR)** (no PR · **V75**)

## Before 2026-10-01
- Base system (V1 – V74): masters, bookings, trips, invoices with GST, receipts, fuel, expenses, payroll, maintenance,
  stores, payables, accounts (double entry), reports, multi-company / branch SaaS with Platform Admin. See `ARCHITECTURE.md`
  and `BUSINESS_FLOW.md`.
