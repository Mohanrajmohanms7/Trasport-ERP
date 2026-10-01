# Units of measure on orders (V76)

Quantities on **bookings, trips and invoices** are recorded in a unit of measure (UOM): Unit, Ton, KG, CFT …
PKC Transport orders by the **Unit** only ("2 Unit M-Sand"); other clients can switch more units on later without any
schema or flow change. **Quantities are never converted between units.**

## Design

| Level | Where | Decides |
|---|---|---|
| Global catalogue | `uom_master` (rows with `company_id` NULL; companies may add their own rows) | Which units exist (code, name, symbol, category). Standard rows can only be changed by the platform admin. |
| Client / tenant | `company_uoms` (`status` ACTIVE/INACTIVE, `is_default`, one default per company — unique index) | Which units this client may use on orders, and the default. No row = not enabled. A company with no rows at all behaves as "Unit only". |
| Material | `materials.default_uom_id` | Pre-selects the unit on a new line, if that unit is switched on for the client. |
| Transaction line | `booking_details.uom_id`, `trip_details.uom_id`, `sales_invoice_details.uom_id` | The unit the line was entered in. Rates on the line (material, freight, loading, royalty) are **per that unit**. |

Why this combination: units are a shared reference list (global), what a client sells in is a business choice
(tenant), a material usually has a natural unit (material default), and history must never change when settings
change (stored on the line).

## Rules (service: `OrderUomService`)

- **Line unit** = requested unit (must be switched on for the client, or be the unit the line already had)
  → else the line's previous unit → else the material's default unit (if switched on) → else the client default.
  API clients that send no unit keep working (they get the default).
- **Booking:** one unit per material on a booking (`BOOKING_MIXED_UOM`), so trip matching by material stays exact.
  Once trips have moved a material, its unit is locked (`BOOKING_UOM_LOCKED`).
- **Trip:** each line always takes its booking line's unit (anything sent by the client is ignored). Planned, loaded and
  delivered quantities, the booked-quantity check (with `BOOKING_QTY_TOLERANCE_PERCENT`) and shortage are all in that
  unit. Messages say e.g. "Only 2.00 Unit left on booking BKG-2627/00012".
- **Invoice:** a trip line is billed in the trip line's unit; a manual line follows the line-unit rule above.
  Taxable = quantity × (rate + freight + loading + royalty), all per unit — unchanged formula. Print and PDF show "2 Unit".
- **Settings:** the default unit cannot be switched off; at least one unit stays on; units inactive in the master cannot
  be switched on. Switching a unit off only affects **new** lines — saved lines keep their unit and can still be edited.
- **Who:** `PUT /api/v1/uoms/order-settings/{uomId}` — COMPANY_ADMIN / ADMIN for their company, SUPER_ADMIN for any client
  (`?companyId=`). Everyone can read `GET /api/v1/uoms/order-units` (dropdowns). `GET /api/v1/uoms/order-settings` lists every
  unit with on/off and default.

## What the unit affects

| Area | Effect |
|---|---|
| Order quantity | Entered and shown with its unit ("2 Unit M-Sand"). |
| Vehicle capacity | Not compared. Capacity is a free-text dropdown ("16 Ton") and orders may be in Units; comparing them needs a per-material conversion rule, which the business has not defined. |
| Dispatch / delivery / POD | Trip quantities are in the booking unit. "Loaded / Delivered Qty (Unit)". Billing uses the delivered quantity. |
| Stock / inventory | Not affected — stock is spare parts only (they use `spare_parts.default_uom_id`); order materials are not stocked. |
| Invoice | Line unit shown on screen, print and PDF (GST invoices must show quantity with its unit). |
| Reports | Every order-quantity report has a **Unit** column; summary rows list the units they add up ("Unit", or "Ton, Unit" if a client uses both — such totals mix units and must be read per unit). Management KPI quantity rows show the unit. |
| Driver payroll | Not affected (daily slab by trips/days, not quantity). |

## Conversion

`uom_conversions` and `GET /api/v1/uoms/convert` (V51) exist but are **not used by any business flow**, on purpose.
The V51 seed contains a global rule "1 Unit = 4.53 Ton" — real density differs per material, so it must not be used for
billing. If conversion is ever needed (e.g. weighbridge tonnes → billed Units), define a material-specific rule and add it
as an explicit feature.

## Existing data (V76)

Lines saved before V76 were entered on screens labelled "Tons", so they were marked **TON**; no quantity changed.
Every company started with **Unit** as its only, default order unit. If a client had already typed Unit quantities into
the old "Tons" field, correct those lines with a one-off script for that company (only lines not yet invoiced should be
edited; invoiced ones need the invoice cancelled first).

## Enabling Ton for a client later

Company admin: **Material & Quarry → UOM Master → Order units → Ton → Active** (optionally *Make default*).
Platform admin: **Platform Admin → Feature Access → Per client → (client) → Order units**.
Bookings then offer Unit and Ton; trips and invoices follow the booking automatically.

## Screens

Booking line: Material · Quantity (Unit) · **Unit (UOM)** · Rate / Unit · GST · amount. Booking list: **Order** column.
Trip line: Planned / Loaded / Delivered Qty (Unit), shortage in the unit. Trip list: **Load** column.
Invoice line: **Unit (UOM)**, Rate / Freight / Loading / Royalty per unit. Ready-to-bill list: "2 Unit M-Sand".
Helpers: `shared/uom-label.ts`, `shared/order-units/order-units-panel.ts`.
