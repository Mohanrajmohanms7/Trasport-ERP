import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

interface ReportColumn { key: string; label: string; type: 'text' | 'date' | 'qty' | 'money' | 'number' | 'percent'; total: boolean; }
interface ReportDef { key: string; category: string; title: string; purpose: string; filters: string[]; columns: ReportColumn[]; finance: boolean; }
interface ReportResult { report: ReportDef; filters: Record<string, any>; columns: ReportColumn[]; rows: Record<string, any>[]; totals: Record<string, number>; rowCount: number; truncated: boolean; }
interface Option { id: number | string; label: string; }

const CATEGORY_ORDER = ['Monthly & management', 'Operations', 'Vehicles & trips', 'Bookings & delivery', 'Invoices & receipts', 'Outstanding & pending',
  'Drivers & settlement', 'Fuel & expenses', 'Maintenance & work orders', 'Warehouse & stock', 'Financial & accounting'];
const CATEGORY_ICON: Record<string, string> = {
  'Monthly & management': 'insights', 'Operations': 'local_shipping', 'Vehicles & trips': 'directions_car', 'Bookings & delivery': 'assignment',
  'Invoices & receipts': 'receipt_long', 'Outstanding & pending': 'pending_actions', 'Drivers & settlement': 'badge', 'Fuel & expenses': 'local_gas_station',
  'Maintenance & work orders': 'build', 'Warehouse & stock': 'inventory_2', 'Financial & accounting': 'account_balance'
};
/** Status choices per report (what the underlying record uses). */
const STATUS_OPTIONS: Record<string, string[]> = {
  'trip-register': ['PLANNED', 'DISPATCHED', 'COMPLETED', 'CANCELLED'],
  'booking-register': ['PENDING', 'APPROVED', 'COMPLETED', 'REJECTED'],
  'driver-payroll-register': ['DRAFT', 'APPROVED', 'POSTED', 'PAID', 'CANCELLED'],
  'driver-advances': ['ISSUED', 'CANCELLED'],
  'fuel-register': ['DRAFT', 'APPROVED', 'CANCELLED'],
  'expense-register': ['SUBMITTED', 'APPROVED', 'PAID', 'REJECTED'],
  'work-order-register': ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
  'maintenance-request-register': ['OPEN', 'UNDER_REVIEW', 'APPROVED', 'CONVERTED', 'CANCELLED'],
  'sales-register': ['UNPAID', 'PARTIALLY_PAID', 'PAID'],
  'receipt-register': ['DRAFT', 'APPROVED', 'CANCELLED']
};
const ACCOUNTS: Option[] = [
  { id: '1000', label: '1000 Cash on Hand' }, { id: '1010', label: '1010 Bank' }, { id: '1100', label: '1100 Customer Receivables' },
  { id: '1150', label: '1150 Driver Advances' }, { id: '1200', label: '1200 Spare Parts Inventory' }, { id: '2000', label: '2000 Accounts Payable' },
  { id: '2050', label: '2050 Driver Salary Payable' }, { id: '2200', label: '2200 GST Liability' }, { id: '4000', label: '4000 Freight Income' },
  { id: '5100', label: '5100 Fuel Expense' }, { id: '5150', label: '5150 Driver Salary Expense' }, { id: '5400', label: '5400 Repair & Maintenance' }
];
const EXPENSE_CATEGORIES = ['TOLL', 'DRIVER_BATA', 'PARKING', 'VEHICLE_REPAIR', 'INSURANCE', 'OFFICE'];

