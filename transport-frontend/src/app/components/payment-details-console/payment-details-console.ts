import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule, FormArray } from '@angular/forms';

import { PaymentMgmtService, CustomerReceipt, CustomerLedger } from '../../services/payment-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

@Component({
  selector: 'app-payment-details-console',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfNumberComponent,
    FfButtonComponent
  ],
  templateUrl: './payment-details-console.html',
  styles: []
})
export class PaymentDetailsConsoleComponent implements OnInit {
  private paymentMgmtService = inject(PaymentMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  // States
  activeTab = signal<string>('receipts'); // 'receipts' | 'ledger'
  loading = signal<boolean>(false);
  showEditor = signal<boolean>(false);
  editingReceipt = signal<CustomerReceipt | null>(null);

  // Phase 35 Filter States & Pagination Signals
  page = signal<number>(0);
  pageSize = signal<number>(20);
  totalElements = signal<number>(0);

  searchControl = new FormControl<string>('');
  customerFilterControl = new FormControl<number | ''>('');
  statusControl = new FormControl<string>('');
  paymentMethodControl = new FormControl<string>('');
  fromDateControl = new FormControl<string>('');
  toDateControl = new FormControl<string>('');

  // Phase 35 Detail Overlay Signal & Timeline States
  selectedReceiptDetail = signal<any | null>(null);
  showDetailView = signal<boolean>(false);
  auditTrail = signal<any[]>([]);
  paymentHistorySummary = signal<any | null>(null);

  // Phase 36 Print Signals & Views
  showPrintPreview = signal<boolean>(false);
  printData = signal<any | null>(null);
  showPrintHistory = signal<boolean>(false);
  printHistory = signal<any | null>(null);

  // Phase 37 Dashboard Signals & Views
  showDashboard = signal<boolean>(false);
  dashboardLoading = signal<boolean>(false);
  dashboardError = signal<string | null>(null);
  
  settlementSummary = signal<any | null>(null);
  customerOutstanding = signal<any[]>([]);
  agingSummary = signal<any | null>(null);
  agingInvoices = signal<any[]>([]);
  reconciliationRows = signal<any[]>([]);
  paymentPerformance = signal<any[]>([]);
  recentApprovedReceipts = signal<any[]>([]);
  
  showCustomerOutstanding = signal<boolean>(false);
  showAgingDetails = signal<boolean>(false);
  showReconciliation = signal<boolean>(false);
  
  selectedDashboardCustomer = signal<number | ''>('');
  selectedAgingBucket = signal<string>('');
  
  dashboardCustomerFilterControl = new FormControl<number | ''>('');
  dashboardStatusFilterControl = new FormControl<string>('');
  dashboardPaymentStatusFilterControl = new FormControl<string>('');
  dashboardFromDateControl = new FormControl<string>('');
  dashboardToDateControl = new FormControl<string>('');

  customerOutstandingPage = signal<number>(0);
  customerOutstandingPageSize = signal<number>(10);
  customerOutstandingTotal = signal<number>(0);

  agingInvoicesPage = signal<number>(0);
  agingInvoicesPageSize = signal<number>(10);
  agingInvoicesTotal = signal<number>(0);

  reconciliationPage = signal<number>(0);
  reconciliationPageSize = signal<number>(10);
  reconciliationTotal = signal<number>(0);

  paymentPerformancePage = signal<number>(0);
  paymentPerformancePageSize = signal<number>(10);
  paymentPerformanceTotal = signal<number>(0);

  // Lists
  receipts = signal<any[]>([]);
  ledgerEntries = signal<CustomerLedger[]>([]);
  customers = signal<any[]>([]);
  bookings = signal<any[]>([]);
  paymentMethods = signal<any[]>([]);

  // Selected customer ID for ledger lookup (set after customers load)
  selectedCustomerId = signal<number>(0);
  selectedCustomerControl = new FormControl<number | ''>('');
  
  get customerOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Customer --', value: '' }, ...this.customers().map(customer => ({ label: customer.name, value: customer.id }))];
  }
  get ledgerCustomerOptions(): FfSelectOption[] {
    return this.customers().map(customer => ({ label: customer.name, value: customer.id }));
  }
  get bookingOptions(): FfSelectOption[] {
    return [{ label: '-- Optional / General --', value: '' }, ...this.bookings().map(booking => ({ label: booking.bookingNumber || booking.code, value: booking.id }))];
  }
  get paymentMethodOptions(): FfSelectOption[] {
    return this.paymentMethods().length > 0
      ? this.paymentMethods().map(pm => ({ label: pm.name, value: pm.code }))
      : [{ label: 'CASH', value: 'CASH' }, { label: 'UPI', value: 'UPI' }, { label: 'BANK TRANSFER', value: 'BANK_TRANSFER' }];
  }

