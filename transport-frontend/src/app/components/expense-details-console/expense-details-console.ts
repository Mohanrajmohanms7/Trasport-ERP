import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ExpenseMgmtService, Expense } from '../../services/expense-mgmt.service';
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
  selector: 'app-expense-details-console',
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
  templateUrl: './expense-details-console.html',
  styles: []
})
export class ExpenseDetailsConsoleComponent implements OnInit {
  private expenseMgmtService = inject(ExpenseMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  // States
  activeTab = signal<string>('all'); // 'all' | 'pending'
  loading = signal<boolean>(false);
  showEditor = signal<boolean>(false);
  editingExpense = signal<Expense | null>(null);

  // Lists
  expenses = signal<Expense[]>([]);
  vehicles = signal<any[]>([]);
  drivers = signal<any[]>([]);
  trips = signal<any[]>([]);
  categories = signal<any[]>([]);
  paymentMethods = signal<any[]>([]);

  get categoryOptions(): FfSelectOption[] {
    return this.categories().length > 0
      ? this.categories().map(cat => ({ label: cat.name, value: cat.code }))
      : [{ label: 'TOLL', value: 'TOLL' }, { label: 'DRIVER BATA', value: 'DRIVER_BATA' }, { label: 'PARKING', value: 'PARKING' }, { label: 'VEHICLE REPAIR', value: 'VEHICLE_REPAIR' }, { label: 'INSURANCE', value: 'INSURANCE' }, { label: 'OFFICE', value: 'OFFICE' }, { label: 'MISCELLANEOUS', value: 'MISCELLANEOUS' }];
  }
  get vehicleOptions(): FfSelectOption[] {
    return [{ label: '-- General / Optional --', value: '' }, ...this.vehicles().map(v => ({
      label: [v.code || v.registrationNumber, v.name || [v.brand, v.model].filter(Boolean).join(' ')].filter(Boolean).join(' — ') || 'Unknown Vehicle',
      value: v.id
    }))];
  }
  get driverOptions(): FfSelectOption[] {
    return [{ label: '-- General / Optional --', value: '' }, ...this.drivers().map(driver => ({ label: driver.name, value: driver.id }))];
  }
  get tripOptions(): FfSelectOption[] {
    return [{ label: '-- General / Optional --', value: '' }, ...this.trips().map(trip => ({ label: trip.tripNumber, value: trip.id }))];
  }
  get paymentMethodOptions(): FfSelectOption[] {
    return this.paymentMethods().length > 0
      ? this.paymentMethods().map(pm => ({ label: pm.name, value: pm.code }))
      : [
          { label: 'CASH', value: 'CASH' }, { label: 'UPI WIRE', value: 'UPI' },
          { label: 'BANK TRANSFER', value: 'BANK_TRANSFER' }, { label: 'CHEQUE', value: 'CHEQUE' },
          { label: 'VENDOR CREDIT', value: 'CREDIT' }
        ];
  }

  // Forms
  expenseForm!: FormGroup;

  ngOnInit() {
    this.initForm();
    this.loadExpenses();
    this.loadDropdownData();
  }

  loadDropdownData() {
    this.masterService.getLookupList(this.companyId, 'EXPENSE_TYPE').subscribe(res => {
      if (res.success && res.data) {
        this.categories.set(res.data);
      }
    });
    this.masterService.getLookupList(this.companyId, 'PAYMENT_METHOD').subscribe(res => {
      if (res.success && res.data) {
        this.paymentMethods.set(res.data);
      }
    });
    this.masterService.getMasters<any>('vehicles', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.vehicles.set(res.data.content || res.data);
      }
    });
    this.masterService.getMasters<any>('drivers', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.drivers.set(res.data.content || res.data);
      }
    });
    this.masterService.getMasters<any>('trips', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.trips.set(res.data.content || res.data);
      }
    });
  }

  initForm() {
    this.expenseForm = this.fb.group({
      category: ['TOLL', Validators.required],
      vehicle: this.fb.group({
        id: ['']
      }),
      driver: this.fb.group({
        id: ['']
      }),
      trip: this.fb.group({
        id: ['']
      }),
      description: ['', Validators.required],
      amount: [0, [Validators.required, Validators.min(1)]],
      gstAmount: [0, [Validators.required, Validators.min(0)]],
      paymentMethod: ['CASH', Validators.required],
      remarks: ['']
    });
  }

  loadExpenses() {
    this.loading.set(true);
    const status = this.activeTab() === 'pending' ? 'SUBMITTED' : undefined;
    this.expenseMgmtService.getExpenses({ status }).subscribe(res => {
      if (res.success && res.data) {
        this.expenses.set(res.data.content || res.data);
      }
      this.loading.set(false);
    });
  }

  openAddExpense() {
    this.editingExpense.set(null);
    this.expenseForm.reset({ category: 'TOLL', paymentMethod: 'CASH', amount: 0, gstAmount: 0 });
    this.showEditor.set(true);
  }

  openEditExpense(exp: Expense) {
    this.editingExpense.set(exp);
    this.expenseForm.patchValue({
      category: exp.category,
      vehicle: { id: exp.vehicle?.id || '' },
      driver: { id: exp.driver?.id || '' },
      trip: { id: exp.trip?.id || '' },
      description: exp.description,
      amount: exp.amount,
      gstAmount: exp.gstAmount,
      paymentMethod: exp.paymentMethod,
      remarks: exp.remarks
    });
    this.showEditor.set(true);
  }

  saveExpense() {
    if (this.expenseForm.invalid) return;

    this.loading.set(true);
    const val = this.expenseForm.getRawValue();
    const expObj = this.editingExpense();

    // Clean up associations if empty
    if (!val.vehicle?.id) val.vehicle = null;
    if (!val.driver?.id) val.driver = null;
    if (!val.trip?.id) val.trip = null;

    if (expObj && expObj.id) {
      this.expenseMgmtService.updateExpense(expObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Expense entry updated successfully');
          this.loadExpenses();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to update expense entry');
        }
      });
    } else {
      this.expenseMgmtService.createExpense(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Expense logged successfully');
          this.loadExpenses();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to log expense');
        }
      });
    }
  }

  approveExpense(exp: Expense) {
    if (!exp.id) return;
    this.expenseMgmtService.approveExpense(exp.id).subscribe({
      next: () => {
        this.notify.success('Expense approved successfully');
        this.loadExpenses();
      },
      error: () => this.notify.error('Failed to approve expense')
    });
  }

  rejectExpense(exp: Expense) {
    if (!exp.id) return;
    this.expenseMgmtService.rejectExpense(exp.id).subscribe({
      next: () => {
        this.notify.success('Expense rejected successfully');
        this.loadExpenses();
      },
      error: () => this.notify.error('Failed to reject expense')
    });
  }

  deleteExpense(exp: Expense) {
    if (!exp.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Cancel Expense Entry',
        message: `Are you sure you want to remove expense entry: ${exp.expenseNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && exp.id) {
        this.expenseMgmtService.deleteExpense(exp.id).subscribe({
          next: () => {
            this.notify.success('Expense entry canceled successfully');
            this.loadExpenses();
          },
          error: () => this.notify.error('Failed to cancel expense entry')
        });
      }
    });
  }
}
