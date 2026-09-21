# UI/UX Design System & Layout Foundation Guide (Phase 2)

This document contains the complete style tokens, components catalog, theme mappings, and responsive layouts guide for the Transport ERP.

---

## 1. Color Palette Tokens
All colors are registered under Tailwind CSS v4 in [styles.css](file:///d:/Mohan%20Programs/transport-frontend/src/styles.css) inside `@theme`:

| Token | CSS Variable | HEX Value | Usage |
| :--- | :--- | :--- | :--- |
| **Primary (Base)** | `--color-primary-500` | `#3b82f6` | Default brand headers, links, and buttons. |
| **Primary (Hover)**| `--color-primary-600` | `#2563eb` | Hover states on interactive links. |
| **Success** | `--color-success-500` | `#10b981` | Active chips, positive action confirmation. |
| **Danger** | `--color-danger-500` | `#ef4444` | Deletion buttons, critical alerts. |
| **Warning**| `--color-warning-500` | `#f59e0b` | Expiry warning states, pending alerts. |
| **Info** | `--color-info-500` | `#06b6d4` | Details badges, metadata information tags. |
| **Background** | Custom | `#0b0f19` | Main layout shell background. |
| **Surface/Card** | Custom | `#1e293b` | Material cards background, table headers. |

---

## 2. Typography Rules
- **Font Family**: `'Outfit', 'Inter', system-ui, -apple-system, sans-serif`.
- **Heading Styles**: Font sizes range from `text-3xl` (dashboard headers) to `text-lg` (sidebar cards).
- **Monospace Font**: `font-mono` is mapped for license plates, chassis numbers, database codes, and system IDs.

---

## 3. Responsive Breakpoints
Our grids and flex panels scale dynamically across target viewports:
- **Mobile**: `w-full flex-col` mapping.
- **Tablet**: Collapses sidebar to thin layout (`w-20`) for optimal reading.
- **Laptop/Desktop**: Renders full expanded sidebar (`w-64`) with detailed descriptions.

---

## 4. Reusable Component Catalog
We created lightweight, standalone components in the `src/app/shared/` directory:

### A. Skeleton Loader Component
Renders animated skeleton blocks while loading listings data dynamically:
- **Tag**: `<app-skeleton-loader>`
- **Inputs**: `rows` (number of rows), `cols` (number of grid columns), `className` (extra class modifiers).

### B. Empty State Component
Displays visual alerts for empty collections, server offline, or invalid routes:
- **Tag**: `<app-empty-state>`
- **Inputs**: `icon` (Material icon code), `title` (state header), `message` (state details), `type` (`no-data` \| `error` \| `unauthorized`).

### C. Confirmation Dialog Component
Reusable modal mapping confirmation parameters:
- **Service Integration**: Resolved via `MatDialog.open()`.
- **Properties**: Takes title, message details, button context, and type (danger/warning/primary).
