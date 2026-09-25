import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService, InventoryTransaction, Warehouse } from '../../services/inventory.service';
import { workOrderError } from '../../services/work-order.service';

@Component({
  selector: 'app-inventory-transaction-list',
  standalone: true,
  imports: [ExportButtonsComponent, CommonModule, FormsModule],
  templateUrl: './inventory-transaction-list.html'
})
export class InventoryTransactionListComponent implements OnInit {
  private inventory = inject(InventoryService);

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<InventoryTransaction[]>([]);
  warehouses = signal<Warehouse[]>([]);
  warehouseId = signal('');
  transactionType = signal('');

  ngOnInit() {
    this.inventory.listWarehouses({ page: 0, size: 100 }).subscribe({
      next: res => this.warehouses.set(res?.success && res.data ? res.data.content || [] : []),
      error: () => this.warehouses.set([])
    });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.inventory.listTransactions({
      warehouseId: this.warehouseId() ? Number(this.warehouseId()) : null,
      transactionType: this.transactionType() || null,
      page: 0,
      size: 50
    }).subscribe({
      next: res => {
        this.rows.set(res?.success && res.data ? res.data.content || [] : []);
        if (!res?.success) this.error.set(res?.message || 'Unable to load transactions.');
        this.loading.set(false);
      },
      error: err => {
        this.error.set(workOrderError(err));
        this.loading.set(false);
      }
    });
  }

  signedQuantity(row: InventoryTransaction): string {
    const qty = Number(row.quantity ?? 0);
    const signed = row.transactionType === 'ISSUE' ? -qty : qty;
    const formatted = signed.toFixed(3);
    return signed > 0 ? '+' + formatted : formatted;
  }
}
