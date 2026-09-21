# Accounts, General Ledger & Financial Management Documentation (Phase 17)

This document details the configuration parameters, database models, and REST endpoints for the Accounts, General Ledger & Financial Management module of the Transport ERP.

---

## 1. Module Operations
The module controls Chart of Accounts setup, double-entry Journal Vouchers registry, and Trial Balances validation:
- **Chart of Accounts**: Classifies ledger accounts into Assets, Liabilities, Equity, Income, and Expenses. Tracks account codes, names, opening balances, and running outstanding balances.
- **Journal Vouchers**: Registers double-entry adjustment transactions ensuring total debits equals total credits and updates corresponding account running balances.
- **Trial Balance Sheet**: Aggregates all Asset/Expense debit running balances and Liability/Equity/Income credit running balances to ensure the ledger is balanced.

---

## 2. Database Schema Details
Configured schemas using Flyway `V15__accounts_general_ledger.sql`:
- **`chart_of_accounts` table**: Tracks account codes, names, types, and balances.
- **`journal_vouchers` table**: Tracks voucher dates, debit accounts, credit accounts, and amounts.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/accounts` | None | Returns paginated list of chart of accounts. |
| **GET** | `/api/v1/accounts/{id}` | None | Returns details for a single ledger account. |
| **POST** | `/api/v1/accounts` | `ChartOfAccount` JSON | Registers a new account master. |
| **PUT** | `/api/v1/accounts/{id}` | `ChartOfAccount` JSON | Updates account name or category. |
| **DELETE** | `/api/v1/accounts/{id}` | None | Soft deletes a ledger account. |
| **GET** | `/api/v1/journal` | None | Returns paginated list of journal entries. |
| **POST** | `/api/v1/journal` | `JournalVoucher` JSON | Posts double entry journal vouchers and updates balances. |

---

## 4. Frontend Angular Structure
- **`AccountsMgmtService`**: Connects REST endpoints to components.
- **`AccountsDetailsConsoleComponent`** (SCR-261 & SCR-264): Unified operations viewport split into:
  - **Chart of Accounts**: Displays general ledger accounts list.
  - **Journal Book**: Renders posted double-entry journal vouchers.
  - **Trial Balance Summary**: Displays balanced debit/credit columns ledger summaries.
