# Expense Management Documentation (Phase 14)

This document details the configuration parameters, database models, and REST endpoints for the Expense Management module of the Transport ERP.

---

## 1. Module Operations
The module controls operational expense logs, toll / parking vouchers tracking, and manager approvals workflows:
- **Expense Entries**: Voucher date details, subtotal amounts, GST taxes, calculated totals, categories, payment methods (CASH, UPI, cheque, bank transfers, credit), and optional links to vehicle driver and transit trip.
- **Voucher Categories**: Classifies expenses (TOLL, DRIVER_BATA, PARKING, VEHICLE_REPAIR, INSURANCE, OFFICE, MISCELLANEOUS).
- **Workflow Approvals**: Logs transitions between Draft, Submitted, Approved, Rejected, Paid, and Cancelled.

---

## 2. Database Schema Details
Configured schemas using Flyway `V12__expense_management.sql`:
- **`expenses` table**: Tracks entry numbers, categories, descriptions, amounts, taxes, payment modes, remarks, and approval states.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/expenses` | None | Returns paginated list of expense entries. |
| **GET** | `/api/v1/expenses/{id}` | None | Returns details for a single expense voucher. |
| **POST** | `/api/v1/expenses` | `Expense` JSON | Registers a new expense voucher. |
| **PUT** | `/api/v1/expenses/{id}` | `Expense` JSON | Updates expense amount or category details. |
| **DELETE** | `/api/v1/expenses/{id}` | None | Soft deletes an expense voucher. |
| **POST** | `/api/v1/expenses/{id}/approve` | None | Approves an expense request. |
| **POST** | `/api/v1/expenses/{id}/reject` | None | Rejects an expense request. |

---

## 4. Frontend Angular Structure
- **`ExpenseMgmtService`**: Connects REST endpoints to components.
- **`ExpenseDetailsConsoleComponent`** (SCR-201 & SCR-206): Unified operations viewport split into:
  - **Vouchers Ledger**: Displays refueling entries list.
  - **Voucher Form Editor**: Computes total pricing and GST tax components.
  - **Approvals review grid**: Manages and processes manager approvals.
