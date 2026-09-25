import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TripMgmtService, Trip, TripDetail } from '../../services/trip-mgmt.service';
import { InvoiceMgmtService } from '../../services/invoice-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MaterialMgmtService, LoadingLocation } from '../../services/material-mgmt.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfButtonComponent, FfDatepickerComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';


@Component({
  selector: 'app-trip-details-console',
  standalone: true,
  imports: [ExportButtonsComponent, 
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfNumberComponent,
    FfButtonComponent,
    FfDatepickerComponent
  ],
  templateUrl: './trip-details-console.html',
  styles: []
})
export class TripDetailsConsoleComponent implements OnInit {
  private tripMgmtService = inject(TripMgmtService);
  private invoiceMgmtService = inject(InvoiceMgmtService);
  private router = inject(Router);
  private masterService = inject(MasterService);
  private materialMgmtService = inject(MaterialMgmtService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();


  // Lists
  trips = signal<Trip[]>([]);
  bookings = signal<any[]>([]);
  vehicles = signal<any[]>([]);
  drivers = signal<any[]>([]);
  materials = signal<any[]>([]);
  quarries = signal<any[]>([]);
  loadingLocations = signal<LoadingLocation[]>([]);
  readonly today = new Date().toISOString().slice(0, 10);

  get bookingOptions(): FfSelectOption[] {
    // Only approved bookings can be dispatched; keep the current booking visible while editing.
    const currentBookingId = this.editingTrip()?.booking?.id;
    return [{ label: '-- Choose Booking Reference --', value: '' }, ...this.bookings()
      .filter(booking => booking.status === 'APPROVED' || booking.id === currentBookingId)
      .map(booking => ({ label: [booking.bookingNumber || booking.code, booking.customer?.name].filter(Boolean).join(' — '), value: booking.id }))];
  }
  get vehicleOptions(): FfSelectOption[] {
    const currentVehicleId = this.editingTrip()?.vehicle?.id;
    return [{ label: '-- Choose Transit Vehicle --', value: '' }, ...this.vehicles().map(v => ({
      label: ([v.code || v.registrationNumber, v.name || [v.brand, v.model].filter(Boolean).join(' ')].filter(Boolean).join(' — ') || 'Unknown Vehicle')
        + (v.underMaintenance ? ' (Under maintenance)' : ''),
      value: v.id,
      disabled: !!v.underMaintenance && v.id !== currentVehicleId
    }))];
  }
  get driverOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Driver Assignment --', value: '' }, ...this.drivers().map(driver => ({ label: driver.name, value: driver.id }))];
  }
  get quarryOptions(): FfSelectOption[] {
    return [{ label: '-- Not recorded --', value: '' }, ...this.quarries().map(q => ({ label: q.name, value: q.id }))];
  }
  get loadingLocationOptions(): FfSelectOption[] {
    return [{ label: '-- Not recorded --', value: '' }, ...this.loadingLocations().map(l => ({ label: [l.locationCode, l.loadingPoint].filter(Boolean).join(' — '), value: l.id! }))];
  }
  /** Weighbridge shortage (loaded - delivered) for a trip line form row. */
  getShortage(row: any): number | null {
    const loaded = row.get('loadedQuantity')?.value;
    const delivered = row.get('deliveredQuantity')?.value;
    if (loaded === null || loaded === '' || loaded === undefined || delivered === null || delivered === '' || delivered === undefined) return null;
    return Number(loaded) - Number(delivered);
  }
  /** Material/quantity/vehicle are locked once the truck has delivered; only weighbridge values stay editable. */
  get isCompletedEdit(): boolean {
    return this.editingTrip()?.status === 'COMPLETED';
  }
  get materialOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Material --', value: '' }, ...this.materials().map(material => ({ label: material.name, value: material.id }))];
  }

  // States
  activeTab = signal<string>('list'); // 'list' | 'editor'
  loading = signal<boolean>(false);
  showEditor = signal<boolean>(false);
  editingTrip = signal<Trip | null>(null);

  // Forms
  tripForm!: FormGroup;

  ngOnInit() {
    this.initForm();
    this.loadTrips();
    this.loadDropdownData();
  }

  loadDropdownData() {
    this.masterService.getMasters<any>('bookings', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.bookings.set(res.data.content || res.data);
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
    this.masterService.getMasters<any>('materials', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.materials.set(res.data.content || res.data);
      }
    });
    this.masterService.getMasters<any>('quarries', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.quarries.set((res.data as any).content || res.data);
      }
    });
    this.materialMgmtService.getLocations().subscribe(res => {
      if (res.success && res.data) {
        this.loadingLocations.set(res.data);
      }
    });
  }

  initForm() {
    this.tripForm = this.fb.group({
      booking: this.fb.group({
        id: ['', Validators.required]
      }),
      vehicle: this.fb.group({
        id: ['', Validators.required]
      }),
      driver: this.fb.group({
        id: ['', Validators.required]
      }),
      tripDate: [this.today, Validators.required],
      quarry: this.fb.group({ id: [''] }),
      loadingLocation: this.fb.group({ id: [''] }),
      remarks: [''],
      details: this.fb.array([])
    });
  }

  private buildDetailRow(d?: any) {
    return this.fb.group({
      material: this.fb.group({
        id: [d?.material?.id ?? '', Validators.required]
      }),
      quantity: [d?.quantity ?? 1, [Validators.required, Validators.min(0.01)]],
      loadedQuantity: [d?.loadedQuantity ?? null, Validators.min(0)],
      deliveredQuantity: [d?.deliveredQuantity ?? null, Validators.min(0)],
      rate: [d?.rate ?? 0],
      loadingCharges: [d?.loadingCharges ?? 0, Validators.min(0)],
      royalty: [d?.royalty ?? 0]
    });
  }

  get detailsArray(): FormArray {
    return this.tripForm.get('details') as FormArray;
  }

  addDetail() {
    this.detailsArray.push(this.buildDetailRow());
  }

  removeDetail(index: number) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Remove Line Item',
        message: 'Remove this trip material line?',
        confirmText: 'Remove',
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.detailsArray.removeAt(index);
    });
  }

  loadTrips() {
    this.loading.set(true);
    this.tripMgmtService.getTrips().subscribe(res => {
      if (res.success && res.data) {
        this.trips.set(res.data.content || res.data);
      }
      this.loading.set(false);
    });
  }

  openAddTrip() {
    this.editingTrip.set(null);
    this.tripForm.enable();
    this.tripForm.reset({ tripDate: this.today, quarry: { id: '' }, loadingLocation: { id: '' } });
    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }
    this.addDetail();
    this.showEditor.set(true);
  }

  openEditTrip(trip: Trip) {
    this.editingTrip.set(trip);
    this.tripForm.patchValue({
      booking: { id: trip.booking?.id },
      vehicle: { id: trip.vehicle?.id },
      driver: { id: trip.driver?.id },
      tripDate: trip.tripDate || this.today,
      quarry: { id: trip.quarry?.id ?? '' },
      loadingLocation: { id: trip.loadingLocation?.id ?? '' },
      remarks: trip.remarks
    });

    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }

    if (trip.details) {
      for (const d of trip.details) {
        this.detailsArray.push(this.buildDetailRow(d));
      }
    }

    // After delivery only the weighbridge readings, dates and loading source can be corrected.
    this.tripForm.enable();
    if (trip.status === 'COMPLETED') {
      this.tripForm.get('booking')?.disable();
      this.tripForm.get('vehicle')?.disable();
      this.tripForm.get('driver')?.disable();
      this.detailsArray.controls.forEach(row => {
        row.get('material')?.disable();
        row.get('quantity')?.disable();
      });
    } else {
      this.tripForm.get('booking')?.disable(); // a trip never moves to another booking
    }

    this.showEditor.set(true);
  }

  saveTrip() {
    if (this.tripForm.invalid) return;

    this.loading.set(true);
    const val = this.tripForm.getRawValue();
    const tripObj = this.editingTrip();
    if (!val.quarry?.id) val.quarry = null;
    if (!val.loadingLocation?.id) val.loadingLocation = null;
    for (const d of val.details || []) {
      if (d.loadedQuantity === '' ) d.loadedQuantity = null;
      if (d.deliveredQuantity === '') d.deliveredQuantity = null;
    }

    if (tripObj && tripObj.id) {
      this.tripMgmtService.updateTrip(tripObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Trip updated successfully');
          this.loadTrips();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(this.tripError(err, 'Failed to update trip'));
        }
      });
    } else {
      this.tripMgmtService.createTrip(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Trip created successfully');
          this.loadTrips();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(this.tripError(err, 'Failed to create trip'));
        }
      });
    }
  }

  /** Business errors carry a short title in `message` and the actual reason in `errors[0]`. */
  private tripError(err: any, fallback: string): string {
    const errors = err?.error?.errors;
    const detail = Array.isArray(errors) && errors.length ? String(errors[0]) : '';
    const message = err?.error?.message || '';
    if (message && detail && !detail.startsWith(message)) {
      return `${message}: ${detail}`;
    }
    return detail || message || fallback;
  }

  dispatchTrip(trip: Trip) {
    if (!trip.id) return;
    this.tripMgmtService.dispatchTrip(trip.id).subscribe({
      next: () => {
        this.notify.success('Trip dispatched successfully');
        this.loadTrips();
      },
      error: (err) => this.notify.error(this.tripError(err, 'Failed to dispatch trip'))
    });
  }

  completeTrip(trip: Trip) {
    if (!trip.id) return;
    this.tripMgmtService.completeTrip(trip.id).subscribe({
      next: () => {
        this.notify.success('Trip completed successfully');
        this.loadTrips();
      },
      error: (err) => this.notify.error(this.tripError(err, 'Failed to complete trip'))
    });
  }

  deleteTrip(trip: Trip) {
    if (!trip.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Cancel Trip Itinerary',
        message: `Are you sure you want to cancel planned trip: ${trip.tripNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && trip.id) {
        this.tripMgmtService.deleteTrip(trip.id).subscribe({
          next: () => {
            this.notify.success('Trip canceled successfully');
            this.loadTrips();
          },
          error: (err) => this.notify.error(this.tripError(err, 'Failed to cancel trip'))
        });
      }
    });
  }

  generateInvoice(trip: any) {
    if (!trip.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Generate Invoice',
        message: `Generate tax invoice draft for trip: ${trip.tripNumber}?`,
        confirmText: 'Generate',
        type: 'info'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.invoiceMgmtService.createInvoiceFromTrip(trip.id!).subscribe({
          next: (res) => {
            this.notify.success(res.message || 'Invoice draft generated successfully');
            this.router.navigate(['/billing-invoices']);
          },
          error: (err) => {
            this.notify.error(this.tripError(err, 'Failed to generate invoice'));
          }
        });
      }
    });
  }

  continueInvoice(trip: any) {
    this.router.navigate(['/billing-invoices']);
  }

  viewInvoice(trip: any) {
    this.router.navigate(['/billing-invoices']);
  }
}
