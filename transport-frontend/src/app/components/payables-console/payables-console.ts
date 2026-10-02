import { FeatureService } from '../../services/feature.service';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { AttachmentsPanelComponent } from '../../shared/attachments-panel/attachments-panel';

type Tab = 'bills' | 'payments';

/**
 * Accounts payable: supplier bills (manual, or created automatically by credit stock receipts and workshop jobs)
 * and supplier payments allocated to those bills. Amounts owed come from the server.
 */
import { PickerService, activeOrSelected } from '../../services/picker.service';
import { FfDropdownComponent, FfSelectOption } from '@ff/ui';
import { QuickCreateComponent, QuickCreateHost, QuickCreateService } from '../../shared/quick-create/quick-create';
@Component({
  selector: 'app-payables-console',
  standalone: true,
  imports: [QuickCreateComponent, FfDropdownComponent, CommonModule, FormsModule, RouterLink, AttachmentsPanelComponent],
  templateUrl: './payables-console.html'
})
export class PayablesConsoleComponent implements OnInit {
  private picker = inject(PickerService);
  get supplierOpts(): FfSelectOption[] { return activeOrSelected(this.suppliers(), [this.bill?.supplierId, this.pay?.supplierId]).map((x: any) => ({ label: x.name || x.code, value: String(x.id) })); }
  get vehicleOpts(): FfSelectOption[] { return activeOrSelected(this.vehicles(), this.bill?.vehicleId).map((v: any) => ({ label: v.name || v.code, value: String(v.id) })); }
  /** Subscription feature access (hides tabs/buttons not in the client's plan). */
  readonly features = inject(FeatureService);
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private companyId = resolveTenantCompanyId();
  readonly today = new Date().toISOString().slice(0, 10);
  readonly categories = ['REPAIR', 'TYRES', 'FUEL', 'OFFICE', 'OTHER'];
  readonly methods = [{ v: 'BANK_TRANSFER', l: 'Bank transfer' }, { v: 'UPI', l: 'UPI' }, { v: 'CHEQUE', l: 'Cheque' }, { v: 'CASH', l: 'Cash' }];

  tab = signal<Tab>('bills');
  suppliers = signal<any[]>([]);
  readonly supplierPick = this.picker.bind('suppliers', this.suppliers);
  readonly qc = inject(QuickCreateService);
  readonly quick = new QuickCreateHost();
  newSupplier(text: string): void {
    this.quick.start('supplier', text, rec => { PickerService.merge(this.suppliers as any, [rec]); this.bill.supplierId = String(rec.id); this.onSupplierForBill(); });
  }
  vehicles = signal<any[]>([]);
  readonly vehiclePick = this.picker.bind('vehicles', this.vehicles);
  bills = signal<any[]>([]);
  payments = signal<any[]>([]);
  loading = signal(false);
  filter: any = { supplierId: '', paymentStatus: '', status: '' };

  showBill = signal(false);
  bill: any = {};
  showPay = signal(false);
  pay: any = {};
  openBills = signal<any[]>([]);
  alloc: Record<number, number | null> = {};
  selectedBill = signal<any | null>(null);
  paymentDetail = signal<{ payment: any; rows: any[] } | null>(null);

  readonly totals = computed(() => {
    const open = this.bills().filter(b => b.status === 'APPROVED');
    const due = open.reduce((s, b) => s + (Number(b.totalAmount) - Number(b.paidAmount || 0)), 0);
    const overdue = open.filter(b => b.dueDate && b.dueDate < this.today)
      .reduce((s, b) => s + (Number(b.totalAmount) - Number(b.paidAmount || 0)), 0);
    return { due, overdue, drafts: this.bills().filter(b => b.status === 'DRAFT').length };
  });

