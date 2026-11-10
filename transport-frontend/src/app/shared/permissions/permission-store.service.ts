import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type PermAction = 'view' | 'add' | 'edit' | 'delete' | 'approve';

/**
 * The signed-in user's role permissions (GET /api/v1/role-permissions/me). Used only to HIDE menus / buttons —
 * the server enforces the same rights. Until loaded (or on error) everything shows, so nobody loses a screen by accident.
 */
@Injectable({ providedIn: 'root' })
export class PermissionStore {
  private http = inject(HttpClient);
  readonly rights = signal<Record<string, Record<string, boolean>> | null>(null);

  /** Screen route prefix -> permission module (mirrors RolePermissionService.MODULES on the server). */
  private static readonly ROUTES: [string, string][] = [
    ['/dashboard', 'dashboard'], ['/customers', 'masters'], ['/materials-quarries', 'masters'], ['/suppliers', 'masters'],
    ['/masters', 'masters'], ['/vehicles', 'vehicles'], ['/drivers', 'drivers'], ['/bookings', 'bookings'],
    ['/trips-planning', 'trips'], ['/dispatch-loads', 'trips'], ['/fuel-logs', 'fuel'], ['/expense-logs', 'expenses'],
    ['/maintenance-requests', 'maintenance'], ['/work-orders', 'maintenance'], ['/spare-parts', 'maintenance'], ['/inventory', 'maintenance'],
    ['/billing-invoices', 'invoices'], ['/payment-logs', 'receipts'], ['/payables', 'payables'], ['/accounts-ledger', 'accounts'],
    ['/driver-payroll', 'payroll'], ['/reports', 'reports'], ['/users-roles', 'settings'], ['/branches', 'settings'],
    ['/company-admin', 'settings'], ['/lookup-values', 'settings']
  ];

  load(): void {
    this.http.get<any>('/api/v1/role-permissions/me').subscribe({
      next: r => this.rights.set(r?.data || null),
      error: () => this.rights.set(null)
    });
  }

  can(module: string, action: PermAction = 'view'): boolean {
    const r = this.rights();
    if (!r || !r[module]) return true;
    return r[module][action] !== false;
  }

  /** "invoices:approve" → can('invoices', 'approve'). */
  canKey(key: string): boolean {
    const [m, a] = (key || '').split(':');
    return !m || this.can(m, ((a || 'view') as PermAction));
  }

  canRoute(route: string): boolean {
    const hit = PermissionStore.ROUTES.filter(([p]) => route === p || route.startsWith(p + '/') || route.startsWith(p + '?'))
      .sort((a, b) => b[0].length - a[0].length)[0];
    return !hit || this.can(hit[1], 'view');
  }
}
