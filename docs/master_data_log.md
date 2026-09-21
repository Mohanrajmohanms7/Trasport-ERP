# Master Data Foundation Log (Section 1)

This document details the database migration schemas, audit model properties, API handlers, and Angular client views built for the Master Data Foundation.

---

## 1. Database Schema Migration (V2)
Schema migrations are handled dynamically with Flyway via `V2__create_masters.sql`. The script configures 14 tables using `BIGSERIAL` primary keys:
- **Tenant Scope:** `companies`, `branches`.
- **Identity Scope:** `app_roles`, `app_permissions`, `role_permissions` join, `app_users`, `user_roles` join.
- **Unified Metadata:** `lookup_values` (classification category for departments, vehicle type, fuel type, etc.).
- **Domain Scope:** `vehicles`, `drivers`, `customers`, `suppliers`, `materials`, `quarries`.

---

## 2. BaseEntity Audit Mapping
Every master table inherits its core fields from the Java mapped superclass `BaseEntity.java`:
- **Identity:** `Long id`, `String code` (unique), `String name`.
- **Audit Columns:** `String createdBy`, `LocalDateTime createdDate`, `String updatedBy`, `LocalDateTime updatedDate`, `Integer version`.
- **Tenant Scope:** `Long companyId`, `Long branchId`.
- **Soft Deletion:** `Boolean isDeleted` (defaults to false).
- **LifeCycle Hooks:** `@PrePersist` and `@PreUpdate` interceptors to auto-populate timestamp parameters.

---

## 3. API Handlers & Controllers
- **Consistent Envelope:** All responses are mapped to a structured DTO layout:
  ```json
  {
    "success": true,
    "message": "String context message",
    "data": {},
    "errors": []
  }
  ```
- **REST Endpoints:** Exposes paginated query filters on code/name, details retrieval, deletion triggers, and row status toggling (Active/Inactive).

---

## 4. Frontend Masters Management Component
- **MasterService.ts**: Houses unified client commands targeting REST APIs.
- **MasterManagement Dashboard**: Developed in standalone Angular 20. Displays listing tables with categories switcher and slide-out forms for CRUD updates.
- **Material Providers**: Configured Material animations, forms validation, and Http client interceptors in `app.config.ts`.
