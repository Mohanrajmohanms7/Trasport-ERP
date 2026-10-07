import { FormValidationDirective } from '../../shared/form-validation.directive';
import { FeatureService } from '../../services/feature.service';
import { AttachmentsPanelComponent } from '../../shared/attachments-panel/attachments-panel';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { InvoiceMgmtService, SalesInvoice, SalesInvoiceDetail } from '../../services/invoice-mgmt.service';
import { MasterService } from '../../services/master.service';
import { TripMgmtService } from '../../services/trip-mgmt.service';
import { MatTabsModule } from '@angular/material/tabs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfNumberComponent, FfButtonComponent, FfDatepickerComponent, FfTextboxComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { UomMgmtService, OrderUnit } from '../../services/uom-mgmt.service';
import { uomLabel, orderLineText } from '../../shared/uom-label';

import { PickerService, activeOrSelected, tripLabel } from '../../services/picker.service';
import { QuickCreateComponent, QuickCreateHost, QuickCreateService } from '../../shared/quick-create/quick-create';
import { LookupAddComponent, LookupAddHost, LookupAddService } from '../../shared/lookup-add/lookup-add';
@Component({
  selector: 'app-invoice-details-console',
  standalone: true,
imports: [LookupAddComponent, FormValidationDirective, AttachmentsPanelComponent, ExportButtonsComponent, QuickCreateComponent, 
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfNumberComponent,
    FfButtonComponent,
    FfDatepickerComponent,
    FfTextboxComponent
  ],
  templateUrl: './invoice-details-console.html',
  styles: []
})
export class InvoiceDetailsConsoleComponent implements OnInit {
  /** Subscription feature access (hides tabs/buttons not in the client's plan). */
  readonly features = inject(FeatureService);
  private invoiceMgmtService = inject(InvoiceMgmtService);
  private tripService = inject(TripMgmtService);
  private masterService = inject(MasterService);
  private picker = inject(PickerService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);
  private uomMgmtService = inject(UomMgmtService);

  /** Units switched on for orders (default first) + units of lines being edited that were switched off later. */
  orderUnits = signal<OrderUnit[]>([]);
  private legacyUnits = signal<OrderUnit[]>([]);

  get defaultUnitId(): number | null {
    const u = this.orderUnits();
    return (u.find(x => x.isDefault) || u[0])?.id ?? null;
  }
  private allUnits(): OrderUnit[] {
    const list = [...this.orderUnits()];
    for (const l of this.legacyUnits()) if (!list.some(x => x.id === l.id)) list.push(l);
    return list;
  }
  uomOptions(): FfSelectOption[] {
    return this.allUnits().map(u => ({ label: u.label || u.name, value: u.id }));
  }
  /** Display text of a billing row's unit, e.g. "Unit". */
  rowUnit(row: any): string {
    const id = Number(row.get('uom.id')?.value);
    const u = this.allUnits().find(x => x.id === id);
    return u ? uomLabel(u) : '';
  }
  perUnitLabel(text: string, row: any): string {
    return `${text} / ${this.rowUnit(row) || 'unit'}`;
  }
  /** "2 Unit M-Sand" (ready-to-bill trip list). */
  lineText(d: any): string {
    const q = d?.deliveredQuantity && d.deliveredQuantity > 0 ? d.deliveredQuantity : d?.quantity;
    return orderLineText(q, d?.uom, d?.material?.name);
  }
  private rememberUnit(u: any) {
    if (u?.id && !this.allUnits().some(x => x.id === u.id)) {
      this.legacyUnits.set([...this.legacyUnits(), { id: u.id, code: u.code || '', name: u.name || '', symbol: u.symbol, label: uomLabel(u) }]);
    }
  }

  private companyId = resolveTenantCompanyId();
  readonly today = new Date().toISOString().slice(0, 10);

  // States
  activeTab = signal<string>('list'); // 'list' | 'editor'
  billingTab = signal<'ledger' | 'ready'>('ledger');
  loading = signal<boolean>(false);
  showEditor = signal<boolean>(false);
  editingInvoice = signal<SalesInvoice | null>(null);

