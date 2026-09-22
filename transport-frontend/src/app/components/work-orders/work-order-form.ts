import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MasterService } from '../../services/master.service';
import { WorkOrderService, workOrderError } from '../../services/work-order.service';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-work-order-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './work-order-form.html'
})
export class WorkOrderFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private masters = inject(MasterService);
  private workOrders = inject(WorkOrderService);

  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  lockedPreventive = signal(false);

  vehicles = signal<any[]>([]);
  rules = signal<any[]>([]);
  types = signal<any[]>([]);
  suppliers = signal<any[]>([]);
  users = signal<any[]>([]);

  vehicleId = signal<number | null>(null);
  source = signal('MANUAL');
  maintenanceRuleId = signal<number | null>(null);
  maintenanceType = signal('');
  name = signal('');
  description = signal('');
  priority = signal('NORMAL');
  supplierId = signal<number | null>(null);
  assignedUserId = signal<number | null>(null);
  estimatedCost = signal('');
  requestedDate = signal('');
  attachmentPath = signal<string | null>(null);

  ngOnInit() {
    const companyId = resolveTenantCompanyId();
    const params = this.route.snapshot.queryParamMap;
    const source = (params.get('source') || '').toUpperCase();
    const vehicle = Number(params.get('vehicleId'));
    const rule = Number(params.get('ruleId'));
    if (source === 'PREVENTIVE' && Number.isFinite(vehicle) && Number.isFinite(rule) && rule > 0) {
      this.lockedPreventive.set(true);
      this.source.set('PREVENTIVE');
      this.vehicleId.set(vehicle);
      this.maintenanceRuleId.set(rule);
    } else if (Number.isFinite(vehicle) && vehicle > 0) {
      this.vehicleId.set(vehicle);
    }

    let pending = 4;
    const done = () => {
      pending -= 1;
      if (pending <= 0) this.loading.set(false);
    };
    this.masters.getMasters<any>('vehicles', companyId, { page: 0, size: 100 }).subscribe({
      next: res => { this.vehicles.set(res?.data?.content || []); done(); },
      error: () => { this.error.set('Unable to load vehicles.'); done(); }
    });
    this.http.get<any>('/api/v1/maintenance/rules', { params: { page: 0, size: 100 } }).subscribe({
      next: res => { this.rules.set(res?.data?.content || []); done(); },
      error: () => done()
    });
    this.masters.getLookupList(companyId, 'MAINTENANCE_TYPE').subscribe({
      next: res => { this.types.set(res?.data || []); done(); },
      error: () => done()
    });
    this.masters.getMasters<any>('suppliers', companyId, { page: 0, size: 100 }).subscribe({
      next: res => { this.suppliers.set(res?.data?.content || []); done(); },
      error: () => done()
    });
    this.masters.getMasters<any>('users', companyId, { page: 0, size: 100 }).subscribe({
      next: res => this.users.set(res?.data?.content || []),
      error: () => undefined
    });
  }

  onFile(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.error.set(null);
    this.workOrders.upload(file).subscribe({
      next: res => {
        if (res?.success && res.data?.fileName) {
          this.attachmentPath.set(res.data.fileName);
          this.success.set('Attachment uploaded.');
        } else {
          this.error.set(res?.message || 'Upload failed.');
        }
      },
      error: err => this.error.set(workOrderError(err))
    });
  }

  submit() {
    this.error.set(null);
    this.success.set(null);
    if (this.vehicleId() == null) {
      this.error.set('Select a vehicle.');
      return;
    }
    if (!this.name().trim()) {
      this.error.set('Enter a work order title.');
      return;
    }
    if (this.source() === 'PREVENTIVE' && this.maintenanceRuleId() == null) {
      this.error.set('Select a maintenance rule for a preventive work order.');
      return;
    }
    if (this.source() === 'MANUAL' && !this.maintenanceType()) {
      this.error.set('Select a maintenance type for a manual work order.');
      return;
    }
    const cost = this.estimatedCost().trim();
    if (cost && Number(cost) < 0) {
      this.error.set('Estimated cost cannot be negative.');
      return;
    }
    const body: Record<string, unknown> = {
      vehicleId: this.vehicleId(),
      source: this.source(),
      name: this.name().trim(),
      description: this.description().trim() || null,
      priority: this.priority(),
      supplierId: this.supplierId() || null,
      assignedUserId: this.assignedUserId() || null,
      estimatedCost: cost ? Number(cost) : null,
      requestedDate: this.requestedDate() || null,
      attachmentPath: this.attachmentPath()
    };
    if (this.source() === 'PREVENTIVE') {
      body['maintenanceRuleId'] = this.maintenanceRuleId();
    } else {
      body['maintenanceType'] = this.maintenanceType();
    }
    this.saving.set(true);
    this.workOrders.create(body).subscribe({
      next: res => {
        this.saving.set(false);
        if (res?.success && res.data?.id) {
          this.success.set('Work order ' + res.data.workOrderNumber + ' created.');
          this.router.navigate(['/work-orders', res.data.id]);
        } else {
          this.error.set(res?.message || 'Unable to create the work order.');
        }
      },
      error: err => {
        this.saving.set(false);
        this.error.set(workOrderError(err));
      }
    });
  }
}
