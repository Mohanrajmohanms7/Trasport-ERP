import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InventoryService, Warehouse } from '../../services/inventory.service';
import { MasterService } from '../../services/master.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-warehouse-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './warehouse-form.html'
})
export class WarehouseFormComponent implements OnInit {
  private inventory = inject(InventoryService);
  private masters = inject(MasterService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);

  id = signal<number | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  code = signal('');
  name = signal('');
  description = signal('');
  branchId = signal('');
  status = signal('ACTIVE');
  branches = signal<{ id: number; code: string; name: string }[]>([]);
  branchLocked = signal(false);

  ngOnInit() {
    const user = this.auth.currentUser();
    const roles = user?.roles || [];
    const isSuperAdmin = roles.includes('SUPER_ADMIN');
    if (!isSuperAdmin && user?.branchId) {
      this.branchId.set(String(user.branchId));
      this.branchLocked.set(true);
    }
    const rawId = this.route.snapshot.paramMap.get('id');
    if (rawId && rawId !== 'new') {
      this.id.set(Number(rawId));
      this.load(Number(rawId));
    }
    const companyId = user?.companyId || 0;
    this.masters.getMasters<{ id: number; code: string; name: string }>('branches', companyId, { size: 100 }).subscribe({
      next: res => this.branches.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.branches.set([])
    });
  }

  load(id: number) {
    this.loading.set(true);
    this.inventory.getWarehouse(id).subscribe({
      next: res => {
        const row = res?.data as Warehouse | undefined;
        if (res?.success && row) {
          this.code.set(row.code || '');
          this.name.set(row.name || '');
          this.description.set(row.description || '');
          this.branchId.set(row.branchId != null ? String(row.branchId) : '');
          this.status.set(row.status || 'ACTIVE');
        } else {
          this.error.set(res?.message || 'Unable to load warehouse.');
        }
        this.loading.set(false);
      },
      error: err => {
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
  }

  save() {
    this.error.set(null);
    if (!this.code().trim() || !this.name().trim() || !this.branchId()) {
      this.error.set('Code, name, and branch are required.');
      return;
    }
    const body = {
      code: this.code().trim(),
      name: this.name().trim(),
      description: this.description().trim() || null,
      branchId: Number(this.branchId()),
      status: this.status()
    };
    const request = this.id()
      ? this.inventory.updateWarehouse(this.id()!, body)
      : this.inventory.createWarehouse(body);
    request.subscribe({
      next: res => {
        if (res?.success) {
          this.router.navigate(['/inventory/warehouses']);
        } else {
          this.error.set(res?.message || 'Unable to save warehouse.');
        }
      },
      error: err => this.error.set(workOrderError(err))
    });
  }
}
