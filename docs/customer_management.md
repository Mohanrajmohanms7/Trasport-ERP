# Customer Management Documentation (Phase 9)

This document details the configuration parameters, database models, and REST endpoints for the Customer Management module of the Transport ERP.

---

## 1. Module Operations
The module controls customer directory settings, contacts list registry, delivery sites, and tax configurations:
- **Customer Profiles**: Legal name details, PAN/GST registration card parameters, credit limits, and active status indicators.
- **Multiple Sites & Unloading**: Configures delivery locations site code designations and unloading notes.
- **Contact Directory**: Renders customer designations cards, emails, and mobile phone contacts list.
- **Tax and Compliance Docs**: Links documents (GSTIN Certificate, PAN card copy, Agreements contract papers).

---

## 2. Database Schema Details
Configured schemas using Flyway `V7__customer_management.sql`:
- **`customer_contacts` table**: Stores designations, phones, and emails.
- **`customer_delivery_sites` table**: Stores delivery site codes, names, unloading addresses, and manager contacts.
- **`customer_documents` table**: Tracks files path references for KYC, GSTIN, PAN, and agreements.

---

## 3. Backend REST APIs

| Method | Endpoint | Request Body | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/customers/{id}/contacts` | None | Returns active contacts directory. |
| **POST** | `/api/v1/customers/{id}/contacts` | `CustomerContact` JSON | Adds a new contact personnel. |
| **DELETE** | `/api/v1/customers/{id}/contacts/{cId}` | None | Soft deletes a contact entry. |
| **GET** | `/api/v1/customers/{id}/delivery-sites` | None | Returns active unloading sites list. |
| **POST** | `/api/v1/customers/{id}/delivery-sites` | `CustomerDeliverySite` JSON | Creates a delivery site registry. |
| **DELETE** | `/api/v1/customers/{id}/delivery-sites/{sId}` | None | Soft deletes a delivery site location. |
| **GET** | `/api/v1/customers/{id}/documents` | None | Returns registered compliance documents list. |
| **POST** | `/api/v1/customers/{id}/documents` | `CustomerDocument` JSON | Uploads a legal document. |

---

## 4. Frontend Angular Structure
- **`CustomerMgmtService`**: Connects REST endpoints to components.
- **`CustomerDetailsConsoleComponent`** (SCR-104 & SCR-105): Unified operations viewport split into:
  - **Delivery Sites**: Configures unloading site codes.
  - **Contacts Directory**: Renders designations and mobile listings.
  - **Compliance Documents**: Toggles uploads and deletes.
