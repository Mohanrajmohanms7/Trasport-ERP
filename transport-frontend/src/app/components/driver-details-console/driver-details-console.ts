import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MasterService, Driver } from '../../services/master.service';
import {
  DriverMgmtService,
  DriverDocument,
  DriverAttendance,
  DriverSalary
} from '../../services/driver-mgmt.service';
import {
  FfButtonComponent,
  FfCardComponent,
  FfDashboardCardComponent,
  FfDatepickerComponent,
  FfDialogService,
  FfDropdownComponent,
  FfEmptyStateComponent,
  FfGridActionEvent,
  FfGridComponent,
  FfGridConfig,
  FfLoadingOverlayComponent,
  FfLoadingService,
  FfNotificationService,
  FfNumberComponent,
  FfPageAction,
  FfPageContainerComponent,
  FfPageEvent,
  FfPageHeaderComponent,
  FfSearchboxComponent,
  FfSelectOption,
  FfSidebarComponent,
  FfStatusBadgeComponent,
  FfTabItem,
  FfTabsComponent,
  FfTextareaComponent,
  FfTextboxComponent,
  FfToolbarComponent,
  FfToastComponent,
  ffRequired
} from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-driver-details-console',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FfPageContainerComponent,
    FfPageHeaderComponent,
    FfDashboardCardComponent,
    FfToolbarComponent,
    FfSearchboxComponent,
    FfGridComponent,
    FfEmptyStateComponent,
    FfSidebarComponent,
    FfTextboxComponent,
    FfTextareaComponent,
    FfDropdownComponent,
    FfDatepickerComponent,
    FfNumberComponent,
    FfButtonComponent,
    FfTabsComponent,
    FfCardComponent,
    FfStatusBadgeComponent,
    FfLoadingOverlayComponent,
    FfToastComponent
  ],
  templateUrl: './driver-details-console.html',
  styleUrl: './driver-details-console.css'
})
export class DriverDetailsConsoleComponent implements OnInit {
  private masterService = inject(MasterService);
  private driverMgmt = inject(DriverMgmtService);
  private fb = inject(FormBuilder);
  private dialog = inject(FfDialogService);
  private notify = inject(FfNotificationService);
  private loadingSvc = inject(FfLoadingService);

  private companyId = resolveTenantCompanyId();

  drivers = signal<Driver[]>([]);
  loading = signal(false);
  pageIndex = signal(0);
  pageSize = signal(20);
  totalElements = signal(0);
  searchQuery = signal('');

  showEditor = signal(false);
  isEditMode = signal(false);
  saving = signal(false);

  showOps = signal(false);
  selectedDriver = signal<Driver | null>(null);
  opsTab = signal('documents');

  documents = signal<DriverDocument[]>([]);
  attendanceLogs = signal<DriverAttendance[]>([]);
  salaryConfig = signal<DriverSalary | null>(null);

  showDocEditor = signal(false);
  showAttendanceEditor = signal(false);
  showSalaryEditor = signal(false);

  statuses = signal<any[]>([]);
  docTypes = signal<any[]>([]);
  attendanceStatuses = signal<any[]>([]);

  get statusOptions(): FfSelectOption[] {
    return this.statuses().length > 0
      ? this.statuses().map(s => ({ label: s.name, value: s.code }))
      : [
          { label: 'Active', value: 'ACTIVE' },
          { label: 'Inactive', value: 'INACTIVE' },
          { label: 'On Leave', value: 'ON_LEAVE' },
          { label: 'Suspended', value: 'SUSPENDED' }
        ];
  }

  get docTypeOptions(): FfSelectOption[] {
    return this.docTypes().length > 0
      ? this.docTypes().map(dt => ({ label: dt.name, value: dt.code }))
      : [
          { label: 'Driving License', value: 'LICENSE_SCAN' },
          { label: 'Aadhaar Card', value: 'AADHAAR' },
          { label: 'PAN Card', value: 'PAN' },
          { label: 'Vision Certificate', value: 'VISION_CERT' }
        ];
  }

  get attendanceStatusOptions(): FfSelectOption[] {
    return this.attendanceStatuses().length > 0
      ? this.attendanceStatuses().map(as => ({ label: as.name, value: as.code }))
      : [
          { label: 'Present', value: 'PRESENT' },
          { label: 'Absent', value: 'ABSENT' },
          { label: 'Leave', value: 'LEAVE' },
          { label: 'Half Day', value: 'HALF_DAY' }
        ];
  }

