import { EntityPhotoComponent } from '../../shared/entity-photo/entity-photo';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MasterService, Driver } from '../../services/master.service';
import {
  DriverMgmtService,
  DriverDocument,
  DriverAttendance,
  DriverSalary,
  DriverPayroll
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
  FfToastComponent
} from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { MaintenanceRequestService, maintenanceRequestError } from '../../services/maintenance-request.service';

@Component({
  selector: 'app-driver-details-console',
  standalone: true,
  imports: [EntityPhotoComponent, ExportButtonsComponent, 
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
  private maintenanceRequests = inject(MaintenanceRequestService);

  loginUserId = signal('');
  loginMessage = signal<string | null>(null);

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
  payrolls = signal<DriverPayroll[]>([]);

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
    { id: 'salary', label: 'Payroll Config', icon: 'payments' },
    { id: 'payrolls', label: 'Salary Slips', icon: 'receipt_long' },
    { id: 'login', label: 'Login', icon: 'link' }
  ];

  editorForm = this.fb.group({
    id: [null as number | null],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    status: ['ACTIVE', [Validators.required]],
    companyId: [this.companyId],
    licenseNumber: ['', [Validators.required]],
    licenseExpiryDate: [''],
    phoneNumber: ['']
  });

  documentForm = this.fb.group({
    docType: ['LICENSE_SCAN', [Validators.required]],
    docNumber: ['', [Validators.required]],
    filePath: [''],
    description: ['']
  });

  attendanceForm = this.fb.group({
    attendanceDate: [new Date().toISOString().split('T')[0], [Validators.required]],
    status: ['PRESENT', [Validators.required]],
    description: ['']
  });

  salaryForm = this.fb.group({
    basicSalary: [0, [Validators.required, Validators.min(0)]],
    overtimeRate: [0, [Validators.required, Validators.min(0)]],
    advanceTaken: [0, [Validators.required, Validators.min(0)]]
  });

  activeCount = computed(() =>
    this.drivers().filter(d => d.status === 'ACTIVE' || d.status === 'AVAILABLE').length
  );
  inactiveCount = computed(() =>
    this.drivers().filter(d => d.status === 'INACTIVE' || d.status === 'SUSPENDED').length
  );

  gridConfig = computed<FfGridConfig>(() => ({
    columns: [
      { field: 'code', header: 'Code' },
      { field: 'name', header: 'Driver Name' },
      { field: 'licenseNumber', header: 'License No' },
      { field: 'phoneNumber', header: 'Mobile' },
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

  payrollGridConfig = computed<FfGridConfig>(() => ({
    columns: [
      { field: 'payrollNumber', header: 'Payroll No' },
      { field: 'payYear', header: 'Year' },
      { field: 'payMonth', header: 'Month' },
      { field: 'totalTrips', header: 'Trips' },
      { field: 'grossAmount', header: 'Gross (₹)', type: 'currency' },
      { field: 'netSalaryPayable', header: 'Net Salary (₹)', type: 'currency' },
      {
        field: 'status',
        header: 'Status',
        type: 'badge',
        badgeMap: {
          DRAFT: { color: 'warning', label: 'Draft' },
          APPROVED: { color: 'info', label: 'Approved' },
          POSTED: { color: 'info', label: 'Posted' },
          PAID: { color: 'success', label: 'Paid' },
          CANCELLED: { color: 'danger', label: 'Cancelled' }
        }
      }
    ],
    rowActions: [
      { id: 'pdf', icon: 'picture_as_pdf', label: 'Salary Slip PDF' },
      { id: 'print', icon: 'print', label: 'Print Salary Slip' }
    ],
    paginated: false,
    emptyMessage: 'No payroll records found for this driver.',
    trackByField: 'id'
  }));

  ngOnInit(): void {
    this.loadDrivers();
    this.loadLookups();
  }

  onHeaderAction(action: FfPageAction): void {
    if (action.id === 'add') this.openAddForm();
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
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

  onPayrollAction(event: FfGridActionEvent<DriverPayroll>): void {
    if (event.action === 'pdf') {
      this.downloadSalarySlipPdf(event.row);
    } else if (event.action === 'print') {
      this.printSalarySlip(event.row);
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
    this.loginUserId.set(driver.appUserId != null ? String(driver.appUserId) : '');
    this.loginMessage.set(null);
    this.opsTab.set('documents');
    this.showOps.set(true);
    if (driver.id) {
      this.loadDocuments(driver.id);
      this.loadAttendance(driver.id);
      this.loadSalary(driver.id);
      this.loadPayrolls(driver.id);
    }
  }

  saveDriverLogin(): void {
    const driver = this.selectedDriver();
    if (!driver?.id) return;
    const raw = this.loginUserId().trim();
    const appUserId = raw === '' ? null : Number(raw);
    if (raw !== '' && !Number.isFinite(appUserId)) {
      this.loginMessage.set('Enter a numeric user id, or leave it blank to unlink.');
      return;
    }
    this.maintenanceRequests.linkDriver(driver.id, appUserId).subscribe({
      next: res => {
        if (!res?.success) {
          this.loginMessage.set(res?.message || 'Unable to update the login link.');
          return;
        }
        const updated = {
          ...driver,
          appUserId: res.data?.appUserId ?? null,
          appUserName: res.data?.appUserName ?? null,
          companyId: res.data?.companyId ?? driver.companyId
        };
        this.selectedDriver.set(updated);
        this.loginMessage.set(appUserId == null ? 'Login unlinked.' : 'Login linked.');
      },
      error: err => this.loginMessage.set(maintenanceRequestError(err))
    });
  }

  closeOps(): void {
    this.showOps.set(false);
    this.selectedDriver.set(null);
    this.showDocEditor.set(false);
    this.showAttendanceEditor.set(false);
    this.showSalaryEditor.set(false);
  }

  private loadDrivers(): void {
    this.loading.set(true);
    this.masterService
      .getMasters<Driver>('drivers', this.companyId, {
        page: this.pageIndex(),
        size: this.pageSize(),
        query: this.searchQuery()
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

  private loadLookups(): void {
    this.masterService.getLookupList(this.companyId, 'DRIVER_STATUS').subscribe((res: any) => {
      if (res.success && res.data) this.statuses.set(res.data);
    });
    this.masterService.getLookupList(this.companyId, 'DRIVER_DOC_TYPE').subscribe((res: any) => {
      if (res.success && res.data) this.docTypes.set(res.data);
    });
    this.masterService.getLookupList(this.companyId, 'ATTENDANCE_STATUS').subscribe((res: any) => {
      if (res.success && res.data) this.attendanceStatuses.set(res.data);
    });
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

  private loadPayrolls(driverId: number): void {
    this.driverMgmt.getPayrollsByDriver(driverId).subscribe(res => {
      if (res.success && res.data) {
        this.payrolls.set(res.data);
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

  downloadSalarySlipPdf(payroll: DriverPayroll): void {
    if (!payroll.id) return;
    this.driverMgmt.downloadSalarySlipPdf(payroll.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Salary_Slip_${payroll.payrollNumber || payroll.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.notify.success('Salary Slip PDF downloaded successfully');
      },
      error: () => this.notify.error('Failed to download Salary Slip PDF')
    });
  }

  printSalarySlip(payroll: DriverPayroll): void {
    if (!payroll.id) return;
    this.driverMgmt.getSalarySlipPrint(payroll.id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const p = res.data;
          const printWindow = window.open('', '_blank', 'width=800,height=900');
          if (printWindow) {
            printWindow.document.write(`
              <html>
                <head>
                  <title>Salary Slip - ${p.payrollNumber || p.payrollId}</title>
                  <style>
                    body { font-family: Arial, sans-serif; padding: 20px; color: #333; }
                    h2 { color: #1e3a8a; margin-bottom: 5px; }
                    .header-table, .data-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    .data-table th, .data-table td { border: 1px solid #ddd; padding: 8px; font-size: 13px; }
                    .data-table th { background-color: #f3f4f6; color: #1e3a8a; text-align: left; }
                    .right { text-align: right; }
                    .total-row { font-weight: bold; background-color: #e5e7eb; }
                    .net-box { background-color: #eff6ff; border: 2px solid #1e3a8a; padding: 15px; text-align: center; font-size: 18px; font-weight: bold; color: #1e3a8a; margin-top: 20px; }
                  </style>
                </head>
                <body>
                  <h2>${p.companyName || 'Transport ERP'}</h2>
                  <p>${p.companyAddress || ''}</p>
                  <hr/>
                  <h3 style="color:#1e3a8a;">DRIVER SALARY SLIP — ${p.payPeriod}</h3>
                  <table class="header-table">
                    <tr><td><strong>Payroll No:</strong> ${p.payrollNumber || p.payrollId}</td><td><strong>Status:</strong> ${p.status}</td></tr>
                    <tr><td><strong>Driver:</strong> ${p.driverName} (${p.driverCode})</td><td><strong>License:</strong> ${p.licenseNumber || 'N/A'}</td></tr>
                  </table>
                  <table class="data-table">
                    <thead>
                      <tr><th>Earnings</th><th class="right">Amount (₹)</th><th>Deductions</th><th class="right">Amount (₹)</th></tr>
                    </thead>
                    <tbody>
                      <tr><td>Basic Salary</td><td class="right">₹${p.basicSalary?.toFixed(2)}</td><td>Deductions</td><td class="right">₹${p.deductionAmount?.toFixed(2)}</td></tr>
                      <tr><td>Allowances</td><td class="right">₹${p.allowanceAmount?.toFixed(2)}</td><td>Advance Recovery</td><td class="right">₹${p.advanceAdjustment?.toFixed(2)}</td></tr>
                      <tr class="total-row"><td>Gross Earnings</td><td class="right">₹${p.grossEarnings?.toFixed(2)}</td><td>Total Deductions</td><td class="right">₹${p.totalDeductions?.toFixed(2)}</td></tr>
                    </tbody>
                  </table>
                  <div class="net-box">NET SALARY PAYABLE: ₹${p.netSalaryPayable?.toFixed(2)}</div>
                  <br/><br/>
                  <p style="font-size:11px; color:#666; text-align:right;">System Generated Document | Printed At: ${new Date().toLocaleString()}</p>
                </body>
              </html>
            `);
            printWindow.document.close();
            printWindow.focus();
            setTimeout(() => printWindow.print(), 250);
          }
        }
      },
      error: () => this.notify.error('Failed to load Salary Slip print data')
    });
  }
}
