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

---

## Trip rules (V61)

**Exists**:

- Trips can only be created for **APPROVED** bookings. The booking row is locked while a trip is saved.
- Every trip line must be a material on the booking. Total quantity moved per material across non-cancelled
  trips (delivered if recorded, else planned) cannot exceed the booked quantity plus the
  `BOOKING_QTY_TOLERANCE_PERCENT` app setting (default 0).
- Missing rate / royalty / loading on a trip line is copied from the booking line.
- **Weighbridge**: `trip_details.loaded_quantity` and `delivered_quantity`; shortage = loaded − delivered
  (API field `shortageQuantity`). Delivered cannot exceed loaded. `billableQuantity` = delivered if recorded.
- **Loading source**: optional `trips.quarry_id` and `trips.loading_location_id` (same company only).
- **Trip date** can be back-dated, not in the future and not before the booking date.
- **Status flow** is enforced: PLANNED → DISPATCHED (needs vehicle + driver) → COMPLETED.
- A **completed** trip can still get weighbridge values, date and loading source corrected; vehicle, driver,
  material and planned quantity are locked. Once the trip is on an active invoice, it cannot be edited.
