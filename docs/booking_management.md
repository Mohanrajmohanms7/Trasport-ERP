# Booking Management Documentation (Phase 11)

This document details the configuration parameters, database models, and REST endpoints for the Booking Management module of the Transport ERP.

---

## 1. Module Operations
The module controls customer booking creations, itemized quantity pricing, and workflow status reviews:
- **Booking Headers**: Date details, customer links, delivery locations, internal remarks, and priority classification (High, Medium, Low).
- **Itemized Details**: Maps multiple requested materials, quantities, base rates, transport parameters, royalties, and GST tax percentages to calculate the net total amount automatically.
- **Workflow Approvals**: Logs transitions between Draft, Pending Review, Approved, and Rejected states.

---

## 2. Database Schema Details
Configured schemas using Flyway `V9__booking_management.sql`:
- **`bookings` table**: Tracks booking registration numbers, status flags, remarks, and priority parameters.
- **`booking_details` table**: Tracks quantities, rates, transport rates, government royalties, loading charges, GST rates, and net totals.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/bookings` | None | Returns paginated list of bookings. |
| **GET** | `/api/v1/bookings/{id}` | None | Returns details for a single booking. |
| **POST** | `/api/v1/bookings` | `Booking` JSON | Registers a customer booking request. |
| **PUT** | `/api/v1/bookings/{id}` | `Booking` JSON | Modifies booking headers or details. |
| **DELETE** | `/api/v1/bookings/{id}` | None | Soft deletes / cancels booking. |
| **POST** | `/api/v1/bookings/{id}/approve` | None | Approves a booking request. |
| **POST** | `/api/v1/bookings/{id}/reject` | None | Rejects a booking request. |

---

## 4. Frontend Angular Structure
- **`BookingMgmtService`**: Connects REST endpoints to components.
- **`BookingDetailsConsoleComponent`** (SCR-141 & SCR-142): Unified operations viewport split into:
  - **Booking Requests Grid**: Displays registered bookings list.
  - **Request Form Editor**: Configures itemized materials rates and quantities.
  - **Workflow Controls**: Toggles Approve, Reject, and Cancel actions.
