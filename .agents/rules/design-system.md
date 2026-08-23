# FleetFlow ERP - Design System Rule

This document defines the visual design system for FleetFlow ERP, extracted from the reference designs. All pages, layout shells, components, and forms must adhere to these tokens.

## 1. Color Palette

### Base System Colors
*   **Page Background**: `#f8fafc` (Tailwind `bg-slate-50`)
*   **Card Background**: `#ffffff` (Tailwind `bg-white`)
*   **Border Color**: `#e2e8f0` (Tailwind `border-slate-200`)
*   **Divider Color**: `#f1f5f9` (Tailwind `divide-slate-100`)

### Sidebar & Navigation
*   **Sidebar Background**: `#ffffff` (Tailwind `bg-white`)
*   **Sidebar Border**: `#e2e8f0` (Tailwind `border-r border-slate-200`)
*   **Sidebar Group Header Text**: `#64748b` (Tailwind `text-slate-500`), 11px uppercase font-semibold
*   **Nav Item Text (Default)**: `#475569` (Tailwind `text-slate-600`)
*   **Nav Item Text (Hover)**: `#0f172a` (Tailwind `text-slate-900`)
*   **Nav Item Background (Hover)**: `#f1f5f9` (Tailwind `bg-slate-100`)
*   **Nav Item (Active)**: Background `#2563eb` (Tailwind `bg-blue-600`), Text `#ffffff` (Tailwind `text-white`)

### Accent / Interactive
*   **Primary Accent (Buttons/Links)**: `#2563eb` (Tailwind `bg-blue-600`), hover `#1d4ed8` (Tailwind `bg-blue-700`)
*   **Primary Accent Text**: `#ffffff`
*   **Secondary Actions**: Background `#ffffff`, Text `#334155` (Tailwind `text-slate-700`), Border `#cbd5e1` (Tailwind `border-slate-300`)

### Status Pills
*   **Active / Success**: Background `#ecfdf5`, Text `#047857`, Dot `#10b981` (Green)
*   **On Trip / Info**: Background `#eff6ff`, Text `#1d4ed8`, Dot `#3b82f6` (Blue)
*   **On Leave / Warning**: Background `#fffbeb`, Text `#b45309`, Dot `#f59e0b` (Amber)
*   **Inactive / Danger**: Background `#fff1f2`, Text `#be123c`, Dot `#f43f5e` (Red)

---

## 2. Typography

*   **Font Family**: `Inter`, system-ui, -apple-system, sans-serif
*   **Page Title**: `text-2xl font-bold text-slate-900`
*   **Page Subtitle**: `text-sm text-slate-500`
*   **Card / Section Header**: `text-base font-semibold text-slate-900`
*   **Form Labels**: `text-xs font-semibold text-slate-600 uppercase tracking-wider`
*   **Form Values / Input Text**: `text-sm text-slate-900`

---

## 3. Spacing & Borders

*   **Card Radius**: `rounded-xl` (12px) or `rounded-lg` (8px) for inputs
*   **Inputs Padding**: `px-3 py-2`
*   **Table Row Height**: `py-3 px-4`
*   **Shadows**: Flat, subtle shadows (`shadow-sm` or `shadow-subtle` -> `box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05)`)

---

## 4. Components Layout

### Stat Cards (KPI)
*   Container: White card with subtle border.
*   Left: Circle background (subtle primary color fill) containing outline icon.
*   Right: Stacked elements:
    *   Label (small, grey, uppercase)
    *   Value (large bold text)
    *   Sub-label (percentage or status summary)

### Form Sections
*   Section title header should have an outline icon, colored matching the section category.
*   Fields arranged in a two-column grid.
*   Input fields must have an outline icon as a leading element.
