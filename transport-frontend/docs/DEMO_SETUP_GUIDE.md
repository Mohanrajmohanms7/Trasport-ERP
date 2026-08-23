# FleetFlow ERP — Fresh Demo Setup Guide

How to give someone a **fresh demo ERP** with accounts and sample data.

---

## What gets created automatically?

On **first backend start**, `DataInitializer` (when `app.bootstrap.enabled=true`) creates:

| Item | Value |
|------|--------|
| Company | `DEMO` — Demo Transport ERP |
| Branch | `HO` — Head Office |
| Role | `SUPER_ADMIN` + all 10 permissions |
| User | **`admin` / `Admin@123`** |
| Financial year | Current Indian FY |
| Lookups / settings | Reference lists only |

It does **not** create vehicles, drivers, bookings, invoices, or extra role users.

---

## Option 1 — Fastest demo (recommended for UI walkthrough)

Use this when you want one admin + full business sample data.

### Prerequisites
1. PostgreSQL running with DB `transport_erp`  
   (see `application-dev.yml`: user `transport_admin` / `AdminPass123`)
2. Backend on `:8080`
3. Frontend on `:4200` (`npm start`)

### Steps
1. Start backend → bootstrap creates `admin`.
2. Open `http://localhost:4200/login`
3. Login: **`admin` / `Admin@123`**
4. You are sent to **Setup Wizard** (`/setup`) if setup is incomplete.
5. Click **Seed Demo Data**  
   - Calls `POST /api/v1/setup/seed-demo`  
   - Resets business tables  
   - Loads vehicles, drivers, customers, bookings, trips, fuel, expense, invoices, payments, accounts, GPS, etc.  
   - Marks setup completed  
   - Opens **Dashboard**
6. Explore Masters → Operations → Finance with realistic linked data.

**Keep this login:** `admin` / `Admin@123` (SUPER_ADMIN)

---

## Option 2 — Manual empty company (no sample transactions)

1. Login as `admin` / `Admin@123`
2. On Setup Wizard, **do not** click Seed Demo Data
3. Fill steps: Company → Branch → Vehicle → Driver → Customer → Material
4. Click **Finish Setup** → Dashboard  
   (or Skip and enter data later from screens)

---

## Option 3 — Full multi-role demo accounts (ABC Transport)

Built-in **Seed Demo Data** keeps existing users/roles.  
To add **all demo roles** (manager, operator, accountant, driver, viewer…), run the ordered SQL under:

`transport-backend/src/main/resources/db/demo/`

### Suggested order (fresh DB after Flyway)

```text
001_company.sql
002_branch.sql
003_roles.sql          ← SUPER_ADMIN, DISPATCHER, ACCOUNTANT + permissions
004_users.sql          ← admin, dispatcher, accountant (DEMO company)
005_lookups.sql
006_masters.sql
007_fleet.sql
008_abc_demo_users.sql ← COMPANY_ADMIN, MANAGER, OPERATOR, DRIVER, VIEWER + ABC users
009_abc_demo_scenarios.sql
999_demo_transactions.sql   (optional)
```

Or after app is already running with bootstrap:

1. Login as admin  
2. Click **Seed Demo Data** (business data)  
3. In pgAdmin / `psql`, also run:
   - `003_roles.sql` (if DISPATCHER/ACCOUNTANT missing)
   - `008_abc_demo_users.sql` (multi-role logins)

### ABC demo logins (`008_abc_demo_users.sql`)

| Username | Password | Role |
|----------|----------|------|
| `superadmin` | `Super@123` | SUPER_ADMIN |
| `admin` | `Admin@123` | COMPANY_ADMIN *(overwrites mapping for this username)* |
| `manager` | `Manager@123` | MANAGER |
| `operator` | `Operator@123` | OPERATOR |
| `accountant` | `Accountant@123` | ACCOUNTANT |
| `driver1` | `Driver@123` | DRIVER |
| `viewer` | `Viewer@123` | VIEWER |

> **Caution:** `008` remaps username `admin` to **COMPANY_ADMIN**.  
> Bootstrap `admin` is normally **SUPER_ADMIN**. After `008`, use `superadmin` for full access, or re-assign role in Users & Roles.

### DEMO company logins (`004_users.sql`) — if applied

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@123` | SUPER_ADMIN |
| `dispatcher` | `Dispatcher@123` | DISPATCHER |
| `accountant` | `Accountant@123` | ACCOUNTANT |

---

## Option 4 — Completely wipe and start over

```sql
-- DANGER: destroys all ERP data in this database
DROP DATABASE transport_erp;
CREATE DATABASE transport_erp OWNER transport_admin;
```

Then:

1. Start backend → Flyway V1–V28 + bootstrap `admin`
2. Login → **Seed Demo Data**
3. (Optional) Run `003_roles.sql` + `008_abc_demo_users.sql` for multi-role accounts

---

## API shortcuts (backend must be up)

| Action | Method | URL |
|--------|--------|-----|
| Setup status | GET | `/api/v1/setup/status` |
| Mark setup done | POST | `/api/v1/setup/complete` |
| Reset + seed demo business data | POST | `/api/v1/setup/seed-demo` |

Example:

```bash
# After login, or with curl if endpoint is permitAll in SecurityConfig
curl -X POST http://localhost:8080/api/v1/setup/seed-demo
```

---

## What “Seed Demo Data” does / does not do

| Does | Does not |
|------|----------|
| Truncate business/transaction tables | Delete `app_users` / `app_roles` / permissions |
| Insert linked masters + sample ops/finance | Create manager/operator/driver demo users |
| Set `SETUP_COMPLETED=true` | Replace bootstrap password |

For **demo accounts by role**, use Option 3 SQL (`008_abc_demo_users.sql`).

---

## Checklist to hand a fresh demo to someone

1. [ ] PostgreSQL `transport_erp` ready  
2. [ ] Backend started (bootstrap OK)  
3. [ ] Frontend `npm start`  
4. [ ] Login `admin` / `Admin@123`  
5. [ ] Setup → **Seed Demo Data**  
6. [ ] (Optional) Run ABC role SQL for multi-user demo  
7. [ ] Share login table above  

---

## Config reference

```yaml
# application.yml
app:
  bootstrap:
    enabled: true
    admin-password: Admin@123
```

```yaml
# application-dev.yml
spring.datasource.url: jdbc:postgresql://localhost:5432/transport_erp
```
