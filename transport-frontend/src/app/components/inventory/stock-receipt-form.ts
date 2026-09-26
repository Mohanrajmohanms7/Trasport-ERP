import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InventoryService, Warehouse, WarehouseStock } from '../../services/inventory.service';
import { SparePart, SparePartService } from '../../services/spare-part.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-stock-receipt-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stock-receipt-form.html'
})
export class StockReceiptFormComponent implements OnInit {
  private inventory = inject(InventoryService);
  private spareParts = inject(SparePartService);
  private router = inject(Router);

  warehouses = signal<Warehouse[]>([]);
  parts = signal<SparePart[]>([]);
  suppliers = signal<{ id: number; code?: string; name?: string }[]>([]);
  warehouseId = signal('');
  sparePartId = signal('');
  quantity = signal('');
  unitRate = signal('');
  supplierId = signal('');
  paymentMode = signal<'CREDIT' | 'CASH' | 'BANK'>('CREDIT');
  referenceNumber = signal('');
  description = signal('');
  currentQuantity = signal<number | null>(null);
  lastAvailable = signal<number | null>(null);
  lastReference = signal<string | null>(null);
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
    this.inventory.listSuppliers().subscribe({
      next: res => this.suppliers.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.suppliers.set([])
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

  save() {
    this.error.set(null);
    this.lastAvailable.set(null);
    this.lastReference.set(null);
    if (!this.warehouseId() || !this.sparePartId() || !this.quantity().trim()) {
      this.error.set('Warehouse, spare part, and quantity are required.');
      return;
    }
    this.inventory.receiveStock({
      warehouseId: Number(this.warehouseId()),
      sparePartId: Number(this.sparePartId()),
      quantity: Number(this.quantity()),
      unitRate: this.unitRate().trim() ? Number(this.unitRate()) : null,
      supplierId: this.supplierId() ? Number(this.supplierId()) : null,
      paymentMode: this.paymentMode(),
      referenceNumber: this.referenceNumber().trim() || null,
      description: this.description().trim() || null
    }).subscribe({
      next: res => {
        if (res?.success && res.data) {
          this.lastAvailable.set(res.data.availableQuantity != null ? Number(res.data.availableQuantity) : null);
          this.lastReference.set(res.data.transactionCode || null);
          this.currentQuantity.set(this.lastAvailable());
          this.quantity.set('');
        } else {
          this.error.set(res?.message || 'Unable to receive stock.');
        }
      },
      error: err => this.error.set(workOrderError(err))
    });
  }

  back() {
    this.router.navigate(['/inventory/stock']);
  }
}
