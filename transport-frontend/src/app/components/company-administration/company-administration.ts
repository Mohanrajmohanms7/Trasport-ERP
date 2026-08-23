import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CompanyAdminService, Company, Branch, FinancialYear } from '../../services/company-admin.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import {
  FfDropdownComponent,
  FfSelectOption,
  FfTextboxComponent,
  FfNumberComponent,
  FfTextareaComponent,
  FfDatepickerComponent,
  FfCheckboxComponent,
  FfButtonComponent
} from '@ff/ui';

@Component({
  selector: 'app-company-administration',
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
    FfTextareaComponent,
    FfDatepickerComponent,
    FfCheckboxComponent,
    FfButtonComponent
  ],
  templateUrl: './company-administration.html',
  styles: []
})
export class CompanyAdministrationComponent implements OnInit {
  private companyAdminService = inject(CompanyAdminService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);

  // States
  activeTab = signal<string>('profile'); // 'profile' | 'branches' | 'years' | 'settings'
  loading = signal<boolean>(false);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');

  statusOptions: FfSelectOption[] = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Inactive', value: 'INACTIVE' }
  ];
  
  // Editor panels
  showBranchEditor = signal<boolean>(false);
  showYearEditor = signal<boolean>(false);
  editingBranch = signal<Branch | null>(null);
  editingYear = signal<FinancialYear | null>(null);

  // Lists
  branches = signal<Branch[]>([]);
  financialYears = signal<FinancialYear[]>([]);

  // Forms
  companyForm!: FormGroup;
  branchForm!: FormGroup;
  yearForm!: FormGroup;
  settingsForm!: FormGroup;

  ngOnInit() {
    this.initForms();
    this.loadCompany();
    this.loadBranches();
    this.loadFinancialYears();
    this.loadSettings();
  }

  initForms() {
    this.companyForm = this.fb.group({
      code: [{ value: '', disabled: true }],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      gstNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/)]],
      panNumber: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/)]],
      cinNumber: ['', [Validators.maxLength(21)]],
      phone: ['', [Validators.required, Validators.maxLength(20)]],
      email: ['', [Validators.required, Validators.email]],
      website: [''],
      address: ['', Validators.required],
      city: ['', Validators.required],
      state: ['', Validators.required],
      country: ['India', Validators.required],
      pincode: ['', [Validators.required, Validators.maxLength(10)]],
      logo: [''],
      digitalSignature: ['']
    });

    this.branchForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      manager: ['', Validators.required],
      phone: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      gstNumber: [''],
      address: ['', Validators.required],
      latitude: [null],
      longitude: [null],
      status: ['ACTIVE', Validators.required]
    });

    this.yearForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      status: ['ACTIVE', Validators.required],
      isDefault: [false]
    });

    this.settingsForm = this.fb.group({
      // Number series configurations
      vehicleSeries: ['VEH-000', Validators.required],
      customerSeries: ['CUST-000', Validators.required],
      driverSeries: ['DRV-000', Validators.required],
      tripSeries: ['TRIP-000', Validators.required],
      invoiceSeries: ['INV-000', Validators.required],

      // Tax configs
      gstPercentage: ['18', Validators.required],
      taxType: ['GST', Validators.required],

      // Currency
      defaultCurrency: ['INR', Validators.required],

      // Email Preferences
      smtpHost: ['smtp.gmail.com'],
      smtpPort: ['587'],
      smtpUser: ['noreply@transport.com']
    });
  }

  loadCompany() {
    try {
      this.companyAdminService.getCompany().subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.companyForm.patchValue(res.data);
          }
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message || err.message || 'Failed to load company profile.');
        }
      });
    } catch (e: any) {
      this.errorMessage.set(e?.message || 'No company assigned to this session.');
    }
  }

  loadBranches() {
    this.companyAdminService.getBranches().subscribe(res => {
      if (res.success && res.data) {
        this.branches.set(res.data.content || res.data);
      }
    });
  }

  loadFinancialYears() {
    this.companyAdminService.getFinancialYears().subscribe(res => {
      if (res.success && res.data) {
        this.financialYears.set(res.data.content || res.data);
      }
    });
  }

  loadSettings() {
    this.companyAdminService.getSettings().subscribe(res => {
      if (res.success && res.data) {
        this.settingsForm.patchValue(res.data);
      }
    });
  }

  saveCompany() {
    if (this.companyForm.invalid) return;

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const payload = this.companyForm.getRawValue();

    this.companyAdminService.updateCompany(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMessage.set('Company profile updated successfully!');
        this.loadCompany();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to update company profile details.');
      }
    });
  }

  openAddBranch() {
    this.editingBranch.set(null);
    this.branchForm.reset({ status: 'ACTIVE' });
    this.showBranchEditor.set(true);
  }

  openEditBranch(branch: Branch) {
    this.editingBranch.set(branch);
    this.branchForm.patchValue(branch);
    this.showBranchEditor.set(true);
  }

  saveBranch() {
    if (this.branchForm.invalid) return;
    const val = this.branchForm.value;
    const branchObj = this.editingBranch();

    if (branchObj && branchObj.id) {
      this.companyAdminService.updateBranch(branchObj.id, val).subscribe(() => {
        this.loadBranches();
        this.showBranchEditor.set(false);
      });
    } else {
      this.companyAdminService.createBranch({ ...val }).subscribe(() => {
        this.loadBranches();
        this.showBranchEditor.set(false);
      });
    }
  }

  deleteBranch(branch: Branch) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Branch Office',
        message: `Are you sure you want to delete branch office: ${branch.name}? This cannot be undone.`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && branch.id) {
        this.companyAdminService.deleteBranch(branch.id).subscribe(() => this.loadBranches());
      }
    });
  }

  openAddYear() {
    this.editingYear.set(null);
    this.yearForm.reset({ status: 'ACTIVE', isDefault: false });
    this.showYearEditor.set(true);
  }

  openEditYear(fy: FinancialYear) {
    this.editingYear.set(fy);
    this.yearForm.patchValue(fy);
    this.showYearEditor.set(true);
  }

  saveYear() {
    if (this.yearForm.invalid) return;
    const val = this.yearForm.value;
    const yearObj = this.editingYear();

    if (yearObj && yearObj.id) {
      this.companyAdminService.updateFinancialYear(yearObj.id, val).subscribe(() => {
        this.loadFinancialYears();
        this.showYearEditor.set(false);
      });
    } else {
      this.companyAdminService.createFinancialYear({ ...val }).subscribe(() => {
        this.loadFinancialYears();
        this.showYearEditor.set(false);
      });
    }
  }

  saveSettings() {
    if (this.settingsForm.invalid) return;

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const payload = this.settingsForm.value;

    this.companyAdminService.saveSettings(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMessage.set('Business preferences and configurations saved successfully!');
        this.loadSettings();
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Failed to save configuration settings.');
      }
    });
  }
}
