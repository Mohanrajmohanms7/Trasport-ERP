import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FuelMgmtService, FuelEntry, FuelRequest } from '../../services/fuel-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-fuel-details-console',
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
  templateUrl: './fuel-details-console.html',
  styles: []
})
export class FuelDetailsConsoleComponent implements OnInit {
  private fuelMgmtService = inject(FuelMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);

  private companyId = resolveTenantCompanyId();

  // States
  activeTab = signal<string>('entries'); // 'entries' | 'requests'
  loading = signal<boolean>(false);
  showEntryForm = signal<boolean>(false);
  showRequestForm = signal<boolean>(false);
  editingEntry = signal<FuelEntry | null>(null);

  // Lists
  fuelEntries = signal<FuelEntry[]>([]);
  fuelRequests = signal<FuelRequest[]>([]);
  vehicles = signal<any[]>([]);
  drivers = signal<any[]>([]);
  trips = signal<any[]>([]);
  paymentMethods = signal<any[]>([]);

  get vehicleOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Vehicle --', value: '' }, ...this.vehicles().map(vehicle => ({ label: vehicle.registrationNumber || vehicle.name, value: vehicle.id }))];
  }
  get driverOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Driver --', value: '' }, ...this.drivers().map(driver => ({ label: driver.name, value: driver.id }))];
  }
  get tripOptions(): FfSelectOption[] {
    return [{ label: '-- Optional Trip Link --', value: '' }, ...this.trips().map(trip => ({ label: trip.tripNumber, value: trip.id }))];
  }
  get requestTripOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Transit Trip --', value: '' }, ...this.trips().map(trip => ({ label: trip.tripNumber, value: trip.id }))];
  }
  get paymentMethodOptions(): FfSelectOption[] {
    return this.paymentMethods().length > 0
      ? this.paymentMethods().map(pm => ({ label: pm.name, value: pm.code }))
      : [
          { label: 'CASH', value: 'CASH' }, { label: 'UPI WIRE', value: 'UPI' },
          { label: 'BANK PAYMENT', value: 'BANK' }, { label: 'VENDOR CREDIT', value: 'CREDIT' }
        ];
  }

  // Forms
  entryForm!: FormGroup;
  requestForm!: FormGroup;

  ngOnInit() {
    this.initForms();
    this.loadFuelEntries();
    this.loadFuelRequests();
    this.loadDropdownData();
  }

  loadDropdownData() {
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

  initForms() {
    this.entryForm = this.fb.group({
      vehicle: this.fb.group({
        id: ['', Validators.required]
      }),
      driver: this.fb.group({
        id: ['', Validators.required]
      }),
      trip: this.fb.group({
        id: ['']
      }),
      fuelStation: ['', [Validators.required, Validators.maxLength(150)]],
      fuelQuantity: [0, [Validators.required, Validators.min(1)]],
      ratePerLitre: [0, [Validators.required, Validators.min(1)]],
      paymentMethod: ['CASH', Validators.required],
      invoiceNumber: [''],
      currentOdometer: [0, [Validators.required, Validators.min(0)]],
      previousOdometer: [0, [Validators.required, Validators.min(0)]],
      remarks: ['']
    });

    this.requestForm = this.fb.group({
      trip: this.fb.group({
        id: ['', Validators.required]
      }),
      requestedQuantity: [0, [Validators.required, Validators.min(1)]],
      requestedAmount: [0, [Validators.required, Validators.min(1)]]
    });
  }

  loadFuelEntries() {
    this.fuelMgmtService.getFuelEntries().subscribe(res => {
      if (res.success && res.data) {
        this.fuelEntries.set(res.data.content || res.data);
      }
    });
  }

  loadFuelRequests() {
    this.fuelMgmtService.getFuelRequests().subscribe(res => {
      if (res.success && res.data) {
        this.fuelRequests.set(res.data.content || res.data);
      }
    });
  }

  openAddEntry() {
    this.editingEntry.set(null);
    this.entryForm.reset({ paymentMethod: 'CASH' });
    this.showEntryForm.set(true);
  }

  openEditEntry(entry: FuelEntry) {
    this.editingEntry.set(entry);
    this.entryForm.patchValue({
      vehicle: { id: entry.vehicle?.id },
      driver: { id: entry.driver?.id },
      trip: { id: entry.trip?.id },
      fuelStation: entry.fuelStation,
      fuelQuantity: entry.fuelQuantity,
      ratePerLitre: entry.ratePerLitre,
      paymentMethod: entry.paymentMethod,
      invoiceNumber: entry.invoiceNumber,
      currentOdometer: entry.currentOdometer,
      previousOdometer: entry.previousOdometer,
      remarks: entry.remarks
    });
    this.showEntryForm.set(true);
  }

  saveEntry() {
    if (this.entryForm.invalid) return;

    this.loading.set(true);
    const val = this.entryForm.getRawValue();
    const entryObj = this.editingEntry();

    if (entryObj && entryObj.id) {
      this.fuelMgmtService.updateFuelEntry(entryObj.id, val).subscribe(() => {
        this.loading.set(false);
        this.loadFuelEntries();
        this.showEntryForm.set(false);
      });
    } else {
      this.fuelMgmtService.createFuelEntry(val).subscribe(() => {
        this.loading.set(false);
        this.loadFuelEntries();
        this.showEntryForm.set(false);
      });
    }
  }

  deleteEntry(entry: FuelEntry) {
    if (!entry.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Fuel Log',
        message: `Are you sure you want to delete fuel entry: ${entry.fuelEntryNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && entry.id) {
        this.fuelMgmtService.deleteFuelEntry(entry.id).subscribe(() => {
          this.loadFuelEntries();
        });
      }
    });
  }

  saveRequest() {
    if (this.requestForm.invalid) return;

    this.loading.set(true);
    const val = this.requestForm.getRawValue();

    this.fuelMgmtService.createFuelRequest(val).subscribe(() => {
      this.loading.set(false);
      this.loadFuelRequests();
      this.showRequestForm.set(false);
      this.requestForm.reset();
    });
  }

  approveRequest(req: FuelRequest) {
    if (!req.id) return;
    this.fuelMgmtService.approveFuelRequest(req.id).subscribe(() => this.loadFuelRequests());
  }

  rejectRequest(req: FuelRequest) {
    if (!req.id) return;
    this.fuelMgmtService.rejectFuelRequest(req.id).subscribe(() => this.loadFuelRequests());
  }
}
