import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { MasterService, BaseMaster, LookupValue, Vehicle, Driver, Customer } from '../../services/master.service';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import {
  FfDropdownComponent,
  FfSelectOption,
  FfTextboxComponent,
  FfNumberComponent,
  FfTextareaComponent,
  FfSearchboxComponent,
  FfButtonComponent
} from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-master-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatDialogModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfNumberComponent,
    FfTextareaComponent,
    FfSearchboxComponent,
    FfButtonComponent
  ],
  templateUrl: './master-management.html',
  styleUrls: []
})
export class MasterManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private masterService = inject(MasterService);
  private dialog = inject(MatDialog);
  private route = inject(ActivatedRoute);

  // Use logged-in tenant (never hardcode company 1 for multi-tenant)
  companyId = signal<number>(resolveTenantCompanyId());
  activeTab = signal<string>('vehicle'); // 'vehicle', 'driver', 'customer', 'lookup'
  lookupType = signal<string>('DEPARTMENT'); // For generic lookups

  ownerTypes = signal<any[]>([]);
  statuses = signal<any[]>([]);

  get ownerTypeOptions(): FfSelectOption[] {
    return this.ownerTypes().length > 0
      ? this.ownerTypes().map(o => ({ label: o.name, value: o.code }))
      : [
          { label: 'Self Owned', value: 'SELF' },
          { label: 'Hired Contractor', value: 'HIRED' },
          { label: 'Client Owned', value: 'CLIENT' }
        ];
  }

  get statusOptions(): FfSelectOption[] {
    return this.statuses().length > 0
      ? this.statuses().map(s => ({ label: s.name, value: s.code }))
      : [
          { label: 'Active', value: 'ACTIVE' },
          { label: 'Inactive', value: 'INACTIVE' }
        ];
  }

  // Data signals
  vehicles = signal<Vehicle[]>([]);
  drivers = signal<Driver[]>([]);
  customers = signal<Customer[]>([]);
  lookups = signal<LookupValue[]>([]);

  // Lookup Type Options
  lookupTypes = [
    { value: 'DEPARTMENT', label: 'Department' },
    { value: 'DESIGNATION', label: 'Designation' },
    { value: 'VEHICLE_TYPE', label: 'Vehicle Type' },
    { value: 'VEHICLE_CATEGORY', label: 'Vehicle Category' },
    { value: 'VEHICLE_CAPACITY', label: 'Vehicle Capacity' },
    { value: 'FUEL_TYPE', label: 'Fuel Type' },
    { value: 'EXPENSE_CATEGORY', label: 'Expense Category' },
    { value: 'PAYMENT_METHOD', label: 'Payment Method' },
    { value: 'MATERIAL_UNIT', label: 'Material Unit' },
    { value: 'MATERIAL_CATEGORY', label: 'Material Category' }
  ];

  lookupTypeOptions: FfSelectOption[] = this.lookupTypes.map(t => ({ label: t.label, value: t.value }));
  lookupTypeControl = new FormControl(this.lookupType());

  // Pagination states
  pageSize = signal<number>(5);
  pageIndex = signal<number>(0);
  totalElements = signal<number>(0);
  searchQuery = signal<string>('');

  // Form state
  showEditor = signal<boolean>(false);
  isEditMode = signal<boolean>(false);
  editorForm!: FormGroup;

  // Table Columns
  displayedColumns = computed(() => {
    switch (this.activeTab()) {
      case 'vehicle':
        return ['code', 'name', 'model', 'brand', 'ownerType', 'status', 'actions'];
      case 'driver':
        return ['code', 'name', 'licenseNumber', 'phoneNumber', 'status', 'actions'];
      case 'customer':
        return ['code', 'name', 'email', 'phone', 'creditLimit', 'status', 'actions'];
      case 'lookup':
      default:
        return ['code', 'name', 'description', 'status', 'actions'];
    }
  });

  // Active data source computed for the generic table
  activeData = computed<any[]>(() => {
    switch (this.activeTab()) {
      case 'vehicle':
        return this.vehicles();
      case 'driver':
        return this.drivers();
      case 'customer':
        return this.customers();
      case 'lookup':
      default:
        return this.lookups();
    }
  });

  /** User-friendly labels per master type (avoid generic "Master Code"). */
  entityLabel = computed(() => {
    switch (this.activeTab()) {
      case 'vehicle': return 'Vehicle';
      case 'driver': return 'Driver';
      case 'customer': return 'Customer';
      case 'lookup': return 'Lookup';
      default: return 'Item';
    }
  });

  codeFieldLabel = computed(() => {
    switch (this.activeTab()) {
      case 'vehicle': return 'Vehicle Number / Plate';
      case 'driver': return 'Driver Code';
      case 'customer': return 'Customer Code';
      case 'lookup': return 'Lookup Code';
      default: return 'Code';
    }
  });

  nameFieldLabel = computed(() => {
    switch (this.activeTab()) {
      case 'vehicle': return 'Vehicle Name';
      case 'driver': return 'Driver Name';
      case 'customer': return 'Customer Name';
      case 'lookup': return 'Lookup Name';
      default: return 'Name';
    }
  });

  editorTitle = computed(() =>
    `${this.isEditMode() ? 'Edit' : 'Create'} ${this.entityLabel()}`
  );

  addButtonLabel = computed(() => `Add ${this.entityLabel()}`);

  saveButtonLabel = computed(() => `Save ${this.entityLabel()}`);

  searchPlaceholder = computed(() => {
    switch (this.activeTab()) {
      case 'vehicle': return 'Search plate, name…';
      case 'driver': return 'Search driver code, name…';
      case 'customer': return 'Search customer code, name…';
      default: return 'Search code, name…';
    }
  });

  ngOnInit() {
    this.initForm();
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab === 'vehicle' || tab === 'driver' || tab === 'customer' || tab === 'lookup') {
      this.activeTab.set(tab);
    }
    this.loadData();
    this.loadDropdownData();
    this.lookupTypeControl.valueChanges.subscribe(value => {
      if (value) this.onLookupTypeChange(value);
    });
  }

  loadDropdownData() {
    this.masterService.getLookupList(this.companyId(), 'OWNER_TYPE').subscribe(res => {
      if (res.success && res.data) {
        this.ownerTypes.set(res.data);
      }
    });
    this.masterService.getLookupList(this.companyId(), 'STATUS').subscribe(res => {
      if (res.success && res.data) {
        this.statuses.set(res.data);
      }
    });
  }

  initForm() {
    this.editorForm = this.fb.group({
      id: [null],
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      status: ['ACTIVE', Validators.required],
      companyId: [this.companyId()],

      // Vehicle specific fields
      chassisNumber: [''],
      engineNumber: [''],
      model: [''],
      brand: [''],
      ownerName: [''],
      ownerType: ['SELF'],
      purchaseDate: [null],
      insuranceExpiryDate: [null],
      fitnessExpiryDate: [null],
      permitExpiryDate: [null],

      // Driver specific fields
      licenseNumber: [''],
      licenseExpiryDate: [null],
      phoneNumber: [''],

      // Customer specific
      email: [''],
      phone: [''],
      address: [''],
      gstNumber: [''],
      creditLimit: [0]
    });
  }

  loadData() {
    const search = this.searchQuery();
    const pageParams = {
      page: this.pageIndex(),
      size: this.pageSize(),
      search: search
    };

    const tab = this.activeTab();
    if (tab === 'vehicle') {
      this.masterService.getMasters<Vehicle>('vehicles', this.companyId(), pageParams).subscribe(res => {
        if (res.success) {
          this.vehicles.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
        }
      });
    } else if (tab === 'driver') {
      this.masterService.getMasters<Driver>('drivers', this.companyId(), pageParams).subscribe(res => {
        if (res.success) {
          this.drivers.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
        }
      });
    } else if (tab === 'customer') {
      this.masterService.getMasters<Customer>('customers', this.companyId(), pageParams).subscribe(res => {
        if (res.success) {
          this.customers.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
        }
      });
    } else if (tab === 'lookup') {
      const lookupParams = { ...pageParams, type: this.lookupType() };
      this.masterService.getMasters<LookupValue>('lookups', this.companyId(), lookupParams).subscribe(res => {
        if (res.success) {
          this.lookups.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
        }
      });
    }
  }

  switchTab(tab: string) {
    this.activeTab.set(tab);
    this.pageIndex.set(0);
    this.searchQuery.set('');
    this.showEditor.set(false);
    this.loadData();
  }

  onSearch(term: string) {
    this.searchQuery.set(term);
    this.pageIndex.set(0);
    this.loadData();
  }

  onPageChange(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadData();
  }

  onLookupTypeChange(type: string) {
    this.lookupType.set(type);
    this.pageIndex.set(0);
    this.loadData();
  }

  openAddForm() {
    this.isEditMode.set(false);
    this.initForm();
    this.showEditor.set(true);
  }

  openEditForm(element: any) {
    this.isEditMode.set(true);
    this.initForm();
    this.editorForm.patchValue(element);
    this.showEditor.set(true);
  }

  save() {
    if (this.editorForm.invalid) return;

    let endpoint = 'vehicles';
    const tab = this.activeTab();
    const payload = {
      ...this.editorForm.value,
      companyId: resolveTenantCompanyId()
    };

    if (tab === 'driver') endpoint = 'drivers';
    else if (tab === 'customer') endpoint = 'customers';
    else if (tab === 'lookup') {
      endpoint = 'lookups';
      payload.type = this.lookupType();
    }

    this.masterService.saveMaster(endpoint, payload).subscribe(res => {
      if (res.success) {
        this.showEditor.set(false);
        this.loadData();
      }
    });
  }

  deleteItem(id: number) {
    let endpoint = 'vehicles';
    const tab = this.activeTab();
    if (tab === 'driver') endpoint = 'drivers';
    else if (tab === 'customer') endpoint = 'customers';
    else if (tab === 'lookup') endpoint = 'lookups';

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Record',
        message: 'Are you sure you want to delete this master item? This cannot be undone.',
        confirmText: 'Delete',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.masterService.deleteMaster(endpoint, id).subscribe(res => {
        if (res.success) {
          this.loadData();
        }
      });
    });
  }

  toggleStatus(id: number) {
    let endpoint = 'vehicles';
    const tab = this.activeTab();
    if (tab === 'driver') endpoint = 'drivers';
    else if (tab === 'customer') endpoint = 'customers';
    else if (tab === 'lookup') endpoint = 'lookups';

    this.masterService.toggleStatus(endpoint, id).subscribe(res => {
      if (res.success) {
        this.loadData();
      }
    });
  }
}
