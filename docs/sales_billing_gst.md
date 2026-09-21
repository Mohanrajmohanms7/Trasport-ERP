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