  ngOnInit(): void {
    const p = new HttpParams().set('companyId', String(this.companyId)).set('size', '500');
    this.http.get<any>('/api/v1/suppliers', { params: p }).subscribe({ next: r => this.suppliers.set(r?.data?.content ?? r?.data ?? []), error: () => {} });
    this.http.get<any>('/api/v1/vehicles', { params: p }).subscribe({ next: r => this.vehicles.set(r?.data?.content ?? r?.data ?? []), error: () => {} });
    this.load();
  }

  private err(e: any, fb: string): string {
    const errs = e?.error?.errors;
    const d = Array.isArray(errs) && errs.length ? String(errs[0]) : '';
    const t = e?.error?.message || '';
    return t && d && !d.startsWith(t) ? `${t}: ${d}` : d || t || fb;
  }

  setTab(t: Tab): void { this.tab.set(t); this.load(); }

  load(): void {
    let p = new HttpParams().set('size', '500');
    if (this.filter.supplierId) p = p.set('supplierId', this.filter.supplierId);
    this.loading.set(true);
    if (this.tab() === 'bills') {
      if (this.filter.paymentStatus) p = p.set('paymentStatus', this.filter.paymentStatus);
      if (this.filter.status) p = p.set('status', this.filter.status);
      this.http.get<any>('/api/v1/payables/bills', { params: p }).subscribe({
        next: r => { this.bills.set(r?.data?.content ?? []); this.loading.set(false); },
        error: e => { this.loading.set(false); this.notify.error(this.err(e, 'Could not load bills')); }
      });
    } else {
      this.http.get<any>('/api/v1/payables/payments', { params: p }).subscribe({
        next: r => { this.payments.set(r?.data?.content ?? []); this.loading.set(false); },
        error: e => { this.loading.set(false); this.notify.error(this.err(e, 'Could not load payments')); }
      });
    }
  }

  balance(b: any): number { return Number(b.totalAmount || 0) - Number(b.paidAmount || 0); }
  overdue(b: any): boolean { return b.status === 'APPROVED' && this.balance(b) > 0.009 && b.dueDate < this.today; }
  sourceLabel(b: any): string {
    return b.sourceType === 'STOCK_RECEIPT' ? 'Stock receipt' : b.sourceType === 'WORK_ORDER' ? 'Work order' : 'Manual';
  }

  // ---------------- bills
  newBill(): void {
    this.bill = { supplierId: '', supplierBillNo: '', billDate: this.today, dueDate: '', category: 'REPAIR', vehicleId: '', taxableAmount: null, gstAmount: 0, remarks: '' };
    this.showBill.set(true);
  }

  editBill(b: any): void {
    this.bill = { id: b.id, supplierId: b.supplier?.id, supplierBillNo: b.supplierBillNo, billDate: b.billDate, dueDate: b.dueDate, category: b.category,
      vehicleId: b.vehicle?.id || '', taxableAmount: b.taxableAmount, gstAmount: b.gstAmount, remarks: b.remarks };
    this.showBill.set(true);
  }

  onSupplierForBill(): void {
    const s = this.suppliers().find(x => String(x.id) === String(this.bill.supplierId));
    if (s?.creditDays && this.bill.billDate) {
      const d = new Date(this.bill.billDate);
      d.setDate(d.getDate() + Number(s.creditDays));
      this.bill.dueDate = d.toISOString().slice(0, 10);
    }
  }

  billTotal(): number { return Number(this.bill.taxableAmount || 0) + Number(this.bill.gstAmount || 0); }

  saveBill(): void {
    const body: any = {
      supplier: { id: Number(this.bill.supplierId) }, supplierBillNo: this.bill.supplierBillNo, billDate: this.bill.billDate,
      dueDate: this.bill.dueDate || null, category: this.bill.category, taxableAmount: Number(this.bill.taxableAmount || 0),
      gstAmount: Number(this.bill.gstAmount || 0), remarks: this.bill.remarks,
      vehicle: this.bill.vehicleId ? { id: Number(this.bill.vehicleId) } : null
    };
    const req = this.bill.id ? this.http.put<any>(`/api/v1/payables/bills/${this.bill.id}`, body) : this.http.post<any>('/api/v1/payables/bills', body);
    req.subscribe({
      next: r => { this.showBill.set(false); this.notify.success(`Bill ${r?.data?.billNumber} saved as draft`); this.load(); },
      error: e => this.notify.error(this.err(e, 'Bill could not be saved'))
    });
  }

