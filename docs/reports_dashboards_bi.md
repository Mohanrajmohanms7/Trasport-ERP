# Reports, Dashboards & BI Documentation (Phase 18)

This document details the configuration parameters, database models, and REST endpoints for the Reports, Dashboards & Business Intelligence module of the Transport ERP.

---

## 1. Module Operations
The module controls executive BI key performance indicators dashboard, custom layout reports generation, and automated cron triggers:
- **Executive BI KPI Cards**: Aggregates real-time corporate metrics (Today's Revenue, Expenses, Net Profit Margin, active fleet ratio).
- **Interactive SVG Trend Charts**: Visualizes weekly operations trends dynamically.
- **Report Templates**: Design custom layouts specifying column selections and query targets.
- **Scheduled Triggers**: Automated cron jobs executing reports templates distribution to configured emails.

---

## 2. Database Schema Details
Configured schemas using Flyway `V16__reports_dashboards.sql`:
- **`report_templates` table**: Tracks template names, categories, and selected columns list.
- **`scheduled_reports` table**: Tracks cron patterns, templates, and recipient email addresses.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/reports` | None | Returns paginated list of report templates. |
| **POST** | `/api/v1/reports` | `ReportTemplate` JSON | Registers a new custom report layout template. |
| **DELETE** | `/api/v1/reports/{id}` | None | Soft deletes a report template. |
| **POST** | `/api/v1/reports/export` | Export JSON | Compiles template columns data into mock Excel links. |
| **GET** | `/api/v1/reports/schedule` | None | Returns paginated scheduled report triggers list. |
| **POST** | `/api/v1/reports/schedule` | `ScheduledReport` JSON | Configures a new scheduled report cron trigger. |
| **DELETE** | `/api/v1/reports/schedule/{id}` | None | Deletes a scheduled report trigger. |

---

## 4. Frontend Angular Structure
- **`ReportMgmtService`**: Connects REST endpoints to components.
- **`ReportDetailsConsoleComponent`** (SCR-280 & SCR-294): Unified operations viewport split into:
  - **Executive BI Viewport**: Renders KPI cards and SVG line charts trends.
  - **Report Templates**: Renders custom reports list and creation form.
  - **Scheduled Triggers**: Renders automated scheduled report triggers.
