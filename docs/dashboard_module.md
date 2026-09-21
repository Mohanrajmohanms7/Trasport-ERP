# Dashboard Module Documentation (Phase 4)

This document details the widgets configuration, REST endpoints, and directory listings for the role-based Dashboard Module of the Transport ERP.

---

## 1. Role-Based Dashboards
The system supports six enterprise-wide dashboards showing telemetry metrics:
- **Admin Dashboard**: Aggregates trip totals, available vehicles, daily/monthly revenue metrics, fuel costs, and utilization details.
- **Owner Dashboard**: Net profit, gross income, total expenses, outstanding collections, and utilization ratios.
- **Operations Dashboard**: Real-time dispatch counts, delayed routes tracking, pending dispatch flags, and quarry loading queue status.
- **Vehicle Manager Dashboard**: Fleet availability, workshop maintenance status, insurance expirations alarm, and permit renewals tracking.
- **Accountant Dashboard**: Daily receipts, outstanding balances, income/expense ledgers, and pending invoices.
- **Driver Dashboard**: Assigned dispatch routes, upcoming trips, attendance logs, and fuel slip logs.

---

## 2. Dynamic SVG Charting Widgets
To optimize rendering speed and eliminate dependency bloat in Angular 20, all analytics graphs are custom SVG-based:
- **Operations Line Trend**: Renders a dynamic curved coordinates path using SVG `<polyline>` and nodes.
- **Circular Fleet Utilization Ring**: Renders dynamic progress percentages using animated SVG `<circle>` rings.

---

## 3. Backend REST APIs

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **GET** | `/api/v1/dashboard/admin` | Returns core fleet and revenue telemetry. |
| **GET** | `/api/v1/dashboard/owner` | Returns profit & loss margins and trends. |
| **GET** | `/api/v1/dashboard/operations` | Returns loading queues and dispatch delays. |
| **GET** | `/api/v1/dashboard/vehicle` | Returns maintenance tasks and permits details. |
| **GET** | `/api/v1/dashboard/account` | Returns monthly collections ledger. |
| **GET** | `/api/v1/dashboard/driver` | Returns driver attendance and assignments. |

---

## 4. Frontend Angular Structure
- **`DashboardService`**: Connects REST endpoints to the components.
- **`DashboardComponent`**: Renders dynamic template blocks according to the active role.
- **Quick Action Bar**: Renders quick links (e.g. Add Vehicle, Receive Payment) depending on authorization level.
- **System Alarm Panel**: Aggregates expirations warnings (permit, insurance, license).
