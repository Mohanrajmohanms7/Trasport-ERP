import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InventoryService, Warehouse } from '../../services/inventory.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './warehouse-list.html'
})
export class WarehouseListComponent implements OnInit {
  private inventory = inject(InventoryService);
  private router = inject(Router);
  private auth = inject(AuthService);

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<Warehouse[]>([]);
  status = signal('');
  code = signal('');
  canWrite = signal(false);

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canWrite.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.inventory.listWarehouses({
      status: this.status() || null,
      code: this.code().trim() || null,
      page: 0,
      size: 50
    }).subscribe({
      next: res => {
        this.rows.set(res?.success && res.data ? res.data.content || [] : []);
        if (!res?.success) this.error.set(res?.message || 'Unable to load warehouses.');
        this.loading.set(false);
      },
      error: err => {
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
  }

  create() {
    this.router.navigate(['/inventory/warehouses/new']);
  }

  open(row: Warehouse) {
    this.router.navigate(['/inventory/warehouses', row.id]);
  }
}
