import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  FfAutocompleteComponent,
  FfAvatarComponent,
  FfBadgeComponent,
  FfButtonComponent,
  FfCardComponent,
  FfCheckboxComponent,
  FfChipComponent,
  FfDashboardCardComponent,
  FfDatepickerComponent,
  FfDatetimeComponent,
  FfDialogService,
  FfDropdownComponent,
  FfEmptyStateComponent,
  FfExpansionPanelComponent,
  FfFileUploadComponent,
  FfFilterField,
  FfFilterPanelComponent,
  FfGridComponent,
  FfGridConfig,
  FfImageUploadComponent,
  FfLoadingOverlayComponent,
  FfLoadingService,
  FfNumberComponent,
  FfPageContainerComponent,
  FfPageFooterComponent,
  FfPageHeaderComponent,
  FfPasswordComponent,
  FfRadioComponent,
  FfSearchboxComponent,
  FfSelectOption,
  FfSidebarComponent,
  FfStatusBadgeComponent,
  FfStepItem,
  FfStepperComponent,
  FfSwitchComponent,
  FfTabItem,
  FfTabsComponent,
  FfTextareaComponent,
  FfTextboxComponent,
  FfThemeService,
  FfTimepickerComponent,
  FfNotificationService,
  FfPageAction,
  FfToolbarComponent,
  ffEmail,
  ffRequired
} from '@ff/ui';

interface DemoDriver {
  id: number;
  code: string;
  name: string;
  mobile: string;
  license: string;
  status: string;
}

@Component({
  selector: 'app-ff-playground',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FfPageContainerComponent,
    FfPageHeaderComponent,
    FfToolbarComponent,
    FfFilterPanelComponent,
    FfDashboardCardComponent,
    FfTabsComponent,
    FfStepperComponent,
    FfCardComponent,
    FfExpansionPanelComponent,
    FfSidebarComponent,
    FfEmptyStateComponent,
    FfStatusBadgeComponent,
    FfPageFooterComponent,
    FfLoadingOverlayComponent,
    FfTextboxComponent,
    FfTextareaComponent,
    FfPasswordComponent,
    FfSearchboxComponent,
    FfNumberComponent,
    FfDropdownComponent,
    FfAutocompleteComponent,
    FfRadioComponent,
    FfCheckboxComponent,
    FfSwitchComponent,
    FfDatepickerComponent,
    FfDatetimeComponent,
    FfTimepickerComponent,
    FfFileUploadComponent,
    FfImageUploadComponent,
    FfAvatarComponent,
    FfBadgeComponent,
    FfChipComponent,
    FfButtonComponent,
    FfGridComponent
  ],
  templateUrl: './ff-playground.component.html',
  styleUrl: './ff-playground.component.css'
})
export class FfPlaygroundComponent implements OnInit {
  private fb = inject(FormBuilder);
  private theme = inject(FfThemeService);
  readonly notifySvc = inject(FfNotificationService);
  private dialog = inject(FfDialogService);
  private loading = inject(FfLoadingService);

  submitted = signal('');
  searchLog = signal('');
  sidebarOpen = signal(false);
  activeTab = signal('forms');
  activeStep = signal(0);
  chips = signal(['Heavy Vehicle', 'Night Shift']);
  showEmpty = signal(false);

