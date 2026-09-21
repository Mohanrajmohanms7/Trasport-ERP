# Material & Quarry Management Documentation (Phase 10)

This document details the configuration parameters, database models, and REST endpoints for the Material & Quarry Management module of the Transport ERP.

---

## 1. Module Operations
The module controls quarry compliance metadata, unloading loading points registers, and base pricing guidelines:
- **Quarry Profiles**: Compliance metadata (Owner name, contact phone, GSTIN, License numbers, active working hours) and geolocation coordinates.
- **Loading Points**: Loading point name details, loading charges, and GPS coordinate configurations.
- **Base Pricing Rules**: Connects materials to specific cost components (Base material rates, transport rates, government royalties, and loading charges).

---

## 2. Database Schema Details
Configured schemas using Flyway `V8__material_quarry_management.sql`:
- **`quarries` table**: Altered to support owner name, license number, coordinates, and hours parameters.
- **`loading_locations` table**: Stores loading point codes, loading point names, loading charges, and GPS coordinates.
- **`material_prices` table**: Stores base material rates, transport rates, royalty rates, loading charges, and effective dates.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/loading-locations` | None | Returns active loading locations directory. |
| **POST** | `/api/v1/loading-locations` | `LoadingLocation` JSON | Registers a loading point. |
| **PUT** | `/api/v1/loading-locations/{id}` | `LoadingLocation` JSON | Updates loading point coordinates or charges. |
| **DELETE** | `/api/v1/loading-locations/{id}` | None | Soft deletes a loading point. |
| **GET** | `/api/v1/material-prices` | None | Returns active base pricing rules list. |
| **POST** | `/api/v1/material-prices` | `MaterialPrice` JSON | Adds a new pricing rule mapping. |
| **DELETE** | `/api/v1/material-prices/{id}` | None | Soft deletes a pricing entry. |

---

## 4. Frontend Angular Structure
- **`MaterialMgmtService`**: Connects REST endpoints to components.
- **`MaterialQuarryConsoleComponent`** (SCR-132 & SCR-133): Unified operations viewport split into:
  - **Material Pricing Guidelines**: Manages base pricing parameters.
  - **Loading Points**: Configures loading points coordinates and charges.
