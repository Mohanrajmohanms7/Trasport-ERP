import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AccountsMgmtService, ChartOfAccount, JournalVoucher } from '../../services/accounts-mgmt.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfButtonComponent } from '@ff/ui';

@Component({
  selector: 'app-accounts-details-console',
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
  templateUrl: './accounts-details-console.html',
  styles: []
})
export class AccountsDetailsConsoleComponent implements OnInit {
  private accountsMgmtService = inject(AccountsMgmtService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);

  // States
  activeTab = signal<string>('chart'); // 'chart' | 'journal' | 'trial'
  loading = signal<boolean>(false);
  showAccountEditor = signal<boolean>(false);
  showJournalEditor = signal<boolean>(false);
  editingAccount = signal<ChartOfAccount | null>(null);

  // Lists
  accounts = signal<ChartOfAccount[]>([]);
  vouchers = signal<JournalVoucher[]>([]);
  accountTypes = signal<string[]>([
    'ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE'
  ]);
  get accountTypeOptions(): FfSelectOption[] {
    return this.accountTypes().map(type => ({ label: type, value: type }));
  }
  get accountOptions(): FfSelectOption[] {
    return [
      { label: '-- Choose Account --', value: '' },
      ...this.accounts().map(acc => ({ label: `${acc.accountName} (${acc.accountCode})`, value: acc.id ?? '' }))
    ];
  }

  // Forms
  accountForm!: FormGroup;
  journalForm!: FormGroup;

  ngOnInit() {
    this.initForms();
    this.loadAccounts();
    this.loadVouchers();
  }

  initForms() {
    this.accountForm = this.fb.group({
      accountCode: ['', [Validators.required, Validators.pattern('[a-zA-Z0-9-_]+')]],
      accountName: ['', Validators.required],
      accountType: ['ASSET', Validators.required],
      openingBalance: [0, [Validators.required, Validators.min(0)]]
    });

    this.journalForm = this.fb.group({
      debitAccount: this.fb.group({
        id: ['', Validators.required]
      }),
      creditAccount: this.fb.group({
        id: ['', Validators.required]
      }),
      amount: [0, [Validators.required, Validators.min(1)]],
      referenceNumber: [''],
      description: ['', Validators.required]
    });
  }

  loadAccounts() {
    this.loading.set(true);
    this.accountsMgmtService.getAccounts().subscribe(res => {
      if (res.success && res.data) {
        this.accounts.set(res.data.content || res.data);
      }
      this.loading.set(false);
    });
  }

  loadVouchers() {
    this.accountsMgmtService.getVouchers().subscribe(res => {
      if (res.success && res.data) {
        this.vouchers.set(res.data.content || res.data);
      }
    });
  }

  openAddAccount() {
    this.editingAccount.set(null);
    this.accountForm.reset({ accountType: 'ASSET', openingBalance: 0 });
    this.showAccountEditor.set(true);
  }

  openEditAccount(acc: ChartOfAccount) {
    this.editingAccount.set(acc);
    this.accountForm.patchValue({
      accountCode: acc.accountCode,
      accountName: acc.accountName,
      accountType: acc.accountType,
      openingBalance: acc.openingBalance
    });
    this.showAccountEditor.set(true);
  }

  openAddJournal() {
    this.journalForm.reset({ amount: 0 });
    this.showJournalEditor.set(true);
  }

  saveAccount() {
    if (this.accountForm.invalid) return;

    this.loading.set(true);
    const val = this.accountForm.getRawValue();
    const accObj = this.editingAccount();

    if (accObj && accObj.id) {
      this.accountsMgmtService.updateAccount(accObj.id, val).subscribe(() => {
        this.loading.set(false);
        this.loadAccounts();
        this.showAccountEditor.set(false);
      });
    } else {
      this.accountsMgmtService.createAccount(val).subscribe(() => {
        this.loading.set(false);
        this.loadAccounts();
        this.showAccountEditor.set(false);
      });
    }
  }

  saveJournal() {
    if (this.journalForm.invalid) return;

    this.loading.set(true);
    const val = this.journalForm.getRawValue();

    this.accountsMgmtService.createVoucher(val).subscribe(() => {
      this.loading.set(false);
      this.loadVouchers();
      this.loadAccounts();
      this.showJournalEditor.set(false);
    });
  }

  deleteAccount(acc: ChartOfAccount) {
    if (!acc.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Ledger Account',
        message: `Are you sure you want to remove account ledger: ${acc.accountName}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && acc.id) {
        this.accountsMgmtService.deleteAccount(acc.id).subscribe(() => {
          this.loadAccounts();
        });
      }
    });
  }

  // Get total debits / credits for Trial Balance calculation checks
  get totalAssetsExpenses(): number {
    return this.accounts()
      .filter(a => a.accountType === 'ASSET' || a.accountType === 'EXPENSE')
      .reduce((sum, a) => sum + (a.runningBalance || 0), 0);
  }

  get totalLiabilitiesEquityRevenue(): number {
    return this.accounts()
      .filter(a => a.accountType === 'LIABILITY' || a.accountType === 'EQUITY' || a.accountType === 'INCOME')
      .reduce((sum, a) => sum + (a.runningBalance || 0), 0);
  }
}
