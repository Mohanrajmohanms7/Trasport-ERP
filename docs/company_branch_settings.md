# Company, Branch & Settings Documentation (Phase 6)

This document details the configuration properties, database models, and REST endpoints for the Company Administration module of the Transport ERP.

---

## 1. Company Administration Features
The module manages the tenant structures, location registries, and core settings:
- **Company Profile**: Captures legal and registration parameters (CIN, GSTIN, PAN), timezone configuration, website, address details, and digitised logos.
- **Branch Management**: Controls office locations details, manager assignments, and geographical coordinates (Latitude & Longitude).
- **Financial Year Master**: Manages start and end dates parameters, default fiscal year alarms, and active/inactive status toggles.
- **Business Preferences Settings**: Exposes number series mappings (custom prefix sequence for trips, vehicles, customer registries, and invoice systems), default GST tax percentages, base currency specifications, and SMTP mail servers settings.

---

## 2. Database Schema Details
Extends database schemas using Flyway `V4__company_administration.sql`:
- **`companies` / `branches` tables**: Altered to support manager info, GST registry details, and latitude/longitude coordinates.
- **`financial_years` table**: Tracks year code, dates limits, status flags, and default year overrides.
- **`app_settings` table**: Key-value pairs dictionary storing SMTP settings, number sequence masks, and configurations.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/companies/1` | None | Returns corporate profile details. |
| **PUT** | `/api/v1/companies/1` | `Company` JSON | Updates corporate name, addresses, and tax numbers. |
| **GET** | `/api/v1/branches` | None | Returns branch office locations list. |
| **POST** | `/api/v1/branches` | `Branch` JSON | Creates a branch office registry. |
| **PUT** | `/api/v1/branches/{id}` | `Branch` JSON | Updates branch manager details or coordinates. |
| **DELETE** | `/api/v1/branches/{id}` | None | Soft deletes a branch office location. |
| **GET** | `/api/v1/financial-years` | None | Returns active fiscal years list. |
| **POST** | `/api/v1/financial-years` | `FinancialYear` JSON | Registers a new fiscal financial year. |
| **PUT** | `/api/v1/financial-years/{id}` | `FinancialYear` JSON | Updates details or defaults settings. |
| **GET** | `/api/v1/settings` | None | Returns application preferences map. |
| **PUT** | `/api/v1/settings` | Map of settings | Saves preference configurations (SMTP, number series). |

---

## 4. Frontend Angular Structure
- **`CompanyAdminService`**: Connects REST endpoints to components.
- **`CompanyAdministrationComponent`** (SCR-041 & SCR-042): Unified administration viewport divided into:
  - **Company Specs**: Configures corporate details.
  - **Branch Panel**: List grid with coordinates.
  - **Fiscal Years Panel**: Sets up dates ranges.
  - **Preferences Panel**: Number sequences prefixes, default GST tax rates, and SMTP configs.
