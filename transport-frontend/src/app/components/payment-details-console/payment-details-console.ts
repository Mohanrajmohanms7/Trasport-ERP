import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
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

  // Lists
  receipts = signal<CustomerReceipt[]>([]);
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
          this.selectedCustomerId.set(res.data.content[0].id);
          this.selectedCustomerControl.setValue(res.data.content[0].id, { emitEvent: false });
          this.loadLedger();
        }
      }
    });
    this.masterService.getMasters<any>('bookings', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.bookings.set(res.data.content || res.data);
      }
    });
  }

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
      remarks: ['']
    });
  }

  loadReceipts() {
    this.loading.set(true);
    this.paymentMgmtService.getReceipts().subscribe(res => {
      if (res.success && res.data) {
        this.receipts.set(res.data.content || res.data);
      }
      this.loading.set(false);
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
      this.paymentMgmtService.createReceipt(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Receipt voucher recorded successfully');
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
}
