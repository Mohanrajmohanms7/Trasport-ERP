import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { VehicleMgmtService, VehicleDocument, VehicleServiceLog, VehicleDriverAssignment } from '../../services/vehicle-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfTextareaComponent, FfDatepickerComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

@Component({
  selector: 'app-vehicle-details-console',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    MatMenuModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfNumberComponent,
    FfTextareaComponent,
    FfDatepickerComponent,
    FfButtonComponent
  ],
  templateUrl: './vehicle-details-console.html',
  styles: []
})
export class VehicleDetailsConsoleComponent implements OnInit {
  private vehicleMgmtService = inject(VehicleMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  activeTab = signal<string>('documents');
  vehicleId = signal<number | null>(null);
  vehicles = signal<any[]>([]);
  loading = signal<boolean>(false);

  readonly selectedVehicle = computed(() => {
    const id = this.vehicleId();
    return this.vehicles().find(v => v.id === id) || null;
  });

  readonly selectedVehicleLabel = computed(() => {
    const v = this.selectedVehicle();
    if (!v) return 'No vehicle selected';
    return [v.code, v.name].filter(Boolean).join(' — ');
  });

  documents = signal<VehicleDocument[]>([]);
  maintenanceLogs = signal<VehicleServiceLog[]>([]);
  assignments = signal<VehicleDriverAssignment[]>([]);
  drivers = signal<any[]>([]);
  documentTypes = signal<any[]>([]);
  serviceTypes = signal<any[]>([]);

  get vehicleOptions(): FfSelectOption[] {
    return [
      { label: '-- Select Vehicle --', value: '' },
      ...this.vehicles().map(v => ({
        label: [v.code || v.registrationNumber, v.name || [v.brand, v.model].filter(Boolean).join(' ')].filter(Boolean).join(' — ') || 'Unknown Vehicle',
        value: v.id
      }))
    ];
  }

  get documentTypeOptions(): FfSelectOption[] {
    return this.documentTypes().length > 0
      ? this.documentTypes().map(dt => ({ label: dt.name, value: dt.code }))
      : [
          { label: 'INSURANCE', value: 'INSURANCE' }, { label: 'PERMIT', value: 'PERMIT' },
          { label: 'FITNESS', value: 'FITNESS' }, { label: 'PUC (POLLUTION)', value: 'PUC' },
          { label: 'ROAD TAX', value: 'ROAD_TAX' }
        ];
  }

  get serviceTypeOptions(): FfSelectOption[] {
    return this.serviceTypes().length > 0
      ? this.serviceTypes().map(st => ({ label: st.name, value: st.code }))
      : [
          { label: 'OIL CHANGE', value: 'OIL_CHANGE' }, { label: 'ENGINE SERVICE', value: 'ENGINE_SERVICE' },
          { label: 'TYRE CHANGE', value: 'TYRE_CHANGE' }, { label: 'BRAKE SERVICE', value: 'BRAKE_SERVICE' },
          { label: 'GENERAL MAINTENANCE', value: 'GENERAL_SERVICE' }
        ];
  }

  documentForm!: FormGroup;
  maintenanceForm!: FormGroup;
  vehicleSelectForm!: FormGroup;

  showDocEditor = signal<boolean>(false);
  showMaintenanceEditor = signal<boolean>(false);

  ngOnInit() {
    this.initForms();
    this.loadVehicles();
    this.loadDropdownData();
  }

  loadVehicles() {
    this.masterService.getMasters<any>('vehicles', this.companyId, { size: 100, page: 0 }).subscribe(res => {
      if (res.success && res.data) {
        const list = res.data.content || res.data || [];
        this.vehicles.set(list);
        if (list.length && this.vehicleId() == null) {
          this.selectVehicle(list[0].id);
        }
      }
    });
  }

  selectVehicle(id: number | string | null) {
    const vehicleId = id === '' || id == null ? null : Number(id);
    this.vehicleId.set(vehicleId);
    this.vehicleSelectForm.patchValue({ vehicleId: vehicleId ?? '' }, { emitEvent: false });
    this.documents.set([]);
    this.maintenanceLogs.set([]);
    this.assignments.set([]);
    if (vehicleId == null) return;
    this.loadDocuments();
    this.loadMaintenanceHistory();
    this.loadDriverAssignments();
  }

  loadDropdownData() {
    this.masterService.getLookupList(this.companyId, 'VEHICLE_DOCUMENT_TYPE').subscribe(res => {
      if (res.success && res.data) {
        this.documentTypes.set(res.data);
      }
    });
    this.masterService.getLookupList(this.companyId, 'MAINTENANCE_TYPE').subscribe(res => {
      if (res.success && res.data) {
        this.serviceTypes.set(res.data);
      }
    });
    this.masterService.getMasters<any>('drivers', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.drivers.set(res.data.content || res.data);
      }
    });
  }

  initForms() {
    this.vehicleSelectForm = this.fb.group({
      vehicleId: ['']
    });

    this.vehicleSelectForm.get('vehicleId')!.valueChanges.subscribe(value => {
      this.selectVehicle(value);
    });

    this.documentForm = this.fb.group({
      docType: ['INSURANCE', Validators.required],
      docNumber: ['', [Validators.required, Validators.maxLength(50)]],
      expiryDate: ['', Validators.required],
      filePath: ['']
    });

    this.maintenanceForm = this.fb.group({
      serviceType: ['OIL_CHANGE', Validators.required],
      serviceDate: ['', Validators.required],
      nextServiceDate: [''],
      workshop: ['', Validators.required],
      cost: [0, [Validators.required, Validators.min(0)]],
      remarks: ['']
    });
  }

  openDocEditor() {
    if (!this.vehicleId()) return;
    this.showDocEditor.set(true);
  }

  openMaintenanceEditor() {
    if (!this.vehicleId()) return;
    this.showMaintenanceEditor.set(true);
  }

  loadDocuments() {
    const id = this.vehicleId();
    if (id == null) return;
    this.vehicleMgmtService.getDocuments(id).subscribe(res => {
      if (res.success && res.data) {
        this.documents.set(res.data);
      }
    });
  }

  loadMaintenanceHistory() {
    const id = this.vehicleId();
    if (id == null) return;
    this.vehicleMgmtService.getMaintenanceHistory(id).subscribe(res => {
      if (res.success && res.data) {
        this.maintenanceLogs.set(res.data);
      }
    });
  }

  loadDriverAssignments() {
    const id = this.vehicleId();
    if (id == null) return;
    this.vehicleMgmtService.getAssignments(id).subscribe(res => {
      if (res.success && res.data) {
        this.assignments.set(res.data);
      }
    });
  }

  saveDocument() {
    const id = this.vehicleId();
    if (id == null || this.documentForm.invalid) return;

    this.loading.set(true);
    this.vehicleMgmtService.addDocument(id, this.documentForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Document uploaded successfully');
        this.loadDocuments();
        this.showDocEditor.set(false);
        this.documentForm.reset({ docType: 'INSURANCE' });
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to upload document');
      }
    });
  }

  deleteDocument(doc: VehicleDocument) {
    const id = this.vehicleId();
    if (!doc.id || id == null) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Document',
        message: `Are you sure you want to delete this document: ${doc.docNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && doc.id) {
        this.vehicleMgmtService.deleteDocument(id, doc.id).subscribe({
          next: () => {
            this.notify.success('Document deleted successfully');
            this.loadDocuments();
          },
          error: () => this.notify.error('Failed to delete document')
        });
      }
    });
  }

  saveMaintenanceLog() {
    const id = this.vehicleId();
    if (id == null || this.maintenanceForm.invalid) return;

    this.loading.set(true);
    this.vehicleMgmtService.addServiceLog(id, this.maintenanceForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Maintenance log recorded successfully');
        this.loadMaintenanceHistory();
        this.showMaintenanceEditor.set(false);
        this.maintenanceForm.reset({ serviceType: 'OIL_CHANGE', cost: 0 });
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to record maintenance log');
      }
    });
  }

  assignDriver(driverId: number) {
    const id = this.vehicleId();
    if (id == null) return;
    this.vehicleMgmtService.assignDriver(id, driverId).subscribe({
      next: () => {
        this.notify.success('Driver assigned successfully');
        this.loadDriverAssignments();
      },
      error: () => this.notify.error('Failed to assign driver')
    });
  }

  unassignDriver() {
    const id = this.vehicleId();
    if (id == null) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Unassign Driver',
        message: 'Are you sure you want to unassign the current driver from this vehicle?',
        type: 'warning'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.vehicleMgmtService.unassignDriver(id).subscribe({
          next: () => {
            this.notify.success('Driver unassigned successfully');
            this.loadDriverAssignments();
          },
          error: () => this.notify.error('Failed to unassign driver')
        });
      }
    });
  }
}
