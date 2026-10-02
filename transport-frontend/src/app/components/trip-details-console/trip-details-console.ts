import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TripMgmtService, Trip, TripDetail, BookingTripBalance, BookingTripBalanceLine } from '../../services/trip-mgmt.service';
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
import { uomLabel, orderLineText } from '../../shared/uom-label';


import { PickerService, activeOrSelected, tripLabel } from '../../services/picker.service';
import { QuickCreateComponent, QuickCreateHost, QuickCreateService } from '../../shared/quick-create/quick-create';
@Component({
  selector: 'app-trip-details-console',
  standalone: true,
  imports: [QuickCreateComponent, ExportButtonsComponent, 
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
  private picker = inject(PickerService);
  private materialMgmtService = inject(MaterialMgmtService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();


  // Lists
  trips = signal<Trip[]>([]);
  bookings = signal<any[]>([]);
  vehicles = signal<any[]>([]);
  readonly vehiclePick = this.picker.bind('vehicles', this.vehicles);
  drivers = signal<any[]>([]);
  readonly driverPick = this.picker.bind('drivers', this.drivers);
  materials = signal<any[]>([]);
  quarries = signal<any[]>([]);
  readonly quarryPick = this.picker.bind('quarries', this.quarries);
  readonly qc = inject(QuickCreateService);
  readonly quick = new QuickCreateHost();
  newVehicle(text: string): void {
    this.quick.start('vehicle', text, rec => { PickerService.merge(this.vehicles, [rec]); this.tripForm.get('vehicle.id')?.setValue(rec.id); });
  }
  newDriver(text: string): void {
    this.quick.start('driver', text, rec => { PickerService.merge(this.drivers, [rec]); this.tripForm.get('driver.id')?.setValue(rec.id); });
  }

  loadingLocations = signal<LoadingLocation[]>([]);
  readonly today = new Date().toISOString().slice(0, 10);

  get bookingOptions(): FfSelectOption[] {
    // Only approved bookings can be dispatched; keep the current booking visible while editing.
    const currentBookingId = this.editingTrip()?.booking?.id;
    return [{ label: '-- Choose Booking Reference --', value: '' }, ...this.bookings()
      .filter(booking => booking.status === 'APPROVED' || booking.id === currentBookingId)
      .map(booking => ({ label: [booking.bookingNumber || booking.code, booking.customer?.name, this.bookingOrderText(booking)].filter(Boolean).join(' — '), value: booking.id }))];
  }
  get vehicleOptions(): FfSelectOption[] {
    const currentVehicleId = this.editingTrip()?.vehicle?.id;
    return [{ label: '-- Choose Transit Vehicle --', value: '' }, ...activeOrSelected(this.vehicles(), this.tripForm?.getRawValue()?.vehicle?.id).map(v => ({
      label: ([v.code || v.registrationNumber, v.name || [v.brand, v.model].filter(Boolean).join(' ')].filter(Boolean).join(' — ') || 'Unknown Vehicle')
        + (v.underMaintenance ? ' (Under maintenance)' : ''),
      value: v.id,
      disabled: !!v.underMaintenance && v.id !== currentVehicleId
    }))];
  }
  get driverOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Driver Assignment --', value: '' }, ...activeOrSelected(this.drivers(), this.tripForm?.getRawValue()?.driver?.id).map(driver => ({ label: driver.name, value: driver.id }))];
  }
  get quarryOptions(): FfSelectOption[] {
    return [{ label: '-- Not recorded --', value: '' }, ...activeOrSelected(this.quarries(), this.tripForm?.getRawValue()?.quarry?.id).map(q => ({ label: q.name, value: q.id }))];
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
  /**
   * Unit of a trip line: the unit the material was booked in (trips never change or convert it).
   * Falls back to the saved trip line while the booking list is still loading.
   */
  rowUnit(row: any): string {
    const bookingId = Number(this.tripForm?.getRawValue()?.booking?.id);
    const matId = Number(row.get('material.id')?.value);
    const booking = this.bookings().find(b => b.id === bookingId);
    const line = booking?.details?.find((d: any) => d.material?.id === matId && !d.isDeleted);
    if (line?.uom) return uomLabel(line.uom);
    const bal = this.balance()?.lines.find(l => l.materialId === matId);
    if (bal?.uom) return uomLabel(bal.uom);
    const saved = this.editingTrip()?.details?.find(d => d.material?.id === matId);
    return saved?.uom ? uomLabel(saved.uom) : '';
  }
  qtyLabel(text: string, row: any): string {
    const u = this.rowUnit(row);
    return u ? `${text} (${u})` : text;
  }
  /** "2 Unit M-Sand" per trip line (list view); delivered quantity once recorded. */
  lineText(d: TripDetail): string {
    const q = d.deliveredQuantity && d.deliveredQuantity > 0 ? d.deliveredQuantity : d.quantity;
    return orderLineText(q, d.uom, d.material?.name || this.materials().find(m => m.id === d.material?.id)?.name);
  }
  /** Material/quantity/vehicle are locked once the truck has delivered; only weighbridge values stay editable. */
  get isCompletedEdit(): boolean {
    return this.editingTrip()?.status === 'COMPLETED';
  }
  get materialOptions(): FfSelectOption[] {
    const b = this.balance();
    if (b) {
      // Only the booking's materials (the server rejects any other material for this booking).
      return [{ label: '-- Choose Material --', value: '' }, ...b.lines.map(l => ({ label: l.materialName, value: l.materialId }))];
    }
    return [{ label: '-- Choose Material --', value: '' }, ...this.materials().map(material => ({ label: material.name, value: material.id }))];
  }

  // ---------------------------------------------------------------- booking → trip auto-fill

  /** Booked / already moved / remaining per material of the selected booking (from the server, same as the save check). */
  balance = signal<BookingTripBalance | null>(null);
  balanceLoading = signal(false);
  private lastBookingId: number | null = null;
  private suppressBookingChange = false;

  /** "10 Unit M-Sand, 5 Unit P-Sand" for the booking dropdown. */
  private bookingOrderText(booking: any): string {
    const lines = (booking?.details || []).filter((d: any) => !d.isDeleted);
    const parts = lines.slice(0, 2).map((d: any) => orderLineText(d.quantity, d.uom, d.material?.name));
    if (lines.length > 2) parts.push(`+${lines.length - 2} more`);
    return parts.join(', ');
  }

  balanceLine(row: any): BookingTripBalanceLine | null {
    const matId = Number(row.get('material.id')?.value);
    return this.balance()?.lines.find(l => l.materialId === matId) ?? null;
  }

  /** Quantity this trip takes for a material (delivered if recorded, else planned) — same rule as the server. */
  private tripQtyFor(materialId: number, exceptRow?: any): number {
    let total = 0;
    for (const r of this.detailsArray.controls) {
      if (r === exceptRow || Number(r.get('material.id')?.value) !== materialId) continue;
      const delivered = Number(r.get('deliveredQuantity')?.value);
      const planned = Number(r.get('quantity')?.value);
      total += delivered > 0 ? delivered : (planned > 0 ? planned : 0);
    }
    return total;
  }

  /** Remaining on the booking for this row after the trip's other rows of the same material. */
  rowRemaining(row: any): number | null {
    const line = this.balanceLine(row);
    if (!line) return null;
    return Math.max(0, Number(line.remaining) - this.tripQtyFor(line.materialId, row));
  }

  /** How much this trip is over the booking balance for the row's material (0 = fine). The server makes the final check. */
  overBy(row: any): number {
    const line = this.balanceLine(row);
    if (!line) return 0;
    const over = this.tripQtyFor(line.materialId) - Number(line.remaining);
    return over > 0.0001 ? Math.round(over * 1000) / 1000 : 0;
  }

  canUseRemaining(row: any): boolean {
    const rem = this.rowRemaining(row);
    return rem !== null && rem > 0 && !row.get('quantity')?.disabled;
  }

  useRemaining(row: any): void {
    const rem = this.rowRemaining(row);
    if (rem !== null && rem > 0) {
      row.get('quantity')?.setValue(Math.round(rem * 1000) / 1000);
      row.get('quantity')?.markAsDirty();
    }
  }

  private linesHaveInput(): boolean {
    return this.detailsArray.controls.some(r => !!r.get('material.id')?.value || !!r.get('quantity')?.value
      || !!r.get('loadedQuantity')?.value || !!r.get('deliveredQuantity')?.value);
  }

  /** New trip: selecting a booking loads its customer, site and materials (quantity left blank to enter per lorry). */
  private onBookingChange(rawId: any): void {
    if (this.suppressBookingChange || this.editingTrip()) return;
    const id = Number(rawId) || null;
    if (id === this.lastBookingId) return;
    if (!id) {
      this.lastBookingId = null;
      this.balance.set(null);
      return;
    }
    if (this.lastBookingId && this.linesHaveInput()) {
      const previous = this.lastBookingId;
      this.dialog.open(ConfirmationDialogComponent, {
        data: {
          title: 'Change Booking',
          message: 'The material lines will be replaced with the materials of the newly selected booking. Continue?',
          confirmText: 'Change booking',
          type: 'warning'
        }
      }).afterClosed().subscribe(ok => {
        if (ok) {
          this.loadBookingIntoForm(id);
        } else {
          this.suppressBookingChange = true;
          this.tripForm.get('booking.id')?.setValue(previous);
          this.suppressBookingChange = false;
        }
      });
      return;
    }
    this.loadBookingIntoForm(id);
  }

  private loadBookingIntoForm(id: number): void {
    this.lastBookingId = id;
    this.balance.set(null);
    this.balanceLoading.set(true);
    this.tripMgmtService.getBookingBalance(id).subscribe({
      next: res => {
        if (this.lastBookingId !== id) return;          // user picked another booking meanwhile
        this.balanceLoading.set(false);
        const b = res?.data;
        if (!res?.success || !b) {
          this.notify.error(res?.message || 'Could not load the booking details');
          return;
        }
        this.balance.set(b);
        while (this.detailsArray.length !== 0) this.detailsArray.removeAt(0);
        for (const line of b.lines) {
          if (Number(line.remaining) > 0) {
            this.detailsArray.push(this.buildDetailRow({ material: { id: line.materialId }, quantity: null }));
          }
        }
      },
      error: err => {
        if (this.lastBookingId !== id) return;
        this.balanceLoading.set(false);
        this.notify.error(this.tripError(err, 'Could not load the booking details'));
      }
    });
  }

  /** Editing: show the booking summary and balances, excluding this trip's own quantity. Lines are not touched. */
  private loadBalanceForEdit(trip: Trip): void {
    this.balance.set(null);
    const bookingId = trip.booking?.id;
    if (!bookingId) return;
    this.balanceLoading.set(true);
    this.tripMgmtService.getBookingBalance(bookingId, trip.id).subscribe({
      next: res => {
        if (this.editingTrip()?.id !== trip.id) return;
        this.balanceLoading.set(false);
        if (res?.success && res.data) this.balance.set(res.data);
      },
      error: () => { this.balanceLoading.set(false); }
    });
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
    this.masterService.getMasters<any>('bookings', this.companyId, { status: 'APPROVED', size: 1000 }).subscribe(res => {
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
    this.tripForm.get('booking.id')?.valueChanges.subscribe(id => this.onBookingChange(id));
  }

  private buildDetailRow(d?: any) {
    return this.fb.group({
      material: this.fb.group({
        id: [d?.material?.id ?? '', Validators.required]
      }),
      quantity: [d && d.quantity !== undefined ? d.quantity : null, [Validators.required, Validators.min(0.01)]],
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
    this.balance.set(null);
    this.lastBookingId = null;
    this.tripForm.enable();
    this.suppressBookingChange = true;
    this.tripForm.reset({ tripDate: this.today, quarry: { id: '' }, loadingLocation: { id: '' } });
    this.suppressBookingChange = false;
    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }
    this.addDetail();
    this.showEditor.set(true);
  }

  openEditTrip(trip: Trip) {
    this.editingTrip.set(trip);
    this.lastBookingId = trip.booking?.id ?? null;
    if (trip.booking?.id && !this.bookings().some(b => b.id === trip.booking!.id)) {
      this.bookings.set([...this.bookings(), trip.booking]);
    }
    this.loadBalanceForEdit(trip);
    this.suppressBookingChange = true;
    this.tripForm.patchValue({
      booking: { id: trip.booking?.id },
      vehicle: { id: trip.vehicle?.id },
      driver: { id: trip.driver?.id },
      tripDate: trip.tripDate || this.today,
      quarry: { id: trip.quarry?.id ?? '' },
      loadingLocation: { id: trip.loadingLocation?.id ?? '' },
      remarks: trip.remarks
    });
    this.suppressBookingChange = false;

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
    if (this.detailsArray.length === 0) {
      this.notify.error(this.balance() && !this.balance()!.anythingLeft
        ? 'Nothing is left to plan on this booking.'
        : 'Add at least one material line to the trip.');
      return;
    }
    if (this.tripForm.invalid) {
      this.tripForm.markAllAsTouched();
      this.notify.error('Fill the required fields — enter the planned quantity for each material.');
      return;
    }

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
