# Vehicle Management Documentation (Phase 7)

This document details the configuration parameters, database models, and REST endpoints for the Vehicle Management module of the Transport ERP.

---

## 1. Module Operations
The module controls fleet assets, documentations registers, and active assignments logs:
- **Vehicle Profiles**: Details mechanical parameters (Brand, Model, Chassis, Engine capacity, categories) and registration info.
- **Documents Repository**: Tracks scanned uploads (Insurance, Permits, Fitness certificates, PUC) and automatic expiration warnings.
- **Workshop Maintenance Records**: Logs repairs type, workshops details, actual repair cost, and next schedules dates.
- **Driver Assignments**: Tracks history logs of assigned personnel to each fleet truck.

---

## 2. Database Schema Details
Configured schemas using Flyway `V5__vehicle_management.sql`:
- **`vehicle_documents` table**: Exposes fields for document types (Insurance, Permits, Fitness, PUC), certificate numbers, date parameters, and file references paths.
- **`vehicle_services` table**: Logs types, workshop name details, exact cost amounts, and next service dates.
- **`vehicle_driver_assignments` table**: Connects a vehicle record to a driver, capturing assignment dates and removal dates.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/vehicles/{id}/documents` | None | Returns active documents list for a vehicle. |
| **POST** | `/api/v1/vehicles/{id}/documents` | `VehicleDocument` JSON | Uploads a registration document details. |
| **DELETE** | `/api/v1/vehicles/{id}/documents/{docId}` | None | Soft deletes a document entry. |
| **GET** | `/api/v1/vehicles/{id}/maintenance` | None | Returns workshop repairs log history. |
| **POST** | `/api/v1/vehicles/{id}/service` | `VehicleServiceLog` JSON | Logs a new maintenance entry. |
| **GET** | `/api/v1/vehicles/{id}/driver` | None | Returns driver assignments log list. |
| **POST** | `/api/v1/vehicles/{id}/driver/{drvId}` | None | Links a driver to the vehicle. |
| **DELETE** | `/api/v1/vehicles/{id}/driver` | None | Unassigns the current active driver. |

---

## 4. Frontend Angular Structure
- **`VehicleMgmtService`**: Connects REST endpoints to components.
- **`VehicleDetailsConsoleComponent`** (SCR-064 & SCR-065): Unified operations viewport split into:
  - **Documents List**: Toggles uploads and deletes.
  - **Service Logs**: Lists repairs history.
  - **Driver Assignment**: Shows logs history and links personnel via dropdown menus.
