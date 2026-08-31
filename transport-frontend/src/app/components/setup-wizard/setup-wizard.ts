import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CompanyAdminService } from '../../services/company-admin.service';
import { MasterService } from '../../services/master.service';
import { SetupService } from '../../services/setup.service';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfDatepickerComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-setup-wizard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatStepperModule,
    MatButtonModule,
    MatIconModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfNumberComponent,
    FfDatepickerComponent
  ],
  templateUrl: './setup-wizard.html',
  styles: []
})
export class SetupWizardComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private companyService = inject(CompanyAdminService);
  private masterService = inject(MasterService);
  private setupService = inject(SetupService);

  loading = signal<boolean>(false);
  errorMessage = signal<string>('');
  savedSteps = signal<Record<string, boolean>>({});

  ownerTypeOptions: FfSelectOption[] = [
    { label: 'Own', value: 'SELF' },
    { label: 'Hired', value: 'HIRED' },
    { label: 'Client', value: 'CLIENT' }
  ];

  private companyId = resolveTenantCompanyId();

  companyForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    gstNumber: [''],
    panNumber: [''],
    phone: [''],
    email: ['', [Validators.email]],
    address: [''],
    city: [''],
    state: [''],
    country: ['India'],
    pincode: ['']
  });

  branchForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    manager: [''],
    phone: [''],
    email: ['', [Validators.email]],
    address: ['']
  });

  vehicleForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    brand: [''],
    model: [''],
    ownerName: [''],
    ownerType: ['SELF']
  });

  driverForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    licenseNumber: ['', [Validators.required, Validators.maxLength(50)]],
    licenseExpiryDate: [''],
    phoneNumber: ['']
  });

  customerForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    phone: [''],
    email: ['', [Validators.email]],
    gstNumber: [''],
    address: [''],
    creditLimit: [0]
  });

  materialForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    defaultRate: [0],
    density: [1.0],
    description: ['']
  });

  ngOnInit() {
    this.loadCompany();
  }

  private loadCompany() {
    this.companyService.getCompany().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.companyForm.patchValue(res.data);
        }
      },
      error: () => {
        // A missing company profile is not fatal - the user can fill it in here.
      }
    });
  }

  saveCompany() {
    if (this.companyForm.invalid) return;
    this.runSave('company', this.companyService.updateCompany({
      ...this.companyForm.value,
      id: this.companyId,
      status: 'ACTIVE'
    }));
  }

  saveBranch() {
    if (this.branchForm.invalid) return;
    this.runSave('branch', this.companyService.createBranch({
      ...this.branchForm.value,
      status: 'ACTIVE',
      companyId: this.companyId
    }));
  }

  saveVehicle() {
    if (this.vehicleForm.invalid) return;
    this.runSave('vehicle', this.masterService.saveMaster('vehicles', {
      ...this.vehicleForm.value,
      status: 'ACTIVE',
      companyId: this.companyId
    }));
  }

  saveDriver() {
    if (this.driverForm.invalid) return;
    this.runSave('driver', this.masterService.saveMaster('drivers', {
      ...this.driverForm.value,
      status: 'ACTIVE',
      companyId: this.companyId
    }));
  }

  saveCustomer() {
    if (this.customerForm.invalid) return;
    this.runSave('customer', this.masterService.saveMaster('customers', {
      ...this.customerForm.value,
      status: 'ACTIVE',
      companyId: this.companyId
    }));
  }

  saveMaterial() {
    if (this.materialForm.invalid) return;
    this.runSave('material', this.masterService.saveMaster('materials', {
      ...this.materialForm.value,
      status: 'ACTIVE',
      companyId: this.companyId
    }));
  }

  private runSave(step: string, request: any) {
    this.loading.set(true);
    this.errorMessage.set('');

    request.subscribe({
      next: (res: any) => {
        this.loading.set(false);
        if (res.success) {
          this.savedSteps.update(steps => ({ ...steps, [step]: true }));
        } else {
          this.errorMessage.set(res.message || 'Could not save this step.');
        }
      },
      error: (err: any) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Could not save this step. Please check the values and try again.');
      }
    });
  }

  isSaved(step: string): boolean {
    return !!this.savedSteps()[step];
  }

  finishSetup() {
    this.loading.set(true);
    this.errorMessage.set('');

    this.setupService.completeSetup().subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Could not finish setup.');
      }
    });
  }

  seedSupportingData() {
    this.loading.set(true);
    this.errorMessage.set('');

    this.setupService.seedSupportingExampleData().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          const msg = (res.data?.['message'] as string)
            || res.message
            || 'Supporting example data loaded. Create 1 Customer, 1 Vehicle, 1 Driver next.';
          this.errorMessage.set('');
          alert(msg);
        } else {
          this.errorMessage.set(res.message || 'Could not load supporting data.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || err.error?.errors?.[0] || 'Could not load supporting data.');
      }
    });
  }

  skipSetup() {

    // Mark setup complete so setupGuard does not bounce the user back to /setup
    this.finishSetup();
  }
}

