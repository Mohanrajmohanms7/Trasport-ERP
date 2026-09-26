import { MasterService } from '../../services/master.service';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MaintenanceRequest, MaintenanceRequestService, maintenanceRequestError } from '../../services/maintenance-request.service';

@Component({
  selector: 'app-maintenance-request-list',
  standalone: true,
  imports: [ExportButtonsComponent, CommonModule, FormsModule, RouterLink],
  templateUrl: './maintenance-request-list.html'
})
export class MaintenanceRequestListComponent implements OnInit {
  vehicles = signal<any[]>([]);
  private masters = inject(MasterService);

  private requests = inject(MaintenanceRequestService);
  private router = inject(Router);
  private auth = inject(AuthService);

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<MaintenanceRequest[]>([]);
  vehicleId = signal('');
  status = signal('');
  priority = signal('');
  fromDate = signal('');
  toDate = signal('');
  readonly canCreate = signal(false);

  ngOnInit() {
    this.masters.getMasters<any>('vehicles', resolveTenantCompanyId(), { size: 500 }).subscribe({ next: res => this.vehicles.set(res?.success ? ((res.data as any)?.content || res.data || []) : []), error: () => this.vehicles.set([]) });
    const roles = this.auth.currentUser()?.roles || [];
    this.canCreate.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER' || role === 'DRIVER'));
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    const vehicle = this.vehicleId().trim();
    this.requests.list({
      vehicleId: vehicle ? Number(vehicle) : null,
      status: this.status() || null,
      priority: this.priority() || null,
      fromDate: this.fromDate() || null,
      toDate: this.toDate() || null,
      page: 0,
      size: 50
    }).subscribe({
      next: res => {
        this.rows.set(res?.success ? (res.data?.content || []) : []);
        if (!res?.success) this.error.set(res?.message || 'Unable to load maintenance requests.');
        this.loading.set(false);
      },
      error: err => {
        this.rows.set([]);
        this.error.set(maintenanceRequestError(err));
        this.loading.set(false);
      }
    });
  }

  open(row: MaintenanceRequest) {
    this.router.navigate(['/maintenance-requests', row.id]);
  }

  vehicleLabel(row: MaintenanceRequest): string {
    return [row.vehicleRegistrationNumber || row.vehicleCode, row.vehicleName].filter(Boolean).join(' — ') || String(row.vehicleId);
  }
}
