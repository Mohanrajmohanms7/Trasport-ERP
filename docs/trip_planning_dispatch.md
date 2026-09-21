# Trip Planning & Dispatch Documentation (Phase 12)

This document details the configuration parameters, database models, and REST endpoints for the Trip Planning & Dispatch Management module of the Transport ERP.

---

## 1. Module Operations
The module controls trip planning itineraries, driver and vehicle allocations, and transit statuses dispatch:
- **Trip Headers**: Trip dates details, booking references, vehicle registrations, driver allocations, and transit statuses (PLANNED, DISPATCHED, COMPLETED, CANCELLED).
- **Itemized Allocations**: Tracks payload quantities, material categories, loading charges, royalties, dispatch timestamps, and arrival times.
- **Workflow Transit Control**: Drives transitions between planned, transit start dispatches, and delivery completions.

---

## 2. Database Schema Details
Configured schemas using Flyway `V10__trip_planning_dispatch.sql`:
- **`trips` table**: Tracks planned trip registration numbers, allocated vehicles, drivers, and status flags.
- **`trip_details` table**: Tracks quantities, rates, loading charges, royalty rates, dispatch times, and arrival times.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/trips` | None | Returns paginated list of trips. |
| **GET** | `/api/v1/trips/{id}` | None | Returns details for a single planned trip. |
| **POST** | `/api/v1/trips` | `Trip` JSON | Plans a new dispatch trip itinerary. |
| **PUT** | `/api/v1/trips/{id}` | `Trip` JSON | Modifies allocated vehicle or driver. |
| **DELETE** | `/api/v1/trips/{id}` | None | Cancels planned trip. |
| **POST** | `/api/v1/trips/{id}/dispatch` | None | Marks trip as DISPATCHED in transit. |
| **POST** | `/api/v1/trips/{id}/complete` | None | Marks trip as COMPLETED delivered. |

---

## 4. Frontend Angular Structure
- **`TripMgmtService`**: Connects REST endpoints to components.
- **`TripDetailsConsoleComponent`** (SCR-162 & SCR-163): Unified operations viewport split into:
  - **Planned Trips Grid**: Displays planned transits list.
  - **Allocation Form Editor**: Allocates vehicles, drivers, and payloads.
  - **Workflow Controls**: Toggles Dispatch transit start and Complete delivery confirmations.
