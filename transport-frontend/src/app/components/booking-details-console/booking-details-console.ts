import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { BookingMgmtService, Booking, BookingDetail } from '../../services/booking-mgmt.service';
import { MasterService } from '../../services/master.service';
import { CustomerMgmtService } from '../../services/customer-mgmt.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfButtonComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

@Component({
  selector: 'app-booking-details-console',
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
  templateUrl: './booking-details-console.html',
  styles: []
})
export class BookingDetailsConsoleComponent implements OnInit {
  private bookingMgmtService = inject(BookingMgmtService);
  private masterService = inject(MasterService);
  private customerMgmtService = inject(CustomerMgmtService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  // Lists
  bookings = signal<Booking[]>([]);
  customers = signal<any[]>([]);
  materials = signal<any[]>([]);
  sites = signal<any[]>([]);
  priorities = signal<any[]>([]);

  get customerOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Customer --', value: '' }, ...this.customers().map(customer => ({ label: customer.name, value: customer.id }))];
  }
  get siteOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Unloading Site --', value: '' }, ...this.sites().map(site => ({ label: site.siteName, value: site.id }))];
  }
  get priorityOptions(): FfSelectOption[] {
    return this.priorities().length > 0
      ? this.priorities().map(p => ({ label: p.name, value: p.code }))
      : [{ label: 'HIGH', value: 'HIGH' }, { label: 'MEDIUM', value: 'MEDIUM' }, { label: 'LOW', value: 'LOW' }];
  }
  get materialOptions(): FfSelectOption[] {
    return [{ label: '-- Choose Material --', value: '' }, ...this.materials().map(material => ({ label: material.name, value: material.id }))];
  }

  // States
  activeTab = signal<string>('list'); // 'list' | 'editor'
  loading = signal<boolean>(false);
  showEditor = signal<boolean>(false);
  editingBooking = signal<Booking | null>(null);

  // Forms
  bookingForm!: FormGroup;

  ngOnInit() {
    this.initForm();
    this.loadBookings();
    this.loadDropdownData();
  }

  loadDropdownData() {
    this.masterService.getLookupList(this.companyId, 'PRIORITY').subscribe(res => {
      if (res.success && res.data) {
        this.priorities.set(res.data);
      }
    });
    this.masterService.getMasters<any>('customers', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.customers.set(res.data.content || res.data);
      }
    });
    this.masterService.getMasters<any>('materials', this.companyId, { size: 100 }).subscribe(res => {
      if (res.success && res.data) {
        this.materials.set(res.data.content || res.data);
      }
    });
  }

  initForm() {
    this.bookingForm = this.fb.group({
      customer: this.fb.group({
        id: ['', Validators.required]
      }),
      deliverySite: this.fb.group({
        id: ['', Validators.required]
      }),
      priority: ['MEDIUM', Validators.required],
      remarks: [''],
      details: this.fb.array([])
    });

    this.bookingForm.get('customer.id')?.valueChanges.subscribe(custId => {
      if (custId) {
        this.loadCustomerSites(Number(custId));
      } else {
        this.sites.set([]);
      }
    });
  }

  loadCustomerSites(customerId: number) {
    this.customerMgmtService.getSites(customerId).subscribe(res => {
      if (res.success && res.data) {
        this.sites.set(res.data);
      }
    });
  }

  get detailsArray(): FormArray {
    return this.bookingForm.get('details') as FormArray;
  }

  addDetail() {
    const detailGroup = this.fb.group({
      material: this.fb.group({
        id: ['', Validators.required]
      }),
      quantity: [1, [Validators.required, Validators.min(1)]],
      rate: [0, Validators.required],
      transportRate: [0, Validators.required],
      royaltyRate: [0, Validators.required],
      loadingCharge: [0, Validators.required],
      gstPercentage: [18, Validators.required]
    });

    detailGroup.get('material.id')?.valueChanges.subscribe(matId => this.applyMaterialToDetail(detailGroup, matId));
    this.detailsArray.push(detailGroup);
  }

  removeDetail(index: number) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Remove Line Item',
        message: 'Remove this booking material line?',
        confirmText: 'Remove',
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.detailsArray.removeAt(index);
    });
  }

  onMaterialSelect(index: number, matId: any) {
    this.applyMaterialToDetail(this.detailsArray.at(index) as FormGroup, matId);
  }

  private applyMaterialToDetail(row: FormGroup, matId: any) {
    const mat = this.materials().find(m => m.id === +matId);
    if (mat) {
      row.patchValue({
        rate: mat.defaultRate ?? 0,
        transportRate: mat.transportRate ?? 0,
        royaltyRate: mat.royaltyRate ?? 0,
        loadingCharge: mat.loadingCharge ?? 0
      });
    }
  }

  loadBookings() {
    this.loading.set(true);
    this.bookingMgmtService.getBookings().subscribe(res => {
      if (res.success && res.data) {
        this.bookings.set(res.data.content || res.data);
      }
      this.loading.set(false);
    });
  }

  openAddBooking() {
    this.editingBooking.set(null);
    this.bookingForm.reset({ priority: 'MEDIUM' });
    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }
    this.addDetail();
    this.showEditor.set(true);
  }

  openEditBooking(bkg: Booking) {
    if (bkg.customer?.id) {
      this.loadCustomerSites(bkg.customer.id);
    }
    this.editingBooking.set(bkg);
    this.bookingForm.patchValue({
      customer: { id: bkg.customer?.id },
      deliverySite: { id: bkg.deliverySite?.id },
      priority: bkg.priority,
      remarks: bkg.remarks
    });

    while (this.detailsArray.length !== 0) {
      this.detailsArray.removeAt(0);
    }

    if (bkg.details) {
      for (const d of bkg.details) {
        const row = this.fb.group({
          material: this.fb.group({
            id: [d.material?.id, Validators.required]
          }),
          quantity: [d.quantity, [Validators.required, Validators.min(1)]],
          rate: [d.rate, Validators.required],
          transportRate: [d.transportRate, Validators.required],
          royaltyRate: [d.royaltyRate, Validators.required],
          loadingCharge: [d.loadingCharge, Validators.required],
          gstPercentage: [d.gstPercentage, Validators.required]
        });
        row.get('material.id')?.valueChanges.subscribe(matId => this.applyMaterialToDetail(row, matId));
        this.detailsArray.push(row);
      }
    }

    this.showEditor.set(true);
  }

  saveBooking() {
    if (this.bookingForm.invalid) return;

    this.loading.set(true);
    const val = this.bookingForm.getRawValue();
    const bkgObj = this.editingBooking();

    if (bkgObj && bkgObj.id) {
      this.bookingMgmtService.updateBooking(bkgObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Booking updated successfully');
          this.loadBookings();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to update booking');
        }
      });
    } else {
      this.bookingMgmtService.createBooking(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Booking registered successfully');
          this.loadBookings();
          this.showEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to register booking');
        }
      });
    }
  }

  approveBooking(bkg: Booking) {
    if (!bkg.id) return;
    this.bookingMgmtService.approveBooking(bkg.id).subscribe({
      next: () => {
        this.notify.success('Booking approved successfully');
        this.loadBookings();
      },
      error: () => this.notify.error('Failed to approve booking')
    });
  }

  rejectBooking(bkg: Booking) {
    if (!bkg.id) return;
    this.bookingMgmtService.rejectBooking(bkg.id).subscribe({
      next: () => {
        this.notify.success('Booking rejected successfully');
        this.loadBookings();
      },
      error: () => this.notify.error('Failed to reject booking')
    });
  }

  deleteBooking(bkg: Booking) {
    if (!bkg.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Cancel Booking',
        message: `Are you sure you want to cancel booking: ${bkg.bookingNumber}?`,
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && bkg.id) {
        this.bookingMgmtService.deleteBooking(bkg.id).subscribe({
          next: () => {
            this.notify.success('Booking cancelled successfully');
            this.loadBookings();
          },
          error: () => this.notify.error('Failed to cancel booking')
        });
      }
    });
  }
}
