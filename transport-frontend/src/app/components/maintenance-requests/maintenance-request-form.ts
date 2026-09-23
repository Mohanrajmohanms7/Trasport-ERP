import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MasterService } from '../../services/master.service';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import {
  AuthorizedVehicle,
  MaintenanceRequestService,
  maintenanceRequestError
} from '../../services/maintenance-request.service';

@Component({
  selector: 'app-maintenance-request-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './maintenance-request-form.html'
})
export class MaintenanceRequestFormComponent implements OnInit {
  private requests = inject(MaintenanceRequestService);
  private masters = inject(MasterService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);
  vehicles = signal<AuthorizedVehicle[]>([]);
  driverOnly = signal(false);
  requestId = signal<number | null>(null);
  vehicleId = signal<number | null>(null);
  title = signal('');
  description = signal('');
  priority = signal('MEDIUM');

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    const admin = roles.some(role => role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER');
    this.driverOnly.set(roles.includes('DRIVER') && !admin);
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isFinite(id) && id > 0) {
      this.requestId.set(id);
      this.loadExisting(id);
    }
    this.loadVehicles();
  }

  loadVehicles() {
    if (this.driverOnly()) {
      this.requests.authorizedVehicles().subscribe({
        next: res => this.vehicles.set(res?.success ? (res.data || []) : []),
        error: err => this.error.set(maintenanceRequestError(err))
      });
      return;
    }
    const companyId = resolveTenantCompanyId();
    this.masters.getMasters<any>('vehicles', companyId, { page: 0, size: 100 }).subscribe({
      next: res => {
        const list = res?.success ? (res.data?.content || res.data || []) : [];
        this.vehicles.set(list.map((row: any) => ({
          id: row.id,
          code: row.code,
          name: row.name,
          companyId: row.companyId,
          branchId: row.branchId
        })));
      },
      error: err => this.error.set(maintenanceRequestError(err))
    });
  }

  loadExisting(id: number) {
    this.requests.get(id).subscribe({
      next: res => {
        if (!res?.success || !res.data) {
          this.error.set(res?.message || 'Unable to load the request.');
          return;
        }
        this.vehicleId.set(res.data.vehicleId);
        this.title.set(res.data.title || '');
        this.description.set(res.data.description || '');
        this.priority.set(res.data.priority || 'MEDIUM');
      },
      error: err => this.error.set(maintenanceRequestError(err))
    });
  }

  save() {
    if (this.vehicleId() == null || !this.title().trim() || !this.description().trim()) {
      this.error.set('Vehicle, title, and description are required.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const body = {
      vehicleId: this.vehicleId() as number,
      title: this.title().trim(),
      description: this.description().trim(),
      priority: this.priority()
    };
    const id = this.requestId();
    const call = id == null ? this.requests.create(body) : this.requests.update(id, body);
    call.subscribe({
      next: res => {
        this.loading.set(false);
        if (res?.success && res.data?.id) {
          this.router.navigate(['/maintenance-requests', res.data.id]);
          return;
        }
        this.error.set(res?.message || 'Unable to save the request.');
      },
      error: err => {
        this.loading.set(false);
        this.error.set(maintenanceRequestError(err));
      }
    });
  }
}