  // Forms
  receiptForm!: FormGroup;

  ngOnInit() {
    this.selectedCustomerControl.valueChanges.subscribe(customerId => this.onCustomerChange(customerId));
    
    // Subscribe to filter changes
    this.searchControl.valueChanges.subscribe(() => this.applyFilters());
    this.customerFilterControl.valueChanges.subscribe(() => this.applyFilters());
    this.statusControl.valueChanges.subscribe(() => this.applyFilters());
    this.paymentMethodControl.valueChanges.subscribe(() => this.applyFilters());
    this.fromDateControl.valueChanges.subscribe(() => this.applyFilters());
    this.toDateControl.valueChanges.subscribe(() => this.applyFilters());

    this.initForm();
    this.loadReceipts();
    this.loadDropdownData();
  }

  loadDropdownData() {
    this.masterService.getLookupList(this.companyId, 'PAYMENT_METHOD').subscribe(res => {
      if (res.success && res.data) {
        this.paymentMethods.set(res.data);
      }
    });
    this.masterService.getMasters<any>('customers', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.customers.set(res.data.content || res.data);
        if (res.data.content && res.data.content.length > 0) {
          const firstCustId = res.data.content[0].id;
          this.selectedCustomerId.set(firstCustId);
          this.selectedCustomerControl.setValue(firstCustId, { emitEvent: false });
          this.loadLedger();
          this.paymentMgmtService.getCustomerPaymentHistory(firstCustId).subscribe(histRes => {
            if (histRes.success && histRes.data) {
              this.paymentHistorySummary.set(histRes.data);
            }
          });
        }
      }
    });
    this.masterService.getMasters<any>('bookings', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.bookings.set(res.data.content || res.data);
      }
    });
  }

  outstandingInvoices = signal<any[]>([]);

  initForm() {
    this.receiptForm = this.fb.group({
      customer: this.fb.group({
        id: ['', Validators.required]
      }),
      booking: this.fb.group({
        id: ['']
      }),
      amountReceived: [0, [Validators.required, Validators.min(1)]],
      advanceAmount: [0, [Validators.required, Validators.min(0)]],
      paymentMethod: ['CASH', Validators.required],
      referenceNumber: [''],
      remarks: [''],
      allocations: this.fb.array([])
    });

    // Listen to customer changes to load outstanding invoices
    this.receiptForm.get('customer.id')?.valueChanges.subscribe(custId => {
      if (custId) {
        this.loadOutstanding(+custId);
      } else {
        this.outstandingInvoices.set([]);
        this.clearAllocations();
      }
    });

    // Recalculate advance amount whenever amountReceived changes
    this.receiptForm.get('amountReceived')?.valueChanges.subscribe(() => this.recalculateAdvance());
  }

  get allocationsFormArray(): FormArray {
    return this.receiptForm.get('allocations') as FormArray;
  }

  clearAllocations() {
    this.allocationsFormArray.clear();
    this.recalculateAdvance();
  }

  loadOutstanding(customerId: number) {
    if (!customerId) {
      this.outstandingInvoices.set([]);
      this.clearAllocations();
      return;
    }
    this.paymentMgmtService.getOutstandingInvoices(customerId).subscribe(res => {
      if (res.success && res.data) {
        this.outstandingInvoices.set(res.data);
        this.buildAllocationControls();
      }
    });
  }

  buildAllocationControls() {
    this.clearAllocations();
    this.outstandingInvoices().forEach(inv => {
      const outstanding = inv.netAmount - (inv.paidAmount || 0);
      this.allocationsFormArray.push(this.fb.group({
        invoiceId: [inv.id],
        invoiceNumber: [inv.invoiceNumber],
        netAmount: [inv.netAmount],
        outstandingAmount: [outstanding],
        amount: [0, [Validators.min(0), Validators.max(outstanding)]],
        selected: [false]
      }));
    });

    // Listen to allocation changes
    this.allocationsFormArray.valueChanges.subscribe(() => {
      this.recalculateAdvance();
    });
  }

  recalculateAdvance() {
    const amountReceived = this.receiptForm.get('amountReceived')?.value || 0;
    let totalAllocated = 0;
    this.allocationsFormArray.controls.forEach(control => {
      if (control.get('selected')?.value) {
        totalAllocated += control.get('amount')?.value || 0;
      }
    });

    const advance = amountReceived - totalAllocated;
    this.receiptForm.get('advanceAmount')?.setValue(advance >= 0 ? advance : 0, { emitEvent: false });
  }

  get totalAllocated(): number {
    let total = 0;
    this.allocationsFormArray.controls.forEach(control => {
      if (control.get('selected')?.value) {
        total += control.get('amount')?.value || 0;
      }
    });
    return total;
  }

  get remainingUnallocated(): number {
    const amountReceived = this.receiptForm.get('amountReceived')?.value || 0;
    return amountReceived - this.totalAllocated;
  }

  toggleInvoiceSelection(index: number) {
    const group = this.allocationsFormArray.at(index);
    const isSelected = group.get('selected')?.value;
    if (isSelected) {
      const outstanding = group.get('outstandingAmount')?.value || 0;
      const unallocated = this.remainingUnallocated;
      const autoAmount = Math.max(0, Math.min(outstanding, unallocated));
      group.get('amount')?.setValue(autoAmount);
    } else {
      group.get('amount')?.setValue(0);
    }
    this.recalculateAdvance();
  }


  loadReceipts() {
    this.loading.set(true);
    const params: any = {
      page: this.page(),
      size: this.pageSize()
    };
    if (this.searchControl.value) params.search = this.searchControl.value;
    if (this.statusControl.value) params.status = this.statusControl.value;
    if (this.paymentMethodControl.value) params.paymentMethod = this.paymentMethodControl.value;
    if (this.customerFilterControl.value) params.customerId = this.customerFilterControl.value;
    if (this.fromDateControl.value) params.fromDate = this.fromDateControl.value;
    if (this.toDateControl.value) params.toDate = this.toDateControl.value;

    this.paymentMgmtService.getReceipts(params).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.receipts.set(res.data.content || res.data);
          this.totalElements.set(res.data.totalElements || (res.data.content || res.data).length);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  loadLedger() {
    if (!this.selectedCustomerId()) {
      this.ledgerEntries.set([]);
      return;
    }
    this.paymentMgmtService.getCustomerLedger(this.selectedCustomerId()).subscribe(res => {
      if (res.success && res.data) {
        this.ledgerEntries.set(res.data);
      }
    });
  }

  onCustomerChange(custId: any) {
    this.selectedCustomerId.set(+custId);
    this.loadLedger();
    if (custId) {
      this.paymentMgmtService.getCustomerPaymentHistory(+custId).subscribe(res => {
        if (res.success && res.data) {
          this.paymentHistorySummary.set(res.data);
        }
      });
    } else {
      this.paymentHistorySummary.set(null);
    }
  }

  // Phase 35 Reporting & Audit Trail detail loading
  viewReceiptDetails(receiptId: number) {
    this.loading.set(true);
    this.paymentMgmtService.getCustomerReceiptDetails(receiptId).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.selectedReceiptDetail.set(res.data);
          this.showDetailView.set(true);
          this.auditTrail.set(res.data.auditTimeline || []);
        }
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to load receipt details');
      }
    });
  }

  closeDetailView() {
    this.selectedReceiptDetail.set(null);
    this.showDetailView.set(false);
    this.auditTrail.set([]);
  }

  applyFilters() {
    this.page.set(0);
    this.loadReceipts();
  }

  clearFilters() {
    this.searchControl.setValue('', { emitEvent: false });
    this.customerFilterControl.setValue('', { emitEvent: false });
    this.statusControl.setValue('', { emitEvent: false });
    this.paymentMethodControl.setValue('', { emitEvent: false });
    this.fromDateControl.setValue('', { emitEvent: false });
    this.toDateControl.setValue('', { emitEvent: false });
    this.page.set(0);
    this.loadReceipts();
  }

  onPageChange(newPage: number) {
    this.page.set(newPage);
    this.loadReceipts();
  }

  openAddReceipt() {
    this.editingReceipt.set(null);
    this.receiptForm.reset({ paymentMethod: 'CASH', amountReceived: 0, advanceAmount: 0 });
    this.showEditor.set(true);
  }

  openEditReceipt(receipt: CustomerReceipt) {
    this.editingReceipt.set(receipt);
    this.receiptForm.patchValue({
      customer: { id: receipt.customer?.id },
      booking: { id: receipt.booking?.id || '' },
      amountReceived: receipt.amountReceived,
      advanceAmount: receipt.advanceAmount,
      paymentMethod: receipt.paymentMethod,
      referenceNumber: receipt.referenceNumber,
      remarks: receipt.remarks
    });
    this.showEditor.set(true);
  }

  saveReceipt() {
    if (this.receiptForm.invalid) return;

    this.loading.set(true);
    const val = this.receiptForm.getRawValue();
    const receiptObj = this.editingReceipt();

    if (!val.booking?.id) val.booking = null;

    if (receiptObj && receiptObj.id) {
      this.paymentMgmtService.updateReceipt(receiptObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Receipt voucher updated successfully');
          this.loadReceipts();
          this.loadLedger();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to update receipt voucher');
        }
      });
    } else {
      const activeAllocations = this.allocationsFormArray.controls
        .filter(control => control.get('selected')?.value && (control.get('amount')?.value > 0))
        .map(control => ({
          invoiceId: control.get('invoiceId')?.value,
          amount: control.get('amount')?.value
        }));

      if (this.remainingUnallocated < 0) {
        this.notify.error('Total allocation cannot exceed received amount.');
        this.loading.set(false);
        return;
      }

      const payload = {
        customerId: val.customer.id,
        receiptDate: val.receiptDate || new Date().toISOString().substring(0, 10),
        amountReceived: val.amountReceived,
        advanceAmount: this.remainingUnallocated,
        paymentMethod: val.paymentMethod,
        referenceNumber: val.referenceNumber,
        remarks: val.remarks,
        allocations: activeAllocations
      };

      this.paymentMgmtService.createReceipt(payload as any).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Receipt created successfully as Draft.');
          this.loadReceipts();
          this.loadLedger();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to record receipt voucher');
        }
      });
    }
  }


  deleteReceipt(receipt: CustomerReceipt) {
    if (!receipt.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Receipt Voucher',
        message: `Are you sure you want to remove receipt voucher: ${receipt.receiptNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && receipt.id) {
        this.paymentMgmtService.deleteReceipt(receipt.id).subscribe({
          next: () => {
            this.notify.success('Receipt voucher deleted successfully');
            this.loadReceipts();
            this.loadLedger();
          },
          error: () => this.notify.error('Failed to delete receipt voucher')
        });
      }
    });
  }

  cancelReceipt(receipt: CustomerReceipt) {
    if (!receipt.id) return;
    if (receipt.status === 'CANCELLED') {
      this.notify.error('Receipt is already cancelled.');
      return;
    }

    const title = receipt.status === 'DRAFT' ? 'Cancel Draft Receipt' : 'Cancel & Reverse Receipt';
    const message = receipt.status === 'DRAFT' 
      ? `Are you sure you want to cancel draft receipt: ${receipt.receiptNumber}?`
      : `Are you sure you want to cancel receipt: ${receipt.receiptNumber}? This will reverse all invoice allocations, ledger entries, and journal vouchers associated with this receipt.`;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title,
        message,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && receipt.id) {
        this.loading.set(true);
        this.paymentMgmtService.cancelCustomerReceipt(receipt.id).subscribe({
          next: () => {
            this.loading.set(false);
            if (receipt.status === 'DRAFT') {
              this.notify.success('Draft receipt cancelled.');
            } else {
              this.notify.success('Receipt cancelled and payment reversed successfully.');
            }
            this.loadReceipts();
            this.loadLedger();
            const custId = this.receiptForm.get('customer.id')?.value;
            if (custId) {
              this.loadOutstanding(+custId);
            }
          },
          error: (err) => {
            this.loading.set(false);
            this.notify.error(err.error?.message || 'Failed to cancel receipt');
          }
        });
      }
    });
  }

  approveReceipt(receipt: CustomerReceipt) {
    if (!receipt.id) return;
    if (receipt.status === 'APPROVED') {
      this.notify.error('Receipt is already approved.');
      return;
    }
    if (receipt.status === 'CANCELLED') {
      this.notify.error('Cancelled receipt cannot be approved.');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Approve Customer Receipt',
        message: 'Are you sure you want to approve this receipt? Once approved, accounting entries and invoice allocation will be posted and the receipt cannot be edited.',
        type: 'confirm'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && receipt.id) {
        this.loading.set(true);
        this.paymentMgmtService.approveCustomerReceipt(receipt.id).subscribe({
          next: () => {
            this.loading.set(false);
            this.notify.success('Receipt approved and payment posted successfully.');
            this.loadReceipts();
            this.loadLedger();
            const custId = this.receiptForm.get('customer.id')?.value;
            if (custId) {
              this.loadOutstanding(+custId);
            }
          },
          error: (err) => {
            this.loading.set(false);
            this.notify.error(err.error?.message || 'Failed to approve receipt');
          }
        });
      }
    });
  }

  previewReceipt(receipt: any) {
    if (!receipt || !receipt.id) return;
    this.loading.set(true);
    this.paymentMgmtService.getCustomerReceiptPrintData(receipt.id).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.printData.set(res.data);
          this.showPrintPreview.set(true);
        } else {
          this.notify.error('Failed to load print preview data');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to load print preview');
      }
    });
  }

  printReceipt(receipt: any) {
    if (!receipt || !receipt.id) return;
    this.loading.set(true);
    this.paymentMgmtService.getCustomerReceiptPrintData(receipt.id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.printData.set(res.data);
          
          // Fetch print history to determine print vs reprint
          this.paymentMgmtService.getCustomerReceiptPrintHistory(receipt.id).subscribe({
            next: (histRes) => {
              const hasPrintedBefore = histRes.success && histRes.data && histRes.data.events.some((e: any) => e.format === 'PRINT');
              const eventType = hasPrintedBefore ? 'REPRINT' : 'PRINT';
              
              this.paymentMgmtService.recordCustomerReceiptPrintEvent(receipt.id, eventType, 'PRINT').subscribe({
                next: () => {
                  this.loading.set(false);
                  
                  // Trigger browser print
                  setTimeout(() => {
                    const printContent = document.getElementById('printable-receipt-area');
                    if (printContent) {
                      const printWindow = window.open('about:blank', 'PrintWindow_' + new Date().getTime(), 'left=100,top=100,width=800,height=900');
                      if (printWindow) {
                        printWindow.document.write(`
                          <html>
                            <head>
                              <title>Print Customer Receipt - ${res.data.receiptNumber}</title>
                              <style>
                                body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 20px; color: #111827; }
                                .cancelled-banner { color: #dc2626; font-size: 20px; font-weight: bold; text-align: center; margin-bottom: 20px; text-transform: uppercase; border: 2px solid #dc2626; padding: 10px; }
                                .draft-banner { color: #d97706; font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 20px; border: 2px solid #d97706; padding: 10px; }
                                .header-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                                .header-table td { vertical-align: top; }
                                .title { font-size: 20px; font-weight: bold; color: #1e3a8a; text-align: center; margin: 20px 0; }
                                .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                                .meta-table td { padding: 6px; border: 1px solid #e5e7eb; font-size: 13px; }
                                .meta-table td.label { font-weight: bold; background-color: #f9fafb; width: 25%; }
                                .summary-container { display: flex; justify-content: space-between; margin-bottom: 20px; gap: 15px; }
                                .summary-card { flex: 1; background-color: #f3f4f6; padding: 15px; border-radius: 4px; text-align: center; border: 1px solid #e5e7eb; }
                                .summary-card .label { font-size: 11px; font-weight: bold; color: #4b5563; margin-bottom: 5px; text-transform: uppercase; }
                                .summary-card .value { font-size: 18px; font-weight: bold; color: #1e3a8a; }
                                .allocations-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                                .allocations-table th { background-color: #1e3a8a; color: white; padding: 8px; font-size: 12px; font-weight: bold; border: 1px solid #1e3a8a; }
                                .allocations-table td { padding: 8px; border: 1px solid #e5e7eb; font-size: 12px; }
                                .acct-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                                .acct-table th { background-color: #1e3a8a; color: white; padding: 8px; font-size: 12px; font-weight: bold; border: 1px solid #1e3a8a; }
                                .acct-table td { padding: 8px; border: 1px solid #e5e7eb; font-size: 12px; }
                                .footer-table { width: 100%; border-collapse: collapse; margin-top: 30px; font-size: 12px; border-top: 1px solid #e5e7eb; padding-top: 10px; }
                                .footer-table td { vertical-align: top; padding-top: 10px; }
                                @media print {
                                  body { padding: 0; }
                                }
                              </style>
                            </head>
                            <body>
                              <div class="receipt-container">
                                ${printContent.innerHTML}
                              </div>
                              <script>
                                window.onload = function() {
                                  window.print();
                                  window.close();
                                };
                              </script>
                            </body>
                          </html>
                        `);
                        printWindow.document.close();
                      }
                    }
                  }, 200);
                },
                error: (err) => {
                  this.loading.set(false);
                  this.notify.error('Failed to log print audit record');
                }
              });
            },
            error: () => {
              this.loading.set(false);
              this.notify.error('Failed to fetch print history context');
            }
          });
        } else {
          this.loading.set(false);
          this.notify.error('Failed to load printable data');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to print customer receipt');
      }
    });
  }

  exportReceiptPdf(receipt: any) {
    if (!receipt || !receipt.id) return;
    this.loading.set(true);

    // Fetch history first to determine PDF_EXPORT vs REPRINT
    this.paymentMgmtService.getCustomerReceiptPrintHistory(receipt.id).subscribe({
      next: (histRes) => {
        const hasExportedBefore = histRes.success && histRes.data && histRes.data.events.some((e: any) => e.format === 'PDF');
        const eventType = hasExportedBefore ? 'REPRINT' : 'PDF_EXPORT';

        this.paymentMgmtService.recordCustomerReceiptPrintEvent(receipt.id, eventType, 'PDF').subscribe({
          next: () => {
            this.paymentMgmtService.downloadCustomerReceiptPdf(receipt.id).subscribe({
              next: (blob) => {
                this.loading.set(false);
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = `receipt_${receipt.receiptNumber}.pdf`;
                link.click();
                window.URL.revokeObjectURL(url);
                this.notify.success('PDF file downloaded successfully');
              },
              error: (err) => {
                this.loading.set(false);
                this.notify.error('Failed to download PDF document from server');
              }
            });
          },
          error: (err) => {
            this.loading.set(false);
            this.notify.error('Failed to register PDF export audit trail');
          }
        });
      },
      error: () => {
        this.loading.set(false);
        this.notify.error('Failed to load print history metadata');
      }
    });
  }

  openPrintHistory(receipt: any) {
    if (!receipt || !receipt.id) return;
    this.loading.set(true);
    this.paymentMgmtService.getCustomerReceiptPrintHistory(receipt.id).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.printHistory.set(res.data);
          this.showPrintHistory.set(true);
        } else {
          this.notify.error('Failed to load reprint metrics');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to open reprint history');
      }
    });
  }

  // Phase 37 Dashboard Methods
  toggleDashboard() {
    this.activeTab.set('dashboard');
    this.showDashboard.set(true);
    this.refreshDashboard();
  }

  refreshDashboard() {
    this.loadSettlementDashboard();
  }

  loadSettlementDashboard() {
    this.dashboardLoading.set(true);
    this.dashboardError.set(null);
    this.paymentMgmtService.getReceiptSettlementDashboard().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const d = res.data;
          this.settlementSummary.set(d.summary);
          this.agingSummary.set(d.agingSummary);
          this.customerOutstanding.set(d.topOutstandingCustomers || []);
          this.agingInvoices.set(d.agingInvoices || []);
          this.recentApprovedReceipts.set(d.recentApprovedReceipts || []);
          this.reconciliationRows.set(d.reconciliationSummary || []);
          this.paymentPerformance.set(d.paymentPerformance || []);
        } else {
          this.dashboardError.set(res.message || 'Failed to load dashboard statistics.');
        }
        this.dashboardLoading.set(false);
      },
      error: (err) => {
        this.dashboardError.set(err.message || 'Error occurred while loading dashboard.');
        this.dashboardLoading.set(false);
      }
    });
  }

  loadSettlementSummary() {
    const params: any = {};
    if (this.dashboardFromDateControl.value) params.fromDate = this.dashboardFromDateControl.value;
    if (this.dashboardToDateControl.value) params.toDate = this.dashboardToDateControl.value;

    this.paymentMgmtService.getReceiptDashboardSummary(params).subscribe(res => {
      if (res.success && res.data) {
        this.settlementSummary.set(res.data);
      }
    });
  }

  loadCustomerOutstanding() {
    const params: any = {
      page: this.customerOutstandingPage(),
      size: this.customerOutstandingPageSize()
    };
    const search = this.searchControl.value;
    if (search) params.search = search;

    this.paymentMgmtService.getCustomerOutstandingSummary(params).subscribe(res => {
      if (res.success && res.data) {
        this.customerOutstanding.set(res.data.content);
        this.customerOutstandingTotal.set(res.data.totalElements);
      }
    });
  }

  loadAgingSummary() {
    const params: any = {};
    const custId = this.selectedDashboardCustomer();
    if (custId) params.customerId = custId;

    this.paymentMgmtService.getReceiptAgingSummary(params).subscribe(res => {
      if (res.success && res.data) {
        this.agingSummary.set(res.data);
      }
    });
  }

  loadAgingInvoices() {
    const params: any = {
      page: this.agingInvoicesPage(),
      size: this.agingInvoicesPageSize()
    };
    const custId = this.selectedDashboardCustomer();
    if (custId) params.customerId = custId;
    
    const bucket = this.selectedAgingBucket();
    if (bucket) params.bucket = bucket;

    const search = this.searchControl.value;
    if (search) params.search = search;

    this.paymentMgmtService.getReceiptAgingInvoices(params).subscribe(res => {
      if (res.success && res.data) {
        this.agingInvoices.set(res.data.content);
        this.agingInvoicesTotal.set(res.data.totalElements);
      }
    });
  }

  loadReconciliation() {
    const params: any = {
      page: this.reconciliationPage(),
      size: this.reconciliationPageSize()
    };
    this.paymentMgmtService.getReceiptReconciliation(params).subscribe(res => {
      if (res.success && res.data) {
        this.reconciliationRows.set(res.data.content);
        this.reconciliationTotal.set(res.data.totalElements);
      }
    });
  }

  loadPaymentPerformance() {
    const params: any = {
      page: this.paymentPerformancePage(),
      size: this.paymentPerformancePageSize()
    };
    this.paymentMgmtService.getCustomerPaymentPerformance(params).subscribe(res => {
      if (res.success && res.data) {
        this.paymentPerformance.set(res.data.content);
        this.paymentPerformanceTotal.set(res.data.totalElements);
      }
    });
  }

  openCustomerOutstanding(customerId: number) {
    this.selectedDashboardCustomer.set(customerId);
    this.showCustomerOutstanding.set(true);
    this.loadAgingSummary();
    this.loadAgingInvoices();
  }

  openAgingBucket(bucket: string) {
    this.selectedAgingBucket.set(bucket);
    this.showAgingDetails.set(true);
    this.loadAgingInvoices();
  }

  openReconciliation() {
    this.showReconciliation.set(true);
    this.loadReconciliation();
  }

  applyDashboardFilters() {
    this.loadSettlementSummary();
    this.loadCustomerOutstanding();
    this.loadAgingSummary();
    this.loadAgingInvoices();
    this.loadReconciliation();
    this.loadPaymentPerformance();
  }

  clearDashboardFilters() {
    this.dashboardFromDateControl.setValue('');
    this.dashboardToDateControl.setValue('');
    this.selectedDashboardCustomer.set('');
    this.selectedAgingBucket.set('');
    this.applyDashboardFilters();
  }
}