  // Lists
  invoices = signal<SalesInvoice[]>([]);
  readyTrips = signal<any[]>([]);
  customers = signal<any[]>([]);
  readonly customerPick = this.picker.bind('customers', this.customers);
  readonly qc = inject(QuickCreateService);
  readonly quick = new QuickCreateHost();
  readonly la = inject(LookupAddService);
  readonly lookupAdd = new LookupAddHost();
  newPaymentTerms(text: string): void {
    this.lookupAdd.start('PAYMENT_TERMS', 'payment terms', text, rec => { this.paymentTerms.set([...this.paymentTerms(), rec]); this.invoiceForm.get('paymentTerms')?.setValue(rec.code); });
  }

  newCustomer(text: string): void {
    this.quick.start('customer', text, rec => { PickerService.merge(this.customers, [rec]); this.invoiceForm.get('customer.id')?.setValue(rec.id); });
  }

  trips = signal<any[]>([]);
  readonly tripPick = this.picker.bindTrips(this.trips, true);
  materials = signal<any[]>([]);
  readonly materialPick = this.picker.bind('materials', this.materials);
  paymentTerms = signal<any[]>([]);


  get customerOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Customer --', value: '' }, ...activeOrSelected(this.customers(), this.invoiceForm?.getRawValue()?.customer?.id).map(customer => ({ label: customer.name, value: customer.id }))];
  }
  get paymentTermsOptions(): FfSelectOption[] {
    return this.paymentTerms().length > 0
      ? this.paymentTerms().map(pt => ({ label: pt.name, value: pt.code }))
      : [
          { label: 'DUE ON RECEIPT', value: 'IMMEDIATE' },
          { label: 'NET 15 DAYS', value: 'NET_15' },
          { label: 'NET 30 DAYS', value: 'NET_30' }
        ];
  }
  get tripOptions(): FfSelectOption[] {
    // Only completed trips can be billed; the backend also blocks trips already on another invoice.
    return [{ label: '-- General --', value: '' }, ...this.trips()
      .filter(trip => trip.status === 'COMPLETED')
      .map(trip => ({ label: trip.tripNumber, value: trip.id }))];
  }
  get materialOptions(): FfSelectOption[] {
    return [{ label: '-- Choose --', value: '' }, ...activeOrSelected(this.materials(), (this.invoiceForm?.getRawValue()?.details || []).map((d: any) => d?.material?.id)).map(material => ({ label: material.name, value: material.id }))];
  }

  // Forms
  invoiceForm!: FormGroup;

  ngOnInit() {
    this.initForm();
    this.loadInvoices();
    this.loadReadyTrips();
    this.loadDropdownData();
  }


  loadDropdownData() {
    this.masterService.getLookupList(this.companyId, 'PAYMENT_TERMS').subscribe(res => {
      if (res.success && res.data) {
        this.paymentTerms.set(res.data);
      }
    });
    this.masterService.getMasters<any>('customers', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.customers.set(res.data.content || res.data);
      }
    });
    this.masterService.getMasters<any>('trips', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.trips.set(res.data.content || res.data);
      }
    });
    this.uomMgmtService.getOrderUnits().subscribe(res => {
      if (res.success && res.data) {
        this.orderUnits.set(res.data);
        for (const row of this.detailsArray?.controls ?? []) {
          if (!row.get('uom.id')?.value && this.defaultUnitId) row.get('uom.id')?.setValue(this.defaultUnitId);
        }
      }
    });
    this.masterService.getMasters<any>('materials', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.materials.set(res.data.content || res.data);
      }
    });
  }

  initForm() {
    this.invoiceForm = this.fb.group({
      customer: this.fb.group({
        id: ['', Validators.required]
      }),
      invoiceDate: [this.today, Validators.required],
      placeOfSupply: ['', Validators.pattern(/^\d{2}$/)],
      paymentTerms: ['NET_30', Validators.required],
      discount: [0, [Validators.required, Validators.min(0)]],
      details: this.fb.array([])
    });
  }

  get detailsArray(): FormArray {
    return this.invoiceForm.get('details') as FormArray;
  }

  // Mirrors backend InvoiceTaxCalculator: taxable = qty x (rate + freight + loading + royalty);
  // the invoice discount is split across lines by taxable value and taken off BEFORE GST.
  private round2(v: number): number {
    return Math.round((v + Number.EPSILON) * 100) / 100;
  }

  getItemSubtotal(row: any): number {
    const qty = Number(row.get('quantity')?.value || 0);
    const base = Number(row.get('rate')?.value || 0) + Number(row.get('freightCharges')?.value || 0)
      + Number(row.get('loadingCharges')?.value || 0) + Number(row.get('royalty')?.value || 0);
    return this.round2(qty * base);
  }

  private getItemDiscountShare(row: any): number {
    const subtotal = this.grandSubtotal;
    if (subtotal <= 0) return 0;
    return this.round2(Math.min(this.grandDiscount, subtotal) * this.getItemSubtotal(row) / subtotal);
  }

  getItemGst(row: any): number {
    const gstPct = Number(row.get('gstPercentage')?.value ?? 0);
    return this.round2((this.getItemSubtotal(row) - this.getItemDiscountShare(row)) * gstPct / 100);
  }

  getItemTotalAmount(row: any): number {
    return this.getItemSubtotal(row) - this.getItemDiscountShare(row) + this.getItemGst(row);
  }

  get grandSubtotal(): number {
    return this.round2(this.detailsArray.controls.reduce((sum, row) => sum + this.getItemSubtotal(row), 0));
  }

  get grandDiscount(): number {
    return Number(this.invoiceForm.get('discount')?.value || 0);
  }

  get grandTaxable(): number {
    return this.round2(this.grandSubtotal - Math.min(this.grandDiscount, this.grandSubtotal));
  }

  get grandGst(): number {
    return this.round2(this.detailsArray.controls.reduce((sum, row) => sum + this.getItemGst(row), 0));
  }

  get grandTotal(): number {
    return this.round2(this.grandTaxable + this.grandGst);
  }

  get discountTooHigh(): boolean {
    return this.grandDiscount > this.grandSubtotal && this.grandSubtotal > 0;
  }

  private apiError(err: any, fallback: string): string {
    const errors = err?.error?.errors;
    const detail = Array.isArray(errors) && errors.length ? String(errors[0]) : '';
    const message = err?.error?.message || '';
    if (message && detail && !detail.startsWith(message)) return `${message}: ${detail}`;
    return detail || message || fallback;
  }

  private buildDetailRow(d?: any) {
    if (d?.uom) this.rememberUnit(d.uom);
    const row = this.fb.group({
      trip: this.fb.group({
        id: [d?.trip?.id || '']
      }),
      material: this.fb.group({
        id: [d?.material?.id ?? '', Validators.required]
      }),
      uom: this.fb.group({
        id: [d?.uom?.id ?? this.defaultUnitId, Validators.required]
      }),
      quantity: [d?.quantity ?? 1, [Validators.required, Validators.min(0.01)]],
      rate: [d?.rate ?? 0, [Validators.required, Validators.min(0)]],
      freightCharges: [d?.freightCharges ?? 0, [Validators.required, Validators.min(0)]],
      loadingCharges: [d?.loadingCharges ?? 0, [Validators.required, Validators.min(0)]],
      royalty: [d?.royalty ?? 0, [Validators.required, Validators.min(0)]],
      gstPercentage: [d?.gstPercentage ?? 5, [Validators.required, Validators.min(0), Validators.max(28)]]
    });
    // A trip line is billed in the trip's unit (the server enforces it); follow it on screen too.
    const syncTripUnit = () => {
      const trip = this.trips().find(t => t.id === Number(row.get('trip.id')?.value));
      const matId = Number(row.get('material.id')?.value);
      const line = trip?.details?.find((x: any) => x.material?.id === matId);
      if (line?.uom?.id) { this.rememberUnit(line.uom); row.get('uom.id')?.setValue(line.uom.id, { emitEvent: false }); }
    };
    row.get('trip.id')?.valueChanges.subscribe(syncTripUnit);
    row.get('material.id')?.valueChanges.subscribe(syncTripUnit);
    return row;
  }

  addDetail() {
    this.detailsArray.push(this.buildDetailRow());
  }

  removeDetail(index: number) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Remove Line Item',
        message: 'Remove this invoice line item?',
        confirmText: 'Remove',
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.detailsArray.removeAt(index);
    });
  }

  loadInvoices() {
    this.loading.set(true);
    this.invoiceMgmtService.getInvoices().subscribe(res => {
      if (res.success && res.data) {
        this.invoices.set(res.data.content || res.data);
      }
      this.loading.set(false);
    });
  }

  openAddInvoice() {
    this.editingInvoice.set(null);
    this.invoiceForm.reset({ paymentTerms: 'NET_30', discount: 0, invoiceDate: this.today, placeOfSupply: '' });
    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }
    this.addDetail();
    this.showEditor.set(true);
  }

  openEditInvoice(inv: SalesInvoice) {
    this.editingInvoice.set(inv);
    this.invoiceForm.patchValue({
      customer: { id: inv.customer?.id },
      paymentTerms: inv.paymentTerms,
      discount: inv.discount,
      invoiceDate: inv.invoiceDate || this.today,
      placeOfSupply: inv.placeOfSupply || ''
    });

    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }

    if (inv.details) {
      for (const d of inv.details) {
        this.detailsArray.push(this.buildDetailRow(d));
      }
    }

    this.showEditor.set(true);
  }

  saveInvoice() {
    if (this.invoiceForm.invalid) return;

    this.loading.set(true);
    const val = this.invoiceForm.getRawValue();
    const invObj = this.editingInvoice();

    if (!val.placeOfSupply) val.placeOfSupply = null;
    // Clean details trips if empty
    if (val.details) {
      for (const d of val.details) {
        if (!d.trip?.id) d.trip = null;
      }
    }

    if (invObj && invObj.id) {
      this.invoiceMgmtService.updateInvoice(invObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Invoice updated successfully');
          this.loadInvoices();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(this.apiError(err, 'Failed to update invoice'));
        }
      });
    } else {
      this.invoiceMgmtService.createInvoice(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Invoice generated successfully');
          this.loadInvoices();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(this.apiError(err, 'Failed to generate invoice'));
        }
      });
    }
  }

  approveInvoice(inv: SalesInvoice) {
    if (!inv.id) return;
    this.invoiceMgmtService.approveInvoice(inv.id).subscribe({
      next: () => {
        this.notify.success('Invoice approved successfully');
        this.loadInvoices();
      },
      error: (err) => this.notify.error(this.apiError(err, 'Failed to approve invoice'))
    });
  }

  cancelInvoice(inv: SalesInvoice) {
    if (!inv.id) return;
    this.invoiceMgmtService.cancelInvoice(inv.id).subscribe({
      next: () => {
        this.notify.success('Invoice cancelled successfully');
        this.loadInvoices();
      },
      error: (err) => this.notify.error(this.apiError(err, 'Failed to cancel invoice'))
    });
  }

  deleteInvoice(inv: SalesInvoice) {
    if (!inv.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Invoice',
        message: `Are you sure you want to remove invoice record: ${inv.invoiceNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && inv.id) {
        this.invoiceMgmtService.deleteInvoice(inv.id).subscribe({
          next: () => {
            this.notify.success('Invoice record deleted successfully');
            this.loadInvoices();
            this.loadReadyTrips();
          },
          error: (err) => this.notify.error(this.apiError(err, 'Failed to delete invoice record'))
        });
      }
    });
  }

  setTab(tab: 'ledger' | 'ready') {
    this.billingTab.set(tab);
    if (tab === 'ready') {
      this.loadReadyTrips();
    } else {
      this.loadInvoices();
    }
  }

  loadReadyTrips() {
    this.loading.set(true);
    this.tripService.getTripsReadyForBilling().subscribe({
      next: (res) => {
        this.readyTrips.set(res.success && res.data ? (res.data.content || res.data) : []);
        this.loading.set(false);
      },
      error: () => {
        this.readyTrips.set([]);
        this.loading.set(false);
      }
    });
  }

  /** Multi-select on "Ready for billing": trips of one customer become one invoice. */
  selectedTripIds = signal<number[]>([]);
  isTripSelected(trip: any): boolean {
    return this.selectedTripIds().includes(trip.id);
  }
  toggleTrip(trip: any): void {
    const ids = this.selectedTripIds();
    if (ids.includes(trip.id)) {
      this.selectedTripIds.set(ids.filter(x => x !== trip.id));
      return;
    }
    const first = this.readyTrips().find(t => t.id === ids[0]);
    if (first && first.booking?.customer?.id !== trip.booking?.customer?.id) {
      this.notify.error('Select trips of one customer for one invoice.');
      return;
    }
    this.selectedTripIds.set([...ids, trip.id]);
  }
  selectAllForCustomer(trip: any): void {
    const cid = trip.booking?.customer?.id;
    this.selectedTripIds.set(this.readyTrips().filter(t => t.booking?.customer?.id === cid).map(t => t.id));
  }
  invoiceSelectedTrips(): void {
    const ids = this.selectedTripIds();
    if (!ids.length) return;
    this.loading.set(true);
    this.invoiceMgmtService.createInvoiceFromTrips(ids).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.notify.success(res.message || 'Invoice draft generated');
        this.selectedTripIds.set([]);
        this.loadReadyTrips();
        this.loadInvoices();
        this.billingTab.set('ledger');
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(this.apiError(err, 'Failed to generate invoice'));
      }
    });
  }

  generateInvoice(trip: any) {
    if (!trip.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Generate Invoice',
        message: `Generate tax invoice draft for trip: ${trip.tripNumber}?`,
        confirmText: 'Generate',
        type: 'info'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.loading.set(true);
        this.invoiceMgmtService.createInvoiceFromTrip(trip.id).subscribe({
          next: (res) => {
            this.loading.set(false);
            this.notify.success(res.message || 'Invoice draft generated successfully');
            this.loadReadyTrips();
            this.loadInvoices();
            this.billingTab.set('ledger');
            if (res.data) {
              this.openEditInvoice(res.data);
            }
          },
          error: (err) => {
            this.loading.set(false);
            this.notify.error(this.apiError(err, 'Failed to generate invoice'));
          }
        });
      }
    });
  }

  downloadPdf(inv: SalesInvoice) {
    if (!inv.id) return;
    this.invoiceMgmtService.downloadInvoicePdf(inv.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `invoice_${inv.invoiceNumber || inv.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.notify.success('Invoice PDF downloaded successfully');
      },
      error: () => this.notify.error('Failed to download invoice PDF')
    });
  }

  printInvoice(inv: SalesInvoice) {
    if (!inv.id) return;
    this.invoiceMgmtService.getInvoicePrintData(inv.id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const printWindow = window.open('', '_blank');
          if (printWindow) {
            printWindow.document.write(this.buildPrintHtml(res.data));
            printWindow.document.close();
            printWindow.focus();
            setTimeout(() => {
              printWindow.print();
            }, 300);
          }
        } else {
          this.notify.error('Failed to fetch invoice print data');
        }
      },
      error: () => this.notify.error('Failed to fetch invoice print data')
    });
  }

  private buildPrintHtml(data: any): string {
    const interState = data.supplyType === 'INTER_STATE' || (data.totalIGST || 0) > 0;
    const itemsHtml = (data.items || []).map((item: any, index: number) => `
      <tr>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: center;">${index + 1}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${item.materialName || 'General Freight'}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${item.tripNumber || 'N/A'}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">${item.quantity}${item.uomLabel ? ' ' + item.uomLabel : ''}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">₹${(item.rate || 0).toFixed(2)}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">₹${((item.freightCharges || 0) + (item.loadingCharges || 0) + (item.royalty || 0)).toFixed(2)}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">₹${(item.lineTaxable || 0).toFixed(2)}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">${item.gstPercentage}%</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">₹${(item.lineTax || 0).toFixed(2)}</td>
        <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right; font-weight: bold;">₹${(item.netAmount || 0).toFixed(2)}</td>
      </tr>
    `).join('');

    return `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Invoice ${data.invoiceNumber}</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 20px; color: #111827; }
          .header { display: flex; justify-content: space-between; border-bottom: 2px solid #1e3a8a; padding-bottom: 12px; margin-bottom: 16px; }
          .company { font-size: 14px; }
          .company h2 { margin: 0 0 4px 0; color: #1e3a8a; font-size: 18px; }
          .title { text-align: center; font-size: 20px; font-weight: bold; color: #1e3a8a; margin: 16px 0; }
          .meta-grid { display: flex; gap: 20px; margin-bottom: 20px; }
          .meta-card { flex: 1; background: #f9fafb; border: 1px solid #e5e7eb; padding: 12px; border-radius: 6px; font-size: 12px; }
          .meta-card h3 { margin: 0 0 8px 0; color: #1e3a8a; font-size: 13px; text-transform: uppercase; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 20px; }
          th { background: #1e3a8a; color: white; padding: 8px; border: 1px solid #1e3a8a; text-align: left; }
          .summary-container { display: flex; justify-content: space-between; gap: 20px; font-size: 12px; }
          .summary-card { width: 300px; background: #f9fafb; border: 1px solid #e5e7eb; padding: 12px; border-radius: 6px; }
          .summary-row { display: flex; justify-content: space-between; padding: 4px 0; }
          .grand-total { font-weight: bold; font-size: 14px; color: #1e3a8a; border-top: 2px solid #1e3a8a; padding-top: 6px; margin-top: 6px; }
          @media print { body { margin: 0; } }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="company">
            <h2>${data.companyName || 'TRANSAFLOW TRANSPORT ERP'}</h2>
            <div>${data.companyAddress || ''}</div>
            <div>Phone: ${data.companyPhone || ''} | Email: ${data.companyEmail || ''}</div>
            <div>GSTIN: <strong>${data.companyGSTIN || 'N/A'}</strong></div>
          </div>
          <div style="text-align: right; font-size: 12px;">
            <div style="font-weight: bold; color: #1e3a8a;">BRANCH DETAILS</div>
            <div>${data.branchName || ''}</div>
            <div>${data.branchAddress || ''}</div>
          </div>
        </div>

        <div class="title">TAX INVOICE ${data.status === 'DRAFT' ? '(DRAFT)' : ''}</div>

        <div class="meta-grid">
          <div class="meta-card">
            <h3>Bill To (Customer)</h3>
            <div><strong>${data.customerName || 'N/A'}</strong></div>
            <div>Code: ${data.customerCode || 'N/A'}</div>
            <div>Address: ${data.customerAddress || 'N/A'}</div>
            <div>GSTIN: <strong>${data.customerGSTIN || 'N/A'}</strong></div>
          </div>
          <div class="meta-card">
            <h3>Invoice Reference</h3>
            <div>Invoice No: <strong>${data.invoiceNumber || ''}</strong></div>
            <div>Invoice Date: ${data.invoiceDate || ''}</div>
            <div>Payment Terms: ${data.paymentTerms || ''}</div>
            <div>Place of Supply: ${data.placeOfSupply || 'N/A'} (${interState ? 'Inter-state' : 'Intra-state'})</div>
            <div>Status: <strong>${data.status || ''}</strong> (${data.paymentStatus || ''})</div>
          </div>
        </div>

        <table>
          <thead>
            <tr>
              <th style="width: 5%; text-align: center;">#</th>
              <th>Material / Item</th>
              <th>Trip #</th>
              <th style="text-align: right;">Qty</th>
              <th style="text-align: right;">Rate</th>
              <th style="text-align: right;">Add. Charges</th>
              <th style="text-align: right;">Taxable</th>
              <th style="text-align: right;">GST %</th>
              <th style="text-align: right;">GST Amt</th>
              <th style="text-align: right;">Net Total</th>
            </tr>
          </thead>
          <tbody>
            ${itemsHtml}
          </tbody>
        </table>

        <div class="summary-container">
          <div style="font-size: 11px; color: #6b7280;">
            <p>1. Payments subject to terms and conditions.</p>
            <p>2. Computer generated tax invoice.</p>
          </div>
          <div class="summary-card">
            <div class="summary-row"><span>Subtotal:</span><span>₹${(data.subtotal || 0).toFixed(2)}</span></div>
            <div class="summary-row"><span>Discount:</span><span>₹${(data.discount || 0).toFixed(2)}</span></div>
            <div class="summary-row"><span>Taxable Value:</span><span>₹${(data.taxableAmount || 0).toFixed(2)}</span></div>
            ${interState
              ? `<div class="summary-row"><span>IGST:</span><span>₹${(data.totalIGST || 0).toFixed(2)}</span></div>`
              : `<div class="summary-row"><span>CGST:</span><span>₹${(data.totalCGST || 0).toFixed(2)}</span></div>
                 <div class="summary-row"><span>SGST:</span><span>₹${(data.totalSGST || 0).toFixed(2)}</span></div>`}
            <div class="summary-row grand-total"><span>Grand Total:</span><span>₹${(data.netAmount || 0).toFixed(2)}</span></div>
            <div class="summary-row"><span>Paid Amount:</span><span>₹${(data.paidAmount || 0).toFixed(2)}</span></div>
            <div class="summary-row"><span>Balance Due:</span><span><strong>₹${(data.balanceDue || 0).toFixed(2)}</strong></span></div>
          </div>
        </div>
      </body>
      </html>
    `;
  }
}
