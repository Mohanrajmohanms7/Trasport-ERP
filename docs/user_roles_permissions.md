# User, Role & Permission Management Documentation (Phase 5)

This document details the architecture, schemas, and endpoint design for the Identity and Access Management (IAM) module of the Transport ERP.

---

## 1. Role-Permission Architecture
The system utilizes Role-Based Access Control (RBAC) with functional authorization:
- **`app_users`**: User identities, linked to `companies` and `branches`.
- **`app_roles`**: Administrative roles (e.g. `ADMIN`, `OWNER`, `OPERATIONS`, `VEHICLE`, `ACCOUNTANT`, `DRIVER`).
- **`app_permissions`**: Granular security capabilities (e.g. `VIEW`, `ADD`, `EDIT`, `DELETE`, `APPROVE`, `REJECT`).
- **`role_permissions` Matrix**: Connects roles to permissions, letting administrators dynamically toggle capability bindings.

---

## 2. Database Schema Structures
The IAM module operates on the following tables:
- **`app_users`**: Employee code, username, email, phone, status, and company/branch mappings.
- **`app_roles`**: Code, name, status, and description.
- **`app_permissions`**: Code, name, and description.
- **`user_roles`**: Maps user ID to role ID.
- **`role_permissions`**: Maps role ID to permission ID.

---

## 3. Backend REST APIs Catalog

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/users` | None | Returns paginated list of active users. |
| **GET** | `/api/v1/users/{id}` | None | Returns specifications for a single user. |
| **POST** | `/api/v1/users` | `AppUser` JSON | Creates user, hashes password via BCrypt. |
| **PUT** | `/api/v1/users/{id}` | `AppUser` JSON | Modifies user details, status, and roles. |
| **DELETE** | `/api/v1/users/{id}` | None | Soft deletes user account. |
| **GET** | `/api/v1/roles` | None | Returns list of active enterprise roles. |
| **POST** | `/api/v1/roles` | `AppRole` JSON | Registers a new security role. |
| **PUT** | `/api/v1/roles/{id}` | `AppRole` JSON | Modifies role info and permissions links. |
| **DELETE** | `/api/v1/roles/{id}` | None | Soft deletes role. |
| **GET** | `/api/v1/permissions` | None | Returns functional authorization capabilities. |

---

## 4. Frontend Angular Structure
- **`UserRoleService`**: Connects REST endpoints to components.
- **`UserRoleManagementComponent`** (SCR-020 & SCR-028): Unified administration dashboard split into:
  - **Users Panel**: Grid lists with add/edit slide-out sheets.
  - **Roles Panel**: Roles list and interactive **Permission Matrix** checking functional checkboxes capabilities.
