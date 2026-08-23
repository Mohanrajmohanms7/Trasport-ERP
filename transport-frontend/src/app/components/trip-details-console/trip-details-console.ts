import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { TripMgmtService, Trip, TripDetail } from '../../services/trip-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';

@Component({
  selector: 'app-trip-details-console',
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
  templateUrl: './trip-details-console.html',
  styles: []
})
export class TripDetailsConsoleComponent implements OnInit {
  private tripMgmtService = inject(TripMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);

  private companyId = resolveTenantCompanyId();

  // Lists
  trips = signal<Trip[]>([]);
  bookings = signal<any[]>([]);
  vehicles = signal<any[]>([]);
  drivers = signal<any[]>([]);
  materials = signal<any[]>([]);

  get bookingOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Booking Reference --', value: '' }, ...this.bookings().map(booking => ({ label: booking.bookingNumber || booking.code, value: booking.id }))];
  }
  get vehicleOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Transit Vehicle --', value: '' }, ...this.vehicles().map(vehicle => ({ label: vehicle.registrationNumber || vehicle.name, value: vehicle.id }))];
  }
  get driverOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Driver Assignment --', value: '' }, ...this.drivers().map(driver => ({ label: driver.name, value: driver.id }))];
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
      remarks: [''],
      details: this.fb.array([])
    });
  }

  get detailsArray(): FormArray {
    return this.tripForm.get('details') as FormArray;
  }

  addDetail() {
    const detailGroup = this.fb.group({
      material: this.fb.group({
        id: ['', Validators.required]
      }),
      quantity: [1, [Validators.required, Validators.min(1)]],
      rate: [0, Validators.required],
      loadingCharges: [0, Validators.required],
      royalty: [0, Validators.required]
    });

    this.detailsArray.push(detailGroup);
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
    this.tripForm.reset();
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
      remarks: trip.remarks
    });

    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }

    if (trip.details) {
      for (const d of trip.details) {
        const row = this.fb.group({
          material: this.fb.group({
            id: [d.material?.id, Validators.required]
          }),
          quantity: [d.quantity, [Validators.required, Validators.min(1)]],
          rate: [d.rate, Validators.required],
          loadingCharges: [d.loadingCharges, Validators.required],
          royalty: [d.royalty, Validators.required]
        });
        this.detailsArray.push(row);
      }
    }

    this.showEditor.set(true);
  }

  saveTrip() {
    if (this.tripForm.invalid) return;

    this.loading.set(true);
    const val = this.tripForm.getRawValue();
    const tripObj = this.editingTrip();

    if (tripObj && tripObj.id) {
      this.tripMgmtService.updateTrip(tripObj.id, val).subscribe(() => {
        this.loading.set(false);
        this.loadTrips();
        this.showEditor.set(false);
      });
    } else {
      this.tripMgmtService.createTrip(val).subscribe(() => {
        this.loading.set(false);
        this.loadTrips();
        this.showEditor.set(false);
      });
    }
  }

  dispatchTrip(trip: Trip) {
    if (!trip.id) return;
    this.tripMgmtService.dispatchTrip(trip.id).subscribe(() => this.loadTrips());
  }

  completeTrip(trip: Trip) {
    if (!trip.id) return;
    this.tripMgmtService.completeTrip(trip.id).subscribe(() => this.loadTrips());
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
        this.tripMgmtService.deleteTrip(trip.id).subscribe(() => this.loadTrips());
      }
    });
  }
}
