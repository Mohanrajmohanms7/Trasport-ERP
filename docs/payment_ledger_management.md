# Customer Payments & Ledger Documentation (Phase 15)

This document details the configuration parameters, database models, and REST endpoints for the Customer Payments & Ledger Management module of the Transport ERP.

---

## 1. Module Operations
The module controls customer payments deposit receipts, running ledger balance trackers, and outstanding reconciliation:
- **Customer Receipts**: Voucher date details, customer links, booking references, received amounts, advance allocations, payment methods (CASH, UPI, GPAY, PHONEPE, NEFT, RTGS, IMPS, BANK_TRANSFER, CHEQUE), reference numbers, and remarks.
- **Customer Ledgers**: Posts debit adjustments and credit reductions automatically to compute running outstanding balances.

---

## 2. Database Schema Details
Configured schemas using Flyway `V13__customer_payments_ledger.sql`:
- **`customer_receipts` table**: Tracks entry numbers, invoice dates, customers, bookings, amounts received, and payment modes.
- **`customer_ledgers` table**: Tracks debit amounts, credit amounts, and running balances.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/receipts` | None | Returns paginated list of customer receipts. |
| **GET** | `/api/v1/receipts/{id}` | None | Returns details for a single receipt voucher. |
| **POST** | `/api/v1/receipts` | `CustomerReceipt` JSON | Registers a new payment receipt and updates ledger. |
| **PUT** | `/api/v1/receipts/{id}` | `CustomerReceipt` JSON | Updates receipt amount or payment method. |
| **DELETE** | `/api/v1/receipts/{id}` | None | Soft deletes a payment receipt. |
| **GET** | `/api/v1/customer-ledger/{customerId}` | None | Returns active running balance ledger entries list. |

---

## 4. Frontend Angular Structure
- **`PaymentMgmtService`**: Connects REST endpoints to components.
- **`PaymentDetailsConsoleComponent`** (SCR-221 & SCR-225): Unified operations viewport split into:
  - **Receipts Registry**: Displays payment deposit receipts.
  - **Receipt Form Editor**: Captures deposit details and advance mappings.
  - **Customer Ledger View**: Renders real-time running outstanding balances.