  opsTabs: FfTabItem[] = [
    { id: 'documents', label: 'Documents', icon: 'description' },
    { id: 'attendance', label: 'Attendance', icon: 'event_available' },
    { id: 'salary', label: 'Payroll', icon: 'payments' }
  ];

  editorForm = this.fb.group({
    id: [null as number | null],
    code: ['', [ffRequired, Validators.maxLength(50)]],
    name: ['', [ffRequired, Validators.maxLength(150)]],
    description: [''],
    status: ['ACTIVE', [ffRequired]],
    companyId: [this.companyId],
    licenseNumber: ['', [ffRequired, Validators.maxLength(50)]],
    licenseExpiryDate: [''],
    phoneNumber: ['', [Validators.maxLength(20)]]
  });

  documentForm = this.fb.group({
    docType: ['LICENSE_SCAN', [ffRequired]],
    docNumber: ['', [ffRequired, Validators.maxLength(50)]],
    filePath: ['']
  });

  attendanceForm = this.fb.group({
    attendanceDate: ['', [ffRequired]],
    status: ['PRESENT', [ffRequired]],
    description: ['']
  });

  salaryForm = this.fb.group({
    basicSalary: [0, [ffRequired, Validators.min(0)]],
    overtimeRate: [0, [ffRequired, Validators.min(0)]],
    advanceTaken: [0, [ffRequired, Validators.min(0)]]
  });

  activeCount = computed(() =>
    this.drivers().filter(d => {
      const s = (d.status || '').toUpperCase();
      return s === 'ACTIVE' || s === 'AVAILABLE';
    }).length
  );
  inactiveCount = computed(() =>
    this.drivers().filter(d => {
      const s = (d.status || '').toUpperCase();
      return s !== 'ACTIVE' && s !== 'AVAILABLE';
    }).length
  );

  gridConfig = computed<FfGridConfig>(() => ({
    columns: [
      { field: 'code', header: 'Driver Code', sortable: true, width: '120px' },
      { field: 'name', header: 'Driver Name', type: 'avatar', sortable: true },
      { field: 'licenseNumber', header: 'License Number' },
      { field: 'phoneNumber', header: 'Mobile' },
      { field: 'licenseExpiryDate', header: 'License Expiry', type: 'date' },
      {
        field: 'status',
        header: 'Status',
        type: 'badge',
        badgeMap: {
          ACTIVE: { color: 'success', label: 'Active' },
          AVAILABLE: { color: 'success', label: 'Available' },
          INACTIVE: { color: 'danger', label: 'Inactive' },
          ON_LEAVE: { color: 'warning', label: 'On Leave' },
          SUSPENDED: { color: 'danger', label: 'Suspended' }
        }
      }
    ],
    rowActions: [
      { id: 'ops', icon: 'folder_managed', label: 'Operations' },
      { id: 'edit', icon: 'edit', label: 'Edit' },
      { id: 'delete', icon: 'delete', label: 'Delete' }
    ],
    paginated: true,
    pageSize: this.pageSize(),
    pageIndex: this.pageIndex(),
    totalRecords: this.totalElements(),
    sortable: true,
    stickyHeader: true,
    trackByField: 'id',
    emptyMessage: 'No drivers found. Add your first driver to get started.'
  }));

  headerActions: FfPageAction[] = [
    { id: 'add', label: 'Add Driver', icon: 'person_add', variant: 'primary' }
  ];

  docGridConfig = computed<FfGridConfig>(() => ({
    columns: [
      { field: 'docType', header: 'Type' },
      { field: 'docNumber', header: 'Number' },
      { field: 'filePath', header: 'File Path' }
    ],
    rowActions: [{ id: 'delete', icon: 'delete', label: 'Delete' }],
    paginated: false,
    emptyMessage: 'No documents registered.',
    trackByField: 'id'
  }));

