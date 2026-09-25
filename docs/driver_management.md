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

---

## Driver Daily Slab Payroll (V62)

**Exists.** Screen: *Finance → Driver Payroll* (`/driver-payroll`). Drivers with a linked app user see *My Salary* (own POSTED/PAID slips only).

**Earning rule.** Completed trips (`trips.status = 'COMPLETED'`, driver = `trips.driver_id`, date = `trips.trip_date`)
are counted per calendar day with one grouped query. Each day earns ONE amount from the company's
`driver_pay_slabs` (e.g. 1 trip = 500, 2+ trips = 1000). Trips are never multiplied individually.
PLANNED / DISPATCHED / CANCELLED trips do not count.

**Gross** = trip earnings + basic salary (from `driver_salaries.basic_salary`, 0 for pure slab drivers) + other allowance.
**Net** = gross − deductions (FINE / DAMAGE / OTHER, `driver_payroll_deductions`) − advance recovery. Net cannot go below 0.
The day-by-day breakdown is stored in `driver_payroll_days` for the slip and audits.

**Lifecycle.** DRAFT (recalculate / edit / delete) → APPROVED (trips re-read) → POSTED (accounting, locked) → PAID.
APPROVED / POSTED / PAID can be CANCELLED; posted entries are reversed and advance recoveries returned.
One active payroll per driver per month (partial unique index; deleted and cancelled ones don't block a redo).
Numbers: `PAY-<FY>/<seq>` per company.

**Accounting** (existing JV table, fixed references per payroll → retries never duplicate):

| Step | Debit | Credit | Amount |
|---|---|---|---|
| Advance given | 1150 Driver Advances | 1000 Cash / 1010 Bank | advance |
| Payroll POST | 5150 Driver Salary Expense | 2050 Driver Salary Payable | gross |
| Payroll POST | 2050 Driver Salary Payable | 1150 Driver Advances | advance recovered (FIFO over open advances) |
| Payroll POST | 2050 Driver Salary Payable | 4900 Driver Recoveries | deductions |
| Salary PAY | 2050 Driver Salary Payable | 1000 Cash / 1010 Bank | net |

Posting date = last day of the pay month (today if the month is not over). Salary expense is recorded once, at POST.

**Advances** (`driver_advances`, `driver_advance_recoveries`): issued from the payroll screen; outstanding = amount − recovered.
An advance can be cancelled only while nothing has been recovered.

**Roles.** ADMIN / COMPANY_ADMIN / BRANCH_MANAGER / ACCOUNTANT generate, approve and pay; POST and slab changes are
ADMIN / COMPANY_ADMIN / ACCOUNTANT. Branch-bound users only see their branch. DRIVER: own slips via `/api/v1/driver-payrolls/my`.

**Not in scope:** attendance, overtime, PF/ESI/TDS, bonuses.
