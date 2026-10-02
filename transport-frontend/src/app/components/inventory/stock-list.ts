import { FeatureService } from '../../services/feature.service';
import { BulkUploadDialogComponent } from '../../shared/bulk-upload/bulk-upload-dialog';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InventoryService, Warehouse, WarehouseStock } from '../../services/inventory.service';
import { SparePart, SparePartService } from '../../services/spare-part.service';
import { workOrderError } from '../../services/work-order.service';

import { PickerService, activeOrSelected } from '../../services/picker.service';
import { FfDropdownComponent, FfSelectOption } from '@ff/ui';
@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [FfDropdownComponent, BulkUploadDialogComponent, ExportButtonsComponent, CommonModule, FormsModule],
  templateUrl: './stock-list.html'
})
export class StockListComponent implements OnInit {
  private picker = inject(PickerService);
  get partOpts(): FfSelectOption[] { return this.parts().map((p: any) => ({ label: [p.code, p.name].filter(Boolean).join(' — '), value: String(p.id) })); }
  /** Subscription feature access (hides tabs/buttons not in the client's plan). */
  readonly features = inject(FeatureService);
  /** Excel bulk creation dialog. */
  showUpload = signal(false);
  /** One-time cost for stock entered before costing existed (posts it to the inventory account). */
  setCost(row: any): void {
    const v = prompt(`Unit cost (₹) for ${row.sparePartName} in ${row.warehouseCode || row.warehouseName}\nQuantity on hand: ${row.availableQuantity}`);
    if (v === null) return;
    const cost = Number(v);
    if (!(cost > 0)) { alert('Enter a cost greater than zero.'); return; }
    this.inventory.setInitialCost(row.id, cost).subscribe({
      next: () => this.load(),
      error: (e: any) => alert(e?.error?.errors?.[0] || e?.error?.message || 'Could not set cost')
    });
  }

  lowOnly = signal(false);

  visibleRows() {
    return this.lowOnly() ? this.rows().filter((r: any) => r.stockStatus === 'REORDER' || r.stockStatus === 'OUT') : this.rows();
  }

  countStatus(status: string): number {
    return this.rows().filter((r: any) => r.stockStatus === status).length;
  }

  private inventory = inject(InventoryService);
  private spareParts = inject(SparePartService);
  private router = inject(Router);
  private auth = inject(AuthService);

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<WarehouseStock[]>([]);
  warehouses = signal<Warehouse[]>([]);
  parts = signal<SparePart[]>([]);
  readonly partPick = { search: (q: string) => this.picker.spareParts(q).subscribe(r => PickerService.merge(this.parts as any, r)), resolve: (_: unknown) => {} };
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

  receive() {
    this.router.navigate(['/inventory/stock/receipt']);
  }
}