  approveBill(b: any): void {
    if (!confirm(`Approve ${b.billNumber}? It will be posted to accounts.`)) return;
    this.http.post<any>(`/api/v1/payables/bills/${b.id}/approve`, {}).subscribe({
      next: () => { this.notify.success('Bill approved and posted'); this.load(); },
      error: e => this.notify.error(this.err(e, 'Approve failed'))
    });
  }

  cancelBill(b: any): void {
    if (!confirm(`Cancel ${b.billNumber}?`)) return;
    this.http.post<any>(`/api/v1/payables/bills/${b.id}/cancel`, {}).subscribe({
      next: () => { this.notify.success('Bill cancelled'); this.load(); },
      error: e => this.notify.error(this.err(e, 'Cancel failed'))
    });
  }

  // ---------------- payments
  newPayment(supplierId?: any): void {
    this.pay = { supplierId: supplierId ?? '', paymentDate: this.today, amount: null, paymentMethod: 'BANK_TRANSFER', referenceNumber: '', remarks: '' };
    this.alloc = {};
    this.openBills.set([]);
    this.showPay.set(true);
    if (supplierId) this.loadOpenBills();
  }

  loadOpenBills(): void {
    this.alloc = {};
    if (!this.pay.supplierId) { this.openBills.set([]); return; }
    this.http.get<any>(`/api/v1/payables/suppliers/${this.pay.supplierId}/open-bills`).subscribe({
      next: r => this.openBills.set(r?.data ?? []),
      error: () => this.openBills.set([])
    });
  }

  supplierDue(): number { return this.openBills().reduce((s, b) => s + this.balance(b), 0); }
  allocated(): number { return Object.values(this.alloc).reduce((s: number, v) => s + Number(v || 0), 0); }

  autoAllocate(): void {
    let left = Number(this.pay.amount || 0);
    this.alloc = {};
    for (const b of this.openBills()) {
      if (left <= 0) break;
      const take = Math.min(left, this.balance(b));
      this.alloc[b.id] = Math.round(take * 100) / 100;
      left -= take;
    }
  }

  postPayment(): void {
    const allocations = Object.entries(this.alloc).filter(([, v]) => Number(v) > 0).map(([k, v]) => ({ billId: Number(k), amount: Number(v) }));
    const body = { supplierId: Number(this.pay.supplierId), paymentDate: this.pay.paymentDate, amount: Number(this.pay.amount),
      paymentMethod: this.pay.paymentMethod, referenceNumber: this.pay.referenceNumber, remarks: this.pay.remarks,
      allocations: allocations.length ? allocations : null };
    this.http.post<any>('/api/v1/payables/payments', body).subscribe({
      next: r => { this.showPay.set(false); this.notify.success(`Payment ${r?.data?.paymentNumber} posted`); this.load(); },
      error: e => this.notify.error(this.err(e, 'Payment failed'))
    });
  }

  viewPayment(p: any): void {
    this.http.get<any>(`/api/v1/payables/payments/${p.id}/allocations`).subscribe({
      next: r => this.paymentDetail.set({ payment: p, rows: r?.data ?? [] }),
      error: () => this.paymentDetail.set({ payment: p, rows: [] })
    });
  }

  cancelPayment(p: any): void {
    if (!confirm(`Cancel payment ${p.paymentNumber}? The entry is reversed and the bills become unpaid again.`)) return;
    this.http.post<any>(`/api/v1/payables/payments/${p.id}/cancel`, {}).subscribe({
      next: () => { this.notify.success('Payment cancelled'); this.paymentDetail.set(null); this.load(); },
      error: e => this.notify.error(this.err(e, 'Cancel failed'))
    });
  }
}