  attendanceGridConfig = computed<FfGridConfig>(() => ({
    columns: [
      { field: 'attendanceDate', header: 'Date', type: 'date' },
      {
        field: 'status',
        header: 'Status',
        type: 'badge',
        badgeMap: {
          PRESENT: { color: 'success', label: 'Present' },
          ABSENT: { color: 'danger', label: 'Absent' },
          LEAVE: { color: 'info', label: 'Leave' },
          HALF_DAY: { color: 'warning', label: 'Half Day' }
        }
      },
      { field: 'description', header: 'Notes' }
    ],
    paginated: false,
    emptyMessage: 'No attendance logs.',
    trackByField: 'id'
  }));

  ngOnInit(): void {
    this.loadDrivers();
    this.loadDropdownData();
  }

  loadDropdownData(): void {
    this.masterService.getLookupList(this.companyId, 'DRIVER_STATUS').subscribe(res => {
      if (res.success && res.data) this.statuses.set(res.data);
    });
    this.masterService.getLookupList(this.companyId, 'DRIVER_DOCUMENT_TYPE').subscribe(res => {
      if (res.success && res.data) this.docTypes.set(res.data);
    });
    this.masterService.getLookupList(this.companyId, 'ATTENDANCE_STATUS').subscribe(res => {
      if (res.success && res.data) this.attendanceStatuses.set(res.data);
    });
  }

  loadDrivers(): void {
    this.loading.set(true);
    this.masterService
      .getMasters<Driver>('drivers', this.companyId, {
        page: this.pageIndex(),
        size: this.pageSize(),
        search: this.searchQuery() || undefined
      })
      .subscribe({
        next: res => {
          this.loading.set(false);
          if (res.success && res.data) {
            this.drivers.set(res.data.content);
            this.totalElements.set(res.data.totalElements);
          }
        },
        error: () => {
          this.loading.set(false);
          this.notify.error('Failed to load drivers');
        }
      });
  }

  onHeaderAction(action: FfPageAction): void {
    if (action.id === 'add') this.openAddForm();
  }

  onSearch(term: string): void {
    this.searchQuery.set(term);
    this.pageIndex.set(0);
    this.loadDrivers();
  }

  onPageChange(event: FfPageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadDrivers();
  }

  onGridAction(event: FfGridActionEvent<Driver>): void {
    switch (event.action) {
      case 'edit':
        this.openEditForm(event.row);
        break;
      case 'delete':
        this.deleteDriver(event.row);
        break;
      case 'ops':
        this.openOps(event.row);
        break;
    }
  }

  openAddForm(): void {
    this.isEditMode.set(false);
    this.editorForm.reset({
      id: null,
      code: '',
      name: '',
      description: '',
      status: 'ACTIVE',
      companyId: this.companyId,
      licenseNumber: '',
      licenseExpiryDate: '',
      phoneNumber: ''
    });
    this.showEditor.set(true);
  }

  openEditForm(driver: Driver): void {
    this.isEditMode.set(true);
    this.editorForm.patchValue({
      id: driver.id ?? null,
      code: driver.code,
      name: driver.name,
      description: driver.description ?? '',
      status: driver.status,
      companyId: driver.companyId ?? this.companyId,
      licenseNumber: driver.licenseNumber,
      licenseExpiryDate: driver.licenseExpiryDate ?? '',
      phoneNumber: driver.phoneNumber ?? ''
    });
    this.showEditor.set(true);
  }

  saveDriver(): void {
    this.editorForm.markAllAsTouched();
    if (this.editorForm.invalid) {
      this.notify.error('Please fix validation errors');
      return;
    }

    this.saving.set(true);
    this.loadingSvc.show('Saving driver…');
    const payload = this.editorForm.getRawValue();

    this.masterService.saveMaster<Driver>('drivers', payload).subscribe({
      next: res => {
        this.saving.set(false);
        this.loadingSvc.hide();
        if (res.success) {
          this.notify.success(this.isEditMode() ? 'Driver updated' : 'Driver created');
          this.showEditor.set(false);
          this.loadDrivers();
        } else {
          this.notify.error(res.message || 'Save failed');
        }
      },
      error: () => {
        this.saving.set(false);
        this.loadingSvc.hide();
        this.notify.error('Failed to save driver');
      }
    });
  }

