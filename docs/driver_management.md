# Driver Management Documentation (Phase 8)

This document details the configuration parameters, database models, and REST endpoints for the Driver Management module of the Transport ERP.

---

## 1. Module Operations
The module controls driver personnel profiles, qualifications registry, daily attendance calendars, and salary configurations:
- **Driver Profiles**: Basic details, date of birth, blood group, emergency contact configuration, and license levels (Heavy vehicle, state permits).
- **Documents Repository**: Tracks credentials scans (License scan, Aadhaar, PAN card, Medical fitness records).
- **Daily Attendance**: Logs daily statuses (Present, Absent, Leave, Holiday) to calculate salary deductions or overtime.
- **Salary Configurator**: Captures basic monthly pay rates, daily overtime bata allocations, and salary advances releases tracking.

---

## 2. Database Schema Details
Configured schemas using Flyway `V6__driver_management.sql`:
- **`driver_documents` table**: Exposes fields for document types (Aadhaar, PAN, License scans), document card numbers, and file references paths.
- **`driver_attendance` table**: Logs daily duty dates and attendance statuses.
- **`driver_salaries` table**: Stores basic salary rates, overtime rates, and accumulated advances balances.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/drivers/{id}/documents` | None | Returns active documents list for a driver. |
| **POST** | `/api/v1/drivers/{id}/documents` | `DriverDocument` JSON | Uploads a credential document details. |
| **DELETE** | `/api/v1/drivers/{id}/documents/{docId}` | None | Soft deletes a document entry. |
| **GET** | `/api/v1/drivers/{id}/attendance` | None | Returns daily attendance logs history. |
| **POST** | `/api/v1/drivers/{id}/attendance` | `DriverAttendance` JSON | Logs a daily attendance status entry. |
| **GET** | `/api/v1/drivers/{id}/salary` | None | Returns configured salary details. |
| **POST** | `/api/v1/drivers/{id}/salary` | `DriverSalary` JSON | Saves or updates salary settings. |

---

## 4. Frontend Angular Structure
- **`DriverMgmtService`**: Connects REST endpoints to components.
- **`DriverDetailsConsoleComponent`** (SCR-084 & SCR-085): Unified operations viewport split into:
  - **Documents List**: Toggles uploads and deletes.
  - **Attendance Logs**: Logs daily statuses.
  - **Salary Configurations**: Updates salary rates and advances.
