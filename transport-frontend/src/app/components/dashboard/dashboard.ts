import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardService, MaintenanceDueDashboardItem, MaintenanceDueDashboardResponse } from '../../services/dashboard.service';
import { AuthService } from '../../services/auth.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { FfStatusBadgeComponent, FfStatusColor } from '@ff/ui';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, MatCardModule, MatButtonModule, MatMenuModule, FfStatusBadgeComponent],
    templateUrl: './dashboard.html',
    styles: [`
      :host {
        display: block;
        height: 100%;
        min-height: 0;
        overflow: hidden;
      }

      .dash-kpi {
        background-color: var(--ff-surface-card);
        border: 1px solid var(--ff-border-default);
        border-radius: 10px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        position: relative;
        overflow: hidden;
      }
      .dash-kpi--brand { border-left: 3px solid var(--ff-color-primary-600); }
      .dash-kpi--success { border-left: 3px solid var(--ff-color-success-500); }
      .dash-kpi--warning { border-left: 3px solid var(--ff-color-warning-500); }
      .dash-kpi--danger { border-left: 3px solid var(--ff-color-danger-500); }
      .dash-kpi--info { border-left: 3px solid var(--ff-color-info-500); }

      .dash-kpi-icon {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      .dash-kpi-icon--brand { background-color: var(--ff-color-primary-50); color: var(--ff-color-primary-600); }
      .dash-kpi-icon--success { background-color: var(--ff-color-success-50); color: var(--ff-color-success-500); }
      .dash-kpi-icon--warning { background-color: var(--ff-color-warning-50); color: var(--ff-color-warning-500); }
      .dash-kpi-icon--danger { background-color: var(--ff-color-danger-50); color: var(--ff-color-danger-500); }
      .dash-kpi-icon--info { background-color: var(--ff-color-info-50); color: var(--ff-color-info-500); }

      .dash-kpi-label {
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--ff-text-muted);
      }
      .dash-kpi-value {
        font-size: 28px;
        font-weight: 800;
        line-height: 1.15;
        font-variant-numeric: tabular-nums;
        color: var(--ff-text-primary);
      }
      .dash-kpi-sub {
        font-size: 10px;
        font-weight: 600;
        color: var(--ff-text-secondary);
      }
      .dash-kpi-pill {
        display: inline-flex;
        align-self: flex-start;
        font-size: 10px;
        font-weight: 700;
        padding: 0.125rem 0.5rem;
        border-radius: 9999px;
        background-color: var(--ff-color-danger-50);
        color: var(--ff-color-danger-500);
      }

      .dash-panel {
        background-color: var(--ff-surface-card);
        border: 1px solid var(--ff-border-default);
        border-radius: 10px;
        padding: 20px;
      }
      .dash-panel-title {
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--ff-text-muted);
      }

      .dash-action {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        height: 40px;
        padding: 0 12px 0 6px;
        border-radius: 9999px;
        border: 1px solid var(--ff-border-default);
        background-color: var(--ff-surface-card);
        color: var(--ff-text-primary);
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: background-color 120ms ease;
      }
      .dash-action:hover {
        background-color: var(--ff-surface-hover);
      }
      .dash-action-icon {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      .dash-action-icon--brand { background-color: var(--ff-color-primary-50); color: var(--ff-color-primary-600); }
      .dash-action-icon--success { background-color: var(--ff-color-success-50); color: var(--ff-color-success-500); }
      .dash-action-icon--warning { background-color: var(--ff-color-warning-50); color: var(--ff-color-warning-500); }
      .dash-action-icon--danger { background-color: var(--ff-color-danger-50); color: var(--ff-color-danger-500); }
      .dash-action-icon--info { background-color: var(--ff-color-info-50); color: var(--ff-color-info-500); }

      .dash-feed-row {
        display: flex;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 12px;
        border: 1px solid var(--ff-border-default);
        border-radius: 10px;
        background-color: var(--ff-surface-hover);
        border-left-width: 3px;
        border-left-style: solid;
      }
      .dash-feed-row--danger { border-left-color: var(--ff-color-danger-500); }
      .dash-feed-row--warning { border-left-color: var(--ff-color-warning-500); }
      .dash-feed-row--info { border-left-color: var(--ff-color-info-500); }
      .dash-feed-row--neutral { border-left-color: var(--ff-color-primary-600); }

      .dash-feed-chip {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      .dash-feed-chip--danger { background-color: var(--ff-color-danger-50); color: var(--ff-color-danger-500); }
      .dash-feed-chip--warning { background-color: var(--ff-color-warning-50); color: var(--ff-color-warning-500); }
      .dash-feed-chip--info { background-color: var(--ff-color-info-50); color: var(--ff-color-info-500); }
      .dash-feed-chip--brand { background-color: var(--ff-color-primary-50); color: var(--ff-color-primary-600); }
      .dash-feed-chip--success { background-color: var(--ff-color-success-50); color: var(--ff-color-success-500); }

      .dash-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 1.5rem 1rem;
        text-align: center;
        font-size: 12px;
        color: var(--ff-text-muted);
      }
    `]
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private router = inject(Router);

  activeRole = signal<string>('ADMIN');
  metrics = signal<any>(null);
  loading = signal<boolean>(false);
  loadError = signal<string | null>(null);

  notifications = signal<any[]>([]);
  activities = signal<any[]>([]);

  readonly maintenanceDueDashboard = computed<MaintenanceDueDashboardResponse | null>(() => {
    const data = this.metrics()?.maintenanceDueDashboard;
    return data ?? null;
  });

  trendValues = computed(() => {
    const m = this.metrics();
    const raw = m?.monthlyRevenueTrend || m?.revenueTrend || [];
    return (raw as any[]).map(v => Number(v) || 0);
  });

  trendLabels = computed(() => {
    const count = this.trendValues().length || 5;
    const labels: string[] = [];
    const now = new Date();
    for (let i = count - 1; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      labels.push(d.toLocaleString('en', { month: 'short' }));
    }
    return labels;
  });

  chartPath = computed(() => this.buildPolyline(this.trendValues(), 500, 150));
  chartAreaPath = computed(() => {
    const line = this.buildPolyline(this.trendValues(), 500, 150);
    if (!line) return '';
    return `${line} L 450,150 L 50,150 Z`;
  });
  chartDots = computed(() => {
    const values = this.trendValues();
    if (!values.length) return [] as { x: number; y: number }[];
    const max = Math.max(...values, 1);
    const pad = 10;
    const usableH = 150 - pad * 2;
    const step = values.length === 1 ? 0 : 400 / (values.length - 1);
    return values.map((v, i) => ({
      x: 50 + i * step,
      y: pad + usableH - (v / max) * usableH
    }));
  });

  private readonly quickActionRoutes: Record<string, string> = {
    'Add Vehicle': '/masters?tab=vehicle',
    'New Customer': '/masters?tab=customer',
    'Create Booking': '/bookings',
    'Create Trip': '/trips-planning',
    'Receive Payment': '/payment-logs',
    'Expense Entry': '/expense-logs',
    'Fuel Entry': '/fuel-logs',
    'Generate Report': '/reports-bi'
  };

  ngOnInit() {
    this.activeRole.set(this.resolveViewRole());
    this.fetchMetrics();
  }

  private resolveViewRole(): string {
    const roles = (this.authService.currentUser()?.roles
      || JSON.parse(localStorage.getItem('roles') || '[]')) as string[];
    const codes = roles.map(r => String(r).toUpperCase());
    if (codes.some(r => r.includes('DRIVER'))) return 'DRIVER';
    if (codes.some(r => r.includes('ACCOUNT'))) return 'ACCOUNTANT';
    if (codes.some(r => r.includes('VEHICLE') || r.includes('FLEET'))) return 'VEHICLE';
    if (codes.some(r => r.includes('OPERATION'))) return 'OPERATIONS';
    if (codes.some(r => r.includes('OWNER'))) return 'OWNER';
    return 'ADMIN';
  }

  fetchMetrics() {
    this.loading.set(true);
    this.loadError.set(null);
    const role = this.activeRole();
    const done = () => this.loading.set(false);
    const fail = () => {
      this.loadError.set('Unable to load dashboard telemetry.');
      done();
    };
    const apply = (res: any) => {
      if (res?.success) {
        this.metrics.set(res.data);
        this.notifications.set(Array.isArray(res.data?.alerts) ? res.data.alerts : []);
        this.activities.set(Array.isArray(res.data?.recentActivities) ? res.data.recentActivities : []);
      } else {
        this.loadError.set('Unable to load dashboard telemetry.');
      }
      done();
    };

    if (role === 'ADMIN') {
      this.dashboardService.getAdminMetrics().subscribe({ next: apply, error: fail });
    } else if (role === 'OWNER') {
      this.dashboardService.getOwnerMetrics().subscribe({ next: apply, error: fail });
    } else if (role === 'OPERATIONS') {
      this.dashboardService.getOperationsMetrics().subscribe({ next: apply, error: fail });
    } else if (role === 'VEHICLE') {
      this.dashboardService.getVehicleMetrics().subscribe({ next: apply, error: fail });
    } else if (role === 'ACCOUNTANT') {
      this.dashboardService.getAccountMetrics().subscribe({ next: apply, error: fail });
    } else if (role === 'DRIVER') {
      this.dashboardService.getDriverMetrics().subscribe({ next: apply, error: fail });
    } else {
      done();
    }
  }

  changeViewRole(role: string) {
    this.activeRole.set(role);
    this.fetchMetrics();
  }

  executeAction(action: string) {
    const target = this.quickActionRoutes[action] || '/dashboard';
    if (target.includes('?')) {
      const [path, query] = target.split('?');
      const params: Record<string, string> = {};
      new URLSearchParams(query).forEach((v, k) => { params[k] = v; });
      this.router.navigate([path], { queryParams: params });
      return;
    }
    this.router.navigate([target]);
  }

  openMaintenanceVehicle(item: MaintenanceDueDashboardItem) {
    if (item?.vehicleId == null) return;
    this.router.navigate(['/vehicles'], { queryParams: { vehicleId: item.vehicleId } });
  }

  dueStatusColor(status: string | null | undefined): FfStatusColor {
    switch (String(status || '').toUpperCase()) {
      case 'OVERDUE':
      case 'DUE':
        return 'danger';
      case 'DUE_SOON':
        return 'warning';
      case 'UNKNOWN':
        return 'info';
      default:
        return 'neutral';
    }
  }

  dueStatusLabel(status: string | null | undefined): string {
    switch (String(status || '').toUpperCase()) {
      case 'OVERDUE': return 'Overdue';
      case 'DUE': return 'Due';
      case 'DUE_SOON': return 'Due Soon';
      case 'UNKNOWN': return 'Unknown';
      default: return status || '—';
    }
  }

  vehicleLabel(item: MaintenanceDueDashboardItem): string {
    return [item.vehicleCode, item.vehicleName].filter(Boolean).join(' — ') || `Vehicle #${item.vehicleId}`;
  }

  nextDueLabel(item: MaintenanceDueDashboardItem): string {
    if (item.nextDueKm != null) {
      return `${item.nextDueKm} km`;
    }
    if (item.nextDueDate) {
      return String(item.nextDueDate);
    }
    return '—';
  }

  private buildPolyline(values: number[], width: number, height: number): string {
    if (!values.length) return '';
    const max = Math.max(...values, 1);
    const pad = 10;
    const usableH = height - pad * 2;
    const left = 50;
    const usableW = width - 100;
    const step = values.length === 1 ? 0 : usableW / (values.length - 1);
    const points = values.map((v, i) => {
      const x = left + i * step;
      const y = pad + usableH - (v / max) * usableH;
      return `${x} ${y}`;
    });
    return `M ${points.join(' L ')}`;
  }
}