  deleteDriver(driver: Driver): void {
    if (!driver.id) return;
    this.dialog
      .confirm({
        title: 'Delete Driver',
        message: `Delete driver "${driver.name}" (${driver.code})? This cannot be undone.`,
        confirmText: 'Delete',
        type: 'danger'
      })
      .subscribe(ok => {
        if (!ok || !driver.id) return;
        this.loadingSvc.show('Deleting…');
        this.masterService.deleteMaster('drivers', driver.id).subscribe({
          next: res => {
            this.loadingSvc.hide();
            if (res.success) {
              this.notify.success('Driver deleted');
              this.loadDrivers();
            } else {
              this.notify.error(res.message || 'Delete failed');
            }
          },
          error: () => {
            this.loadingSvc.hide();
            this.notify.error('Failed to delete driver');
          }
        });
      });
  }

  openOps(driver: Driver): void {
    this.selectedDriver.set(driver);
    this.opsTab.set('documents');
    this.showOps.set(true);
    if (driver.id) {
      this.loadDocuments(driver.id);
      this.loadAttendance(driver.id);
      this.loadSalary(driver.id);
    }
  }

  closeOps(): void {
    this.showOps.set(false);
    this.selectedDriver.set(null);
    this.showDocEditor.set(false);
    this.showAttendanceEditor.set(false);
    this.showSalaryEditor.set(false);
  }

  private loadDocuments(driverId: number): void {
    this.driverMgmt.getDocuments(driverId).subscribe(res => {
      if (res.success && res.data) this.documents.set(res.data);
    });
  }

  private loadAttendance(driverId: number): void {
    this.driverMgmt.getAttendance(driverId).subscribe(res => {
      if (res.success && res.data) this.attendanceLogs.set(res.data);
    });
  }

  private loadSalary(driverId: number): void {
    this.driverMgmt.getSalary(driverId).subscribe(res => {
      if (res.success && res.data) {
        this.salaryConfig.set(res.data);
        this.salaryForm.patchValue(res.data);
      } else {
        this.salaryConfig.set(null);
      }
    });
  }

  onDocAction(event: FfGridActionEvent<DriverDocument>): void {
    if (event.action === 'delete') this.deleteDocument(event.row);
  }

  saveDocument(): void {
    this.documentForm.markAllAsTouched();
    const driver = this.selectedDriver();
    if (this.documentForm.invalid || !driver?.id) return;

    this.driverMgmt.addDocument(driver.id, this.documentForm.getRawValue() as DriverDocument).subscribe({
      next: () => {
        this.notify.success('Document saved');
        this.showDocEditor.set(false);
        this.documentForm.reset({ docType: 'LICENSE_SCAN' });
        this.loadDocuments(driver.id!);
      },
      error: () => this.notify.error('Failed to save document')
    });
  }

  deleteDocument(doc: DriverDocument): void {
    const driver = this.selectedDriver();
    if (!doc.id || !driver?.id) return;

    this.dialog
      .confirm({
        title: 'Delete Document',
        message: `Delete document ${doc.docNumber}?`,
        confirmText: 'Delete',
        type: 'danger'
      })
      .subscribe(ok => {
        if (!ok) return;
        this.driverMgmt.deleteDocument(driver.id!, doc.id!).subscribe({
          next: () => {
            this.notify.success('Document deleted');
            this.loadDocuments(driver.id!);
          },
          error: () => this.notify.error('Failed to delete document')
        });
      });
  }

  saveAttendance(): void {
    this.attendanceForm.markAllAsTouched();
    const driver = this.selectedDriver();
    if (this.attendanceForm.invalid || !driver?.id) return;

    this.driverMgmt.logAttendance(driver.id, this.attendanceForm.getRawValue() as DriverAttendance).subscribe({
      next: () => {
        this.notify.success('Attendance logged');
        this.showAttendanceEditor.set(false);
        this.attendanceForm.reset({ status: 'PRESENT' });
        this.loadAttendance(driver.id!);
      },
      error: () => this.notify.error('Failed to log attendance')
    });
  }

  saveSalary(): void {
    this.salaryForm.markAllAsTouched();
    const driver = this.selectedDriver();
    if (this.salaryForm.invalid || !driver?.id) return;

    this.driverMgmt.saveSalary(driver.id, this.salaryForm.getRawValue() as DriverSalary).subscribe({
      next: () => {
        this.notify.success('Payroll updated');
        this.showSalaryEditor.set(false);
        this.loadSalary(driver.id!);
      },
      error: () => this.notify.error('Failed to save payroll')
    });
  }
}
