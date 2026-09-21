# Fuel Management Documentation (Phase 13)

This document details the configuration parameters, database models, and REST endpoints for the Fuel Management module of the Transport ERP.

---

## 1. Module Operations
The module controls fuel allocations, odometer mileage calculations, and fuel request approval workflows:
- **Fuel Entries**: Date details, vehicle links, driver links, trip references, fuel stations, fuel quantities, rates per litre, total amounts, and odometer readings.
- **Request Approvals**: Logs fuel requests requested quantities, budgets requested amounts, and status checks (PENDING, APPROVED, REJECTED).
- **Odometer and Mileage**: Tracks odometer previous/current parameters to calculate fuel consumption averages.

---

## 2. Database Schema Details
Configured schemas using Flyway `V11__fuel_management.sql`:
- **`fuel_entries` table**: Tracks entry numbers, invoice reference numbers, quantities, rate factors, odometer readings, and payment modes.
- **`fuel_requests` table**: Tracks request numbers, trip reference mappings, requested quantity buffers, and approval states.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/fuel` | None | Returns paginated list of fuel entries. |
| **GET** | `/api/v1/fuel/{id}` | None | Returns details for a single fuel entry log. |
| **POST** | `/api/v1/fuel` | `FuelEntry` JSON | Records a new fuel entry. |
| **PUT** | `/api/v1/fuel/{id}` | `FuelEntry` JSON | Updates fuel quantity or odometer. |
| **DELETE** | `/api/v1/fuel/{id}` | None | Soft deletes a fuel log. |
| **GET** | `/api/v1/fuel/request` | None | Returns paginated list of fuel requests. |
| **POST** | `/api/v1/fuel/request` | `FuelRequest` JSON | Submits a fuel request. |
| **POST** | `/api/v1/fuel/request/{id}/approve` | None | Approves a fuel request. |
| **POST** | `/api/v1/fuel/request/{id}/reject` | None | Rejects a fuel request. |

---

## 4. Frontend Angular Structure
- **`FuelMgmtService`**: Connects REST endpoints to components.
- **`FuelDetailsConsoleComponent`** (SCR-181 & SCR-185): Unified operations viewport split into:
  - **Fuel Logging Records**: Displays refueling entries list.
  - **Refueling Form Editor**: Computes total pricing and odometer updates.
  - **Requests Workflow Grid**: Manages and processes fuel approvals.
