import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { WorkOrder, WorkOrderService, workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-work-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './work-order-list.html'
})
export class WorkOrderListComponent implements OnInit {
  private workOrders = inject(WorkOrderService);
  private router = inject(Router);
  private auth = inject(AuthService);

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<WorkOrder[]>([]);
  vehicleId = signal('');
  status = signal('');
  source = signal('');
  maintenanceType = signal('');
  branchId = signal('');

  readonly canWrite = signal(false);

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canWrite.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    const vehicle = this.vehicleId().trim();
    const branch = this.branchId().trim();
    this.workOrders.list({
      vehicleId: vehicle ? Number(vehicle) : null,
      status: this.status() || null,
      source: this.source() || null,
      maintenanceType: this.maintenanceType().trim() || null,
      branchId: branch ? Number(branch) : null,
      page: 0,
      size: 50
    }).subscribe({
      next: res => {
        this.rows.set(res?.success ? (res.data?.content || []) : []);
        if (!res?.success) {
          this.error.set(res?.message || 'Unable to load work orders.');
        }
        this.loading.set(false);
      },
      error: err => {
        this.rows.set([]);
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
  }

  open(row: WorkOrder) {
    this.router.navigate(['/work-orders', row.id]);
  }

  vehicleLabel(row: WorkOrder): string {
    return [row.vehicleCode, row.vehicleName].filter(Boolean).join(' — ') || String(row.vehicleId);
  }
}
