import { FormValidationDirective } from '../../shared/form-validation.directive';
import { FeatureService } from '../../services/feature.service';
import { AttachmentsPanelComponent } from '../../shared/attachments-panel/attachments-panel';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
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
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

import { PickerService, activeOrSelected, tripLabel } from '../../services/picker.service';
import { QuickCreateComponent, QuickCreateHost, QuickCreateService } from '../../shared/quick-create/quick-create';
import { LookupAddComponent, LookupAddHost, LookupAddService } from '../../shared/lookup-add/lookup-add';
@Component({
  selector: 'app-fuel-details-console',
  standalone: true,
imports: [LookupAddComponent, FormValidationDirective, AttachmentsPanelComponent, ExportButtonsComponent, QuickCreateComponent, 
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
  /** Subscription feature access (hides tabs/buttons not in the client's plan). */
  readonly features = inject(FeatureService);
  private fuelMgmtService = inject(FuelMgmtService);
  private masterService = inject(MasterService);
  private picker = inject(PickerService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

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
  readonly vehiclePick = this.picker.bind('vehicles', this.vehicles);
  drivers = signal<any[]>([]);
  readonly driverPick = this.picker.bind('drivers', this.drivers);
  trips = signal<any[]>([]);
  readonly tripPick = this.picker.bindTrips(this.trips, false);
  readonly qc = inject(QuickCreateService);
  readonly quick = new QuickCreateHost();
  readonly la = inject(LookupAddService);
  readonly lookupAdd = new LookupAddHost();
  newPaymentMethod(text: string): void {
    this.lookupAdd.start('PAYMENT_METHOD', 'payment mode', text, rec => { this.paymentMethods.set([...this.paymentMethods(), rec]); this.entryForm.get('paymentMethod')?.setValue(rec.code); });
  }

  newVehicle(text: string): void {
    this.quick.start('vehicle', text, rec => { PickerService.merge(this.vehicles, [rec]); this.entryForm.get('vehicle.id')?.setValue(rec.id); });
  }
  newDriver(text: string): void {
    this.quick.start('driver', text, rec => { PickerService.merge(this.drivers, [rec]); this.entryForm.get('driver.id')?.setValue(rec.id); });
  }

  paymentMethods = signal<any[]>([]);

  get vehicleOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Vehicle --', value: '' }, ...activeOrSelected(this.vehicles(), [this.entryForm?.getRawValue()?.vehicle?.id, this.requestForm?.getRawValue()?.vehicle?.id]).map(v => ({
      label: [v.code || v.registrationNumber, v.name || [v.brand, v.model].filter(Boolean).join(' ')].filter(Boolean).join(' — ') || 'Unknown Vehicle',
      value: v.id
    }))];
  }
  get driverOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Driver --', value: '' }, ...activeOrSelected(this.drivers(), [this.entryForm?.getRawValue()?.driver?.id, this.requestForm?.getRawValue()?.driver?.id]).map(driver => ({ label: driver.name, value: driver.id }))];
  }
  get tripOptions(): FfSelectOption[] {
    return [{ label: '-- Optional Trip Link --', value: '' }, ...this.trips().filter(t => t.status !== 'CANCELLED' || [this.entryForm?.getRawValue()?.trip?.id, this.requestForm?.getRawValue()?.trip?.id].some(v => String(v) === String(t.id))).map(trip => ({ label: tripLabel(trip), value: trip.id }))];
  }
  get requestTripOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Transit Trip --', value: '' }, ...this.trips().filter(t => t.status !== 'CANCELLED' || [this.entryForm?.getRawValue()?.trip?.id, this.requestForm?.getRawValue()?.trip?.id].some(v => String(v) === String(t.id))).map(trip => ({ label: tripLabel(trip), value: trip.id }))];
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
      this.fuelMgmtService.updateFuelEntry(entryObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Fuel entry updated successfully');
          this.loadFuelEntries();
          this.showEntryForm.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to update fuel entry');
        }
      });
    } else {
      this.fuelMgmtService.createFuelEntry(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Fuel entry logged successfully');
          this.loadFuelEntries();
          this.showEntryForm.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to log fuel entry');
        }
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
        this.fuelMgmtService.deleteFuelEntry(entry.id).subscribe({
          next: () => {
            this.notify.success('Fuel entry deleted successfully');
            this.loadFuelEntries();
          },
          error: () => this.notify.error('Failed to delete fuel entry')
        });
      }
    });
  }

  saveRequest() {
    if (this.requestForm.invalid) return;

    this.loading.set(true);
    const val = this.requestForm.getRawValue();

    this.fuelMgmtService.createFuelRequest(val).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Fuel request submitted successfully');
        this.loadFuelRequests();
        this.showRequestForm.set(false);
        this.requestForm.reset();
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to submit fuel request');
      }
    });
  }

  approveRequest(req: FuelRequest) {
    if (!req.id) return;
    this.fuelMgmtService.approveFuelRequest(req.id).subscribe({
      next: () => {
        this.notify.success('Fuel request approved successfully');
        this.loadFuelRequests();
      },
      error: () => this.notify.error('Failed to approve fuel request')
    });
  }

  rejectRequest(req: FuelRequest) {
    if (!req.id) return;
    this.fuelMgmtService.rejectFuelRequest(req.id).subscribe({
      next: () => {
        this.notify.success('Fuel request rejected successfully');
        this.loadFuelRequests();
      },
      error: () => this.notify.error('Failed to reject fuel request')
    });
  }
}