@Component({
  selector: 'app-reports-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reports-hub.html',
  styles: [`
    .rh-grid { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 12px; height: 100%; min-height: 0; }
    @media (max-width: 1023px) { .rh-grid { grid-template-columns: 1fr; } .rh-nav { display: none; } }
    .rh-card { background: var(--ff-surface-card); border: 1px solid var(--ff-border-default); border-radius: 12px; }
    .rh-cat { font-size: 12px; font-weight: 600; color: var(--ff-text-muted); padding: 10px 12px 4px; display: flex; gap: 6px; align-items: center; }
    .rh-item { display: block; width: 100%; text-align: left; padding: 7px 12px 7px 32px; font-size: 13px; border-left: 3px solid transparent; color: var(--ff-text-secondary); }
    .rh-item:hover { background: var(--ff-surface-hover); }
    .rh-item.active { border-left-color: var(--ff-accent-amber, #f5a524); background: var(--ff-surface-hover); color: var(--ff-text-primary); font-weight: 600; }
    .rh-field { display: flex; flex-direction: column; gap: 4px; font-size: 11px; font-weight: 600; color: var(--ff-text-muted); min-width: 0; }
    .rh-input { height: 38px; border: 1px solid var(--ff-border-default); border-radius: 8px; padding: 0 10px; font-size: 13px; background: var(--ff-surface-card); color: var(--ff-text-primary); min-width: 0; }
    .rh-btn { height: 38px; padding: 0 14px; border-radius: 8px; font-size: 13px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
    .rh-btn-primary { background: var(--ff-color-primary-600); color: #fff; }
    .rh-btn-ghost { border: 1px solid var(--ff-border-default); background: var(--ff-surface-card); color: var(--ff-text-secondary); }
    .rh-chip { height: 28px; padding: 0 10px; border-radius: 999px; border: 1px solid var(--ff-border-default); font-size: 12px; background: var(--ff-surface-card); }
    .rh-chip.on { background: #172231; color: #fff; border-color: #172231; }
    table.rh-table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
    .rh-table th { position: sticky; top: 0; background: var(--ff-surface-hover); text-align: left; font-weight: 600; font-size: 11.5px; color: var(--ff-text-muted); padding: 8px 10px; white-space: nowrap; z-index: 1; }
    .rh-table td { padding: 7px 10px; border-top: 1px solid var(--ff-border-divider); white-space: nowrap; }
    .rh-table .num { text-align: right; font-variant-numeric: tabular-nums; }
    .rh-table tfoot td { position: sticky; bottom: 0; background: #172231; color: #fff; font-weight: 700; }
    .rh-kpi { padding: 14px; border-radius: 12px; border: 1px solid var(--ff-border-default); background: var(--ff-surface-card); }
  `]
})
export class ReportsHubComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private companyId = resolveTenantCompanyId();

  catalog = signal<ReportDef[]>([]);
  search = signal('');
  current = signal<ReportDef | null>(null);
  result = signal<ReportResult | null>(null);
  loading = signal(false);
  exporting = signal<'xlsx' | 'pdf' | null>(null);

  f: Record<string, any> = {};
  branches = signal<Option[]>([]);
  vehicles = signal<Option[]>([]);
  drivers = signal<Option[]>([]);
  customers = signal<Option[]>([]);
  warehouses = signal<Option[]>([]);
  parts = signal<Option[]>([]);
  readonly accounts = ACCOUNTS;
  readonly expenseCategories = EXPENSE_CATEGORIES;

  readonly groups = computed(() => {
    const q = this.search().toLowerCase().trim();
    const list = this.catalog().filter(r => !q || r.title.toLowerCase().includes(q) || r.purpose.toLowerCase().includes(q));
    const cats = [...new Set(list.map(r => r.category))].sort((a, b) => CATEGORY_ORDER.indexOf(a) - CATEGORY_ORDER.indexOf(b));
    return cats.map(c => ({ category: c, icon: CATEGORY_ICON[c] || 'description', reports: list.filter(r => r.category === c) }));
  });
  readonly statusOptions = computed(() => STATUS_OPTIONS[this.current()?.key || ''] || []);
  readonly isKpi = computed(() => this.current()?.key === 'management-kpis');

  ngOnInit(): void {
    this.resetDates('month');
    this.http.get<any>('/api/v1/report-hub/catalog').subscribe({
      next: r => {
        this.catalog.set(r?.data ?? []);
        const wanted = this.route.snapshot.queryParamMap.get('r');
        const first = this.catalog().find(x => x.key === wanted) || this.catalog().find(x => x.key === 'management-kpis') || this.catalog()[0];
        if (first) this.select(first);
      },
      error: () => this.notify.error('Could not load the report list')
    });
    this.loadOptions();
  }

  private loadOptions(): void {
    const p = new HttpParams().set('companyId', String(this.companyId)).set('size', '500');
    const map = (res: any, label: (x: any) => string) => ((res?.data?.content ?? res?.data ?? []) as any[]).map(x => ({ id: x.id, label: label(x) }));
    this.http.get<any>('/api/v1/branches', { params: p }).subscribe({ next: r => this.branches.set(map(r, x => x.name)), error: () => {} });
    this.http.get<any>('/api/v1/vehicles', { params: p }).subscribe({ next: r => this.vehicles.set(map(r, x => x.name || x.code)), error: () => {} });
    this.http.get<any>('/api/v1/drivers', { params: p }).subscribe({ next: r => this.drivers.set(map(r, x => [x.code, x.name].filter(Boolean).join(' — '))), error: () => {} });
    this.http.get<any>('/api/v1/customers', { params: p }).subscribe({ next: r => this.customers.set(map(r, x => x.name)), error: () => {} });
    this.http.get<any>('/api/v1/warehouses', { params: p }).subscribe({ next: r => this.warehouses.set(map(r, x => x.name)), error: () => {} });
    this.http.get<any>('/api/v1/spare-parts', { params: p }).subscribe({ next: r => this.parts.set(map(r, x => [x.code, x.name].filter(Boolean).join(' — '))), error: () => {} });
  }

  has(filter: string): boolean {
    return !!this.current()?.filters?.includes(filter);
  }

  select(r: ReportDef): void {
    this.current.set(r);
    this.result.set(null);
    this.f['status'] = '';
    this.f['category'] = '';
    this.router.navigate([], { queryParams: { r: r.key }, replaceUrl: true });
    this.run();
  }

  selectByKey(key: string): void {
    const r = this.catalog().find(x => x.key === key);
    if (r) this.select(r);
  }

  private iso(d: Date): string {
    const z = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
    return z.toISOString().slice(0, 10);
  }

  preset = signal('month');
  resetDates(kind: string): void {
    const now = new Date();
    let from = new Date(now.getFullYear(), now.getMonth(), 1);
    let to = now;
    if (kind === 'today') from = now;
    if (kind === 'last-month') { from = new Date(now.getFullYear(), now.getMonth() - 1, 1); to = new Date(now.getFullYear(), now.getMonth(), 0); }
    if (kind === 'quarter') from = new Date(now.getFullYear(), now.getMonth() - 2, 1);
    if (kind === 'fy') { const y = now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1; from = new Date(y, 3, 1); }
    this.f['from'] = this.iso(from);
    this.f['to'] = this.iso(to);
    this.preset.set(kind);
  }

  applyPreset(kind: string): void {
    this.resetDates(kind);
    this.run();
  }

  private params(): HttpParams {
    let p = new HttpParams();
    Object.entries(this.f).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
    });
    return p;
  }

  run(): void {
    const r = this.current();
    if (!r) return;
    this.loading.set(true);
    this.http.get<any>(`/api/v1/report-hub/run/${r.key}`, { params: this.params() }).subscribe({
      next: res => { this.result.set(res?.data ?? null); this.loading.set(false); },
      error: e => {
        this.loading.set(false);
        const errs = e?.error?.errors;
        this.notify.error((Array.isArray(errs) && errs[0]) || e?.error?.message || 'Report failed');
      }
    });
  }

  clearFilters(): void {
    ['branchId', 'vehicleId', 'driverId', 'customerId', 'warehouseId', 'sparePartId', 'status', 'category', 'accountCode', 'days'].forEach(k => this.f[k] = '');
    this.run();
  }

  export(format: 'xlsx' | 'pdf'): void {
    const r = this.current();
    if (!r) return;
    this.exporting.set(format);
    this.http.get(`/api/v1/report-hub/export/${r.key}`, { params: this.params().set('format', format), responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${r.key}_${this.f['from'] || ''}_${this.f['to'] || ''}.${format}`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.exporting.set(null);
      },
      error: () => { this.exporting.set(null); this.notify.error('Export failed'); }
    });
  }

  fmt(v: any, c: ReportColumn): string {
    if (v === null || v === undefined || v === '') return '—';
    switch (c.type) {
      case 'money': return '₹' + Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      case 'qty': return Number(v).toLocaleString('en-IN', { maximumFractionDigits: 3 });
      case 'number': return Number(v).toLocaleString('en-IN', { maximumFractionDigits: 2 });
      case 'percent': return Number(v).toLocaleString('en-IN', { maximumFractionDigits: 2 }) + '%';
      case 'date': { const d = new Date(String(v).slice(0, 10)); return isNaN(d.getTime()) ? String(v) : d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }); }
      default: return String(v);
    }
  }

  isNum(c: ReportColumn): boolean {
    return c.type !== 'text' && c.type !== 'date';
  }

  kpiValue(row: Record<string, any>): string {
    const v = row['value'];
    if (v === null || v === undefined) return '—';
    const u = row['unit'];
    const n = Number(v).toLocaleString('en-IN', { maximumFractionDigits: 2 });
    return u === '₹' ? '₹' + n : u === '%' ? n + '%' : n + ' ' + u;
  }
}
