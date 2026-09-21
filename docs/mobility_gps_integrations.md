# Mobility, GPS & Integrations Documentation (Phase 19)

This document details the configuration parameters, database models, and REST endpoints for the Enterprise Mobility, GPS, AI & Integrations module of the Transport ERP.

---

## 1. Module Operations
The module controls active vehicle tracking, AI predictive analysis models, and database snapshot utilities:
- **Live GPS Tracking**: Real-time speed telemetry and ping coordinates logs visual overlays.
- **AI Telemetry Forecasts**: Models vehicle maintenance thresholds, fuel variances, and delay index probabilities.
- **Backup Snapshot Utility**: Triggers snapshots and tracks backup logs sizes.

---

## 2. Database Schema Details
Configured schemas using Flyway `V17__gps_ai_mobility.sql`:
- **`gps_trackings` table**: Tracks latitudes, longitudes, speeds, and ping timestamps.
- **`ai_predictions` table**: Tracks diagnostic forecasts targets, match probabilities, and suggestions.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/gps/live` | None | Returns live route pings log history for selected vehicle. |
| **POST** | `/api/v1/gps/location` | `GpsTracking` JSON | Registers a live GPS ping coordinate update. |
| **GET** | `/api/v1/ai/dashboard` | None | Returns AI predictive insights dashboard content. |

---

## 4. Frontend Angular Structure
- **`MobilityMgmtService`**: Connects REST endpoints to components.
- **`MobilityDetailsConsoleComponent`** (SCR-304 & SCR-313): Unified operations viewport split into:
  - **Live GPS Tracking**: Displays vehicle ping overlay coordinates log.
  - **AI Insights Dashboard**: Displays maintenance and delay predictions.
  - **Backup & Snapshots**: Triggers database backups and logs snapshots.
