# Sales Invoice, Billing & GST Documentation (Phase 16)

This document details the configuration parameters, database models, and REST endpoints for the Sales Invoice, Billing & GST Management module of the Transport ERP.

---

## 1. Module Operations
The module controls sales invoices creation, CGST/SGST tax configurations, multi-trips billing consolidation, and ledger integration:
- **Sales Invoices**: Tracks billing invoice numbers, invoice dates, customers details, payment terms, itemized list arrays, subtotal values, and net totals.
- **Multiple Trips Consolidation**: Supports mapping multiple completed transit trips under one consolidated tax invoice.
- **GST Tax Breaks**: Dynamically calculates CGST/SGST/IGST components based on HSN/GST configurations (default 18%).
- **Ledger Posting**: Automatically debit entries posted to Customer Ledger upon invoice approval.

---

## 2. Database Schema Details
Configured schemas using Flyway `V14__sales_invoices.sql`:
- **`sales_invoices` table**: Tracks entry numbers, invoice dates, customers, payment terms, and net billing totals.
- **`sales_invoice_details` table**: Tracks itemized quantities, rates, freights, royalties, and computed taxes components.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/invoices` | None | Returns paginated list of sales invoices. |
| **GET** | `/api/v1/invoices/{id}` | None | Returns details for a single sales invoice. |
| **POST** | `/api/v1/invoices` | `SalesInvoice` JSON | Generates a new invoice draft. |
| **PUT** | `/api/v1/invoices/{id}` | `SalesInvoice` JSON | Updates invoice details or itemized lines. |
| **DELETE** | `/api/v1/invoices/{id}` | None | Soft deletes an invoice. |
| **POST** | `/api/v1/invoices/{id}/approve` | None | Approves invoice and posts debit entry to ledger. |
| **POST** | `/api/v1/invoices/{id}/cancel` | None | Cancels an invoice. |

---

## 4. Frontend Angular Structure
- **`InvoiceMgmtService`**: Connects REST endpoints to components.
- **`InvoiceDetailsConsoleComponent`** (SCR-241 & SCR-242): Unified operations viewport split into:
  - **Invoices Ledger**: Displays sales tax invoices.
  - **Billing Form Editor**: Creates/edits itemized billing rows mapping trips/materials.

---

## GST calculation, numbering and posting (V61)

**Exists** as of migration `V61__invoice_gst_numbering_trip_integrity.sql`.

- **Line taxable value** = quantity × (rate + freight + loading + royalty), rounded to 2 decimals.
- **Discount** is split across lines in proportion to taxable value and is taken off **before** GST
  (`sales_invoice_details.discount_amount`, `taxable_amount`).
- **Place of supply**: the invoice's `place_of_supply` (2-digit state code) if entered, else the first two
  digits of the customer GSTIN. Supplier state = branch GSTIN, else company GSTIN.
  Different states → `supply_type = INTER_STATE` and **IGST**; otherwise **CGST + SGST** (half each).
- Header totals: `subtotal` (pre-discount), `discount`, `taxable_amount`, `tax_amount`, `net_amount = taxable + tax`.
  Logic lives in `InvoiceTaxCalculator` (unit-tested in `InvoiceTaxCalculatorTest`).
- **Approval posting**: Dr Customer Receivables / Cr Transport Freight Income for `taxable_amount`, and
  Dr Customer Receivables / Cr GST Liability for `tax_amount`, dated on the invoice date. Drafts are
  recalculated on approval. (Before V61 the whole tax-inclusive amount was posted to income and GST
  liability was never posted.)
- **Invoice date** may be back-dated, never in the future, never before a billed trip's date, and must stay in
  the financial year of its number.
- **Trip lines**: only COMPLETED trips of the same customer, not on another non-cancelled invoice (row-locked).
  Invoice-from-trip bills the weighbridge **delivered** quantity when recorded and falls back to booking
  rates when the trip rate is 0.
- **Document numbers**: `DocumentNumberService` issues `PREFIX + FY + "/" + 5 digits` (e.g. `INV-2627/00001`)
  from `document_sequences`, per company, document type and April–March year, for invoices, receipts,
  bookings, trips, expenses, fuel entries/requests and journal vouchers.

**Planned / configurable**: a separate GST rate for freight (GTA) vs material. Confirm the tax treatment with
the company's CA before adding it; today one GST % applies to the whole line.