  statusOptions: FfSelectOption[] = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'On Trip', value: 'ON_TRIP' },
    { label: 'On Leave', value: 'ON_LEAVE' },
    { label: 'Inactive', value: 'INACTIVE' }
  ];

  currencyOptions: FfSelectOption[] = [
    { label: 'INR', value: 'INR' },
    { label: 'USD', value: 'USD' },
    { label: 'EUR', value: 'EUR' },
    { label: 'GBP', value: 'GBP' },
    { label: 'AED', value: 'AED' }
  ];

  vehicleOptions: FfSelectOption[] = [
    { label: 'TN-01-AB-1234 · Tata Prima', value: 1 },
    { label: 'TN-02-CD-5678 · Ashok Leyland', value: 2 },
    { label: 'TN-03-EF-9012 · Eicher Pro', value: 3 }
  ];

  licenseTypeOptions: FfSelectOption[] = [
    { label: 'Heavy Motor Vehicle (HMV)', value: 'HMV' },
    { label: 'Light Motor Vehicle (LMV)', value: 'LMV' },
    { label: 'Transport Vehicle', value: 'TRANSPORT' }
  ];

  tabs: FfTabItem[] = [
    { id: 'forms', label: 'Forms', icon: 'edit_note' },
    { id: 'layout', label: 'Layout', icon: 'dashboard' },
    { id: 'grid', label: 'Grid', icon: 'table' }
  ];

  steps: FfStepItem[] = [
    { id: '1', label: 'Personal' },
    { id: '2', label: 'License' },
    { id: '3', label: 'Vehicle', optional: true }
  ];

  filterFields: FfFilterField[] = [
    { id: 'status', label: 'Status', type: 'select', options: [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' }
    ]},
    { id: 'from', label: 'From Date', type: 'date' },
    { id: 'q', label: 'Search', type: 'text' }
  ];

  form = this.fb.group({
    name: ['', [ffRequired, Validators.maxLength(100)]],
    email: ['', [ffRequired, ffEmail]],
    password: ['', [ffRequired, Validators.minLength(8)]],
    vehicle: [null as number | null, [ffRequired]],
    licenseType: ['HMV', [ffRequired]],
    address: [''],
    licenseExpiry: ['', [ffRequired]],
    joinedAt: [''],
    shiftStart: ['09:00'],
    capacity: [12, [ffRequired]],
    status: ['ACTIVE', [ffRequired]],
    currency: ['INR', [ffRequired]],
    termsAccepted: [false, [ffRequired]],
    isActive: [true]
  });

  drivers = signal<DemoDriver[]>([
    { id: 1, code: 'DR00001', name: 'Rajesh Kumar', mobile: '98765 43210', license: 'TN12 2018 1234567', status: 'ACTIVE' },
    { id: 2, code: 'DR00002', name: 'Suresh Babu', mobile: '98765 11111', license: 'TN12 2019 7654321', status: 'ON_TRIP' },
    { id: 3, code: 'DR00003', name: 'Anand R', mobile: '98765 22222', license: 'TN33 2020 1122334', status: 'ON_LEAVE' },
    { id: 4, code: 'DR00004', name: 'Karthik M', mobile: '98765 33333', license: 'TN09 2017 9988776', status: 'INACTIVE' }
  ]);

  gridConfig: FfGridConfig = {
    columns: [
      { field: 'code', header: 'Driver Code', sortable: true },
      { field: 'name', header: 'Driver Name', type: 'avatar', sortable: true },
      { field: 'mobile', header: 'Mobile Number' },
      { field: 'license', header: 'License Number' },
      {
        field: 'status',
        header: 'Status',
        type: 'badge',
        badgeMap: {
          ACTIVE: { color: 'success', label: 'Active' },
          ON_TRIP: { color: 'info', label: 'On Trip' },
          ON_LEAVE: { color: 'warning', label: 'On Leave' },
          INACTIVE: { color: 'danger', label: 'Inactive' }
        }
      }
    ],
    rowActions: [
      { id: 'view', icon: 'visibility', label: 'View' },
      { id: 'edit', icon: 'edit', label: 'Edit' }
    ],
    paginated: true,
    pageSize: 10,
    totalRecords: 4,
    sortable: true,
    trackByField: 'id'
  };

  ngOnInit(): void {
    this.theme.loadPersisted();
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.notifySvc.error('Please fix validation errors');
      return;
    }
    this.submitted.set(JSON.stringify(this.form.getRawValue(), null, 2));
    this.notifySvc.success('Form submitted successfully');
  }

  toggleTheme(): void {
    this.theme.toggleTheme();
  }

  onHeaderAction(action: FfPageAction): void {
    switch (action.id) {
      case 'theme':
        this.toggleTheme();
        break;
      case 'sidebar':
        this.sidebarOpen.set(true);
        break;
      case 'confirm':
        this.openConfirm();
        break;
    }
  }

  onSearch(term: string): void {
    this.searchLog.set(`Search: "${term}"`);
  }

  openConfirm(): void {
    this.dialog.confirm({
      title: 'Delete Driver',
      message: 'Are you sure you want to delete this driver record? This cannot be undone.',
      confirmText: 'Delete',
      type: 'danger'
    }).subscribe(ok => {
      if (ok) this.notifySvc.success('Confirmed');
    });
  }

  simulateLoading(): void {
    this.loading.show('Saving driver…');
    setTimeout(() => this.loading.hide(), 1500);
  }

  removeChip(i: number): void {
    this.chips.update(list => list.filter((_, idx) => idx !== i));
  }
}
