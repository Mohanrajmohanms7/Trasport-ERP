import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InventoryService, Warehouse, WarehouseStock } from '../../services/inventory.service';
import { SparePart, SparePartService } from '../../services/spare-part.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-opening-balance-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './opening-balance-form.html'
})
export class OpeningBalanceFormComponent implements OnInit {
  private inventory = inject(InventoryService);
  private spareParts = inject(SparePartService);
  private router = inject(Router);

  warehouses = signal<Warehouse[]>([]);
  parts = signal<SparePart[]>([]);
  warehouseId = signal('');
  sparePartId = signal('');
  quantity = signal('');
  description = signal('');
  currentQuantity = signal<number | null>(null);
  error = signal<string | null>(null);
  loadingCurrent = signal(false);

  ngOnInit() {
    this.inventory.listWarehouses({ status: 'ACTIVE', page: 0, size: 100 }).subscribe({
      next: res => this.warehouses.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.warehouses.set([])
    });
    this.spareParts.list().subscribe({
      next: res => this.parts.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.parts.set([])
    });
  }

  refreshCurrent() {
    this.currentQuantity.set(null);
    if (!this.warehouseId() || !this.sparePartId()) {
      return;
    }
    this.loadingCurrent.set(true);
    this.inventory.listStock({
      warehouseId: Number(this.warehouseId()),
      sparePartId: Number(this.sparePartId()),
      page: 0,
      size: 1
    }).subscribe({
      next: res => {
        const row: WarehouseStock | undefined = res?.data?.content?.[0];
        this.currentQuantity.set(row?.availableQuantity != null ? Number(row.availableQuantity) : 0);
        this.loadingCurrent.set(false);
      },
      error: () => {
        this.currentQuantity.set(null);
        this.loadingCurrent.set(false);
      }
    });
  }

  previewNew(): number | null {
    const current = this.currentQuantity();
    const opening = Number(this.quantity());
    if (current == null || !Number.isFinite(opening)) {
      return null;
    }
    return current + opening;
  }

  save() {
    this.error.set(null);
    if (!this.warehouseId() || !this.sparePartId() || !this.quantity().trim()) {
      this.error.set('Warehouse, spare part, and quantity are required.');
      return;
    }
    this.inventory.openingBalance({
      warehouseId: Number(this.warehouseId()),
      sparePartId: Number(this.sparePartId()),
      quantity: Number(this.quantity()),
      description: this.description().trim() || null
    }).subscribe({
      next: res => {
        if (res?.success) {
          this.router.navigate(['/inventory/stock']);
        } else {
          this.error.set(res?.message || 'Unable to create opening stock.');
        }
      },
      error: err => this.error.set(workOrderError(err))
    });
  }
}
