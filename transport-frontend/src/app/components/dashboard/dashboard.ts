import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardService } from '../../services/dashboard.service';
import { AuthService } from '../../services/auth.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, MatCardModule, MatButtonModule, MatMenuModule],
    templateUrl: './dashboard.html',
    styles: [`
      :host {
        display: block;
        height: 100%;
        min-height: 0;
        overflow: hidden;
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

  notifications = signal<any[]>([]);
  activities = signal<any[]>([]);

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
    const role = this.activeRole();
    const done = () => this.loading.set(false);
    const apply = (res: any) => {
      if (res?.success) {
        this.metrics.set(res.data);
        this.notifications.set(Array.isArray(res.data?.alerts) ? res.data.alerts : []);
        this.activities.set(Array.isArray(res.data?.recentActivities) ? res.data.recentActivities : []);
      }
      done();
    };

    if (role === 'ADMIN') {
      this.dashboardService.getAdminMetrics().subscribe({ next: apply, error: done });
    } else if (role === 'OWNER') {
      this.dashboardService.getOwnerMetrics().subscribe({ next: apply, error: done });
    } else if (role === 'OPERATIONS') {
      this.dashboardService.getOperationsMetrics().subscribe({ next: apply, error: done });
    } else if (role === 'VEHICLE') {
      this.dashboardService.getVehicleMetrics().subscribe({ next: apply, error: done });
    } else if (role === 'ACCOUNTANT') {
      this.dashboardService.getAccountMetrics().subscribe({ next: apply, error: done });
    } else if (role === 'DRIVER') {
      this.dashboardService.getDriverMetrics().subscribe({ next: apply, error: done });
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
