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
import { FfDropdownComponent, FfSelectOption, FfNumberComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

@Component({
  selector: 'app-invoice-details-console',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfNumberComponent,
    FfButtonComponent
  ],
  templateUrl: './invoice-details-console.html',
  styles: []
})
export class InvoiceDetailsConsoleComponent implements OnInit {
  private invoiceMgmtService = inject(InvoiceMgmtService);
  private tripService = inject(TripMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

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
  trips = signal<any[]>([]);
  materials = signal<any[]>([]);
  paymentTerms = signal<any[]>([]);


  get customerOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Customer --', value: '' }, ...this.customers().map(customer => ({ label: customer.name, value: customer.id }))];
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
    return [{ label: '-- General --', value: '' }, ...this.trips().map(trip => ({ label: trip.tripNumber, value: trip.id }))];
  }
  get materialOptions(): FfSelectOption[] {
    return [{ label: '-- Choose --', value: '' }, ...this.materials().map(material => ({ label: material.name, value: material.id }))];
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
      paymentTerms: ['NET_30', Validators.required],
      discount: [0, [Validators.required, Validators.min(0)]],
      details: this.fb.array([])
    });
  }

  get detailsArray(): FormArray {
    return this.invoiceForm.get('details') as FormArray;
  }

  addDetail() {
    const detailGroup = this.fb.group({
      trip: this.fb.group({
        id: ['']
      }),
      material: this.fb.group({
        id: ['', Validators.required]
      }),
      quantity: [1, [Validators.required, Validators.min(1)]],
      rate: [0, [Validators.required, Validators.min(0)]],
      freightCharges: [0, [Validators.required, Validators.min(0)]],
      loadingCharges: [0, [Validators.required, Validators.min(0)]],
      royalty: [0, [Validators.required, Validators.min(0)]],
      gstPercentage: [18, Validators.required]
    });

    this.detailsArray.push(detailGroup);
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
    this.invoiceForm.reset({ paymentTerms: 'NET_30', discount: 0 });
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
      discount: inv.discount
    });

    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }

    if (inv.details) {
      for (const d of inv.details) {
        const row = this.fb.group({
          trip: this.fb.group({
            id: [d.trip?.id || '']
          }),
          material: this.fb.group({
            id: [d.material?.id, Validators.required]
          }),
          quantity: [d.quantity, [Validators.required, Validators.min(1)]],
          rate: [d.rate, [Validators.required, Validators.min(0)]],
          freightCharges: [d.freightCharges, [Validators.required, Validators.min(0)]],
          loadingCharges: [d.loadingCharges, [Validators.required, Validators.min(0)]],
          royalty: [d.royalty, [Validators.required, Validators.min(0)]],
          gstPercentage: [d.gstPercentage, Validators.required]
        });
        this.detailsArray.push(row);
      }
    }

    this.showEditor.set(true);
  }

  saveInvoice() {
    if (this.invoiceForm.invalid) return;

    this.loading.set(true);
    const val = this.invoiceForm.getRawValue();
    const invObj = this.editingInvoice();

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
          this.notify.error(err.error?.message || 'Failed to update invoice');
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
          this.notify.error(err.error?.message || 'Failed to generate invoice');
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
      error: () => this.notify.error('Failed to approve invoice')
    });
  }

  cancelInvoice(inv: SalesInvoice) {
    if (!inv.id) return;
    this.invoiceMgmtService.cancelInvoice(inv.id).subscribe({
      next: () => {
        this.notify.success('Invoice cancelled successfully');
        this.loadInvoices();
      },
      error: () => this.notify.error('Failed to cancel invoice')
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
          error: () => this.notify.error('Failed to delete invoice record')
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
            this.notify.error(err.error?.message || 'Failed to generate invoice');
          }
        });
      }
    });
  }
}
