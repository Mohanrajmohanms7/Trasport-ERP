import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InventoryService, Warehouse, WarehouseStock } from '../../services/inventory.service';
import { SparePart, SparePartService } from '../../services/spare-part.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stock-list.html'
})
export class StockListComponent implements OnInit {
  private inventory = inject(InventoryService);
  private spareParts = inject(SparePartService);
  private router = inject(Router);
  private auth = inject(AuthService);

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<WarehouseStock[]>([]);
  warehouses = signal<Warehouse[]>([]);
  parts = signal<SparePart[]>([]);
  warehouseId = signal('');
  sparePartId = signal('');
  code = signal('');
  canWrite = signal(false);

  ngOnInit() {
    const roles = this.auth.currentUser()?.roles || [];
    this.canWrite.set(roles.some(role =>
      role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'BRANCH_MANAGER'));
    this.inventory.listWarehouses({ status: 'ACTIVE', page: 0, size: 100 }).subscribe({
      next: res => this.warehouses.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.warehouses.set([])
    });
    this.spareParts.list().subscribe({
      next: res => this.parts.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.parts.set([])
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.inventory.listStock({
      warehouseId: this.warehouseId() ? Number(this.warehouseId()) : null,
      sparePartId: this.sparePartId() ? Number(this.sparePartId()) : null,
      code: this.code().trim() || null,
      page: 0,
      size: 50
    }).subscribe({
      next: res => {
        this.rows.set(res?.success && res.data ? res.data.content || [] : []);
        if (!res?.success) this.error.set(res?.message || 'Unable to load stock.');
        this.loading.set(false);
      },
      error: err => {
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
  }

  opening() {
    this.router.navigate(['/inventory/stock/opening-balance']);
  }
}
