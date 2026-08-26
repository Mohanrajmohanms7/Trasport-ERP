import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MaterialMgmtService, LoadingLocation, MaterialPrice } from '../../services/material-mgmt.service';
import { MasterService } from '../../services/master.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfNumberComponent, FfDatepickerComponent, FfButtonComponent, FfTextareaComponent } from '@ff/ui';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

@Component({
  selector: 'app-material-quarry-console',
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
    FfDatepickerComponent,
    FfButtonComponent,
    FfTextareaComponent
  ],
  templateUrl: './material-quarry-console.html',
  styleUrl: './material-quarry-console.css'
})
export class MaterialQuarryConsoleComponent implements OnInit {
  private materialMgmtService = inject(MaterialMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  /** materials | quarries | pricing | locations */
  activeTab = signal<string>('materials');
  materialId = signal<number | null>(null);
  materials = signal<any[]>([]);
  quarries = signal<any[]>([]);
  loading = signal<boolean>(false);
  saveError = signal<string>('');

  readonly selectedMaterial = computed(() => {
    const id = this.materialId();
    return this.materials().find(m => m.id === id) || null;
  });

  readonly selectedMaterialLabel = computed(() => {
    const m = this.selectedMaterial();
    if (!m) return 'No material selected';
    return [m.code, m.name].filter(Boolean).join(' — ');
  });

  get materialOptions(): FfSelectOption[] {
    return [
      { label: '-- Select Material --', value: '' },
      ...this.materials().map(m => ({
        label: `${m.code || ''} ${m.name || ''}`.trim(),
        value: m.id
      }))
    ];
  }

  loadingLocations = signal<LoadingLocation[]>([]);
  prices = signal<MaterialPrice[]>([]);

  materialSelectForm!: FormGroup;
  materialForm!: FormGroup;
  quarryForm!: FormGroup;
  locationForm!: FormGroup;
  priceForm!: FormGroup;

  showMaterialEditor = signal<boolean>(false);
  showQuarryEditor = signal<boolean>(false);
  showLocationEditor = signal<boolean>(false);
  showPriceEditor = signal<boolean>(false);
  editingMaterial = signal<any | null>(null);
  editingQuarry = signal<any | null>(null);
  editingLocation = signal<LoadingLocation | null>(null);

  ngOnInit() {
    this.initForms();
    this.loadMaterials();
    this.loadQuarries();
    this.loadLocations();
  }

  initForms() {
    this.materialSelectForm = this.fb.group({
      materialId: ['']
    });

    this.materialSelectForm.get('materialId')!.valueChanges.subscribe(value => {
      this.selectMaterial(value);
    });

    this.materialForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      defaultRate: [0, [Validators.min(0)]],
      density: [1.5, [Validators.min(0)]],
      status: ['ACTIVE']
    });

    this.quarryForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      locationAddress: [''],
      ownerName: [''],
      contactNumber: [''],
      status: ['ACTIVE']
    });

    this.locationForm = this.fb.group({
      locationCode: ['', [Validators.required, Validators.maxLength(50)]],
      loadingPoint: ['', [Validators.required, Validators.maxLength(150)]],
      loadingCharges: [0, [Validators.required, Validators.min(0)]],
      latitude: [null],
      longitude: [null]
    });

    this.priceForm = this.fb.group({
      materialRate: [0, [Validators.required, Validators.min(0)]],
      transportRate: [0, [Validators.required, Validators.min(0)]],
      royaltyRate: [0, [Validators.required, Validators.min(0)]],
      loadingCharge: [0, [Validators.required, Validators.min(0)]],
      effectiveDate: ['', Validators.required]
    });
  }

  loadMaterials() {
    this.masterService.getMasters<any>('materials', this.companyId, { size: 100, page: 0 }).subscribe(res => {
      if (res.success && res.data) {
        const list = res.data.content || res.data || [];
        this.materials.set(list);
        if (list.length && this.materialId() == null) {
          this.selectMaterial(list[0].id);
        }
      }
    });
  }

  loadQuarries() {
    this.masterService.getMasters<any>('quarries', this.companyId, { size: 100, page: 0 }).subscribe(res => {
      if (res.success && res.data) {
        this.quarries.set(res.data.content || res.data || []);
      }
    });
  }

  selectMaterial(id: number | string | null) {
    const materialId = id === '' || id == null ? null : Number(id);
    this.materialId.set(materialId);
    this.materialSelectForm.patchValue({ materialId: materialId ?? '' }, { emitEvent: false });
    this.prices.set([]);
    if (materialId == null) return;
    this.loadPrices();
  }

  loadLocations() {
    this.materialMgmtService.getLocations().subscribe(res => {
      if (res.success && res.data) {
        this.loadingLocations.set(res.data);
      }
    });
  }

  loadPrices() {
    const id = this.materialId();
    if (id == null) return;
    this.materialMgmtService.getPrices(id).subscribe(res => {
      if (res.success && res.data) {
        this.prices.set(res.data);
      }
    });
  }

  openAddMaterial() {
    this.saveError.set('');
    this.editingMaterial.set(null);
    this.materialForm.reset({ defaultRate: 850, density: 1.5, status: 'ACTIVE' });
    this.showMaterialEditor.set(true);
  }

  openEditMaterial(row: any) {
    this.saveError.set('');
    this.editingMaterial.set(row);
    this.materialForm.patchValue({
      code: row.code,
      name: row.name,
      description: row.description || '',
      defaultRate: row.defaultRate ?? 0,
      density: row.density ?? 1.5,
      status: row.status || 'ACTIVE'
    });
    this.showMaterialEditor.set(true);
  }

  saveMaterial() {
    if (this.materialForm.invalid) return;
    this.loading.set(true);
    this.saveError.set('');
    const payload = {
      ...this.materialForm.value,
      companyId: this.companyId
    };
    const existing = this.editingMaterial();
    const req$ = existing?.id
      ? this.masterService.updateMaster('materials', existing.id, payload)
      : this.masterService.saveMaster('materials', payload);

    req$.subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.notify.success(existing?.id ? 'Material updated successfully' : 'Material created successfully');
          this.showMaterialEditor.set(false);
          this.loadMaterials();
        } else {
          const errMsg = res.errors?.[0] || res.message || 'Failed to save material';
          this.saveError.set(errMsg);
          this.notify.error(errMsg);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const errMsg = err.error?.errors?.[0] || err.error?.message || 'Failed to save material';
        this.saveError.set(errMsg);
        this.notify.error(errMsg);
      }
    });
  }

  deleteMaterial(row: any) {
    if (!row?.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Material',
        message: `Delete material ${row.code} — ${row.name}?`,
        confirmText: 'Delete',
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.masterService.deleteMaster('materials', row.id).subscribe({
        next: () => {
          this.notify.success('Material deleted successfully');
          this.loadMaterials();
        },
        error: () => this.notify.error('Failed to delete material')
      });
    });
  }

  openAddQuarry() {
    this.saveError.set('');
    this.editingQuarry.set(null);
    this.quarryForm.reset({ status: 'ACTIVE' });
    this.showQuarryEditor.set(true);
  }

  openEditQuarry(row: any) {
    this.saveError.set('');
    this.editingQuarry.set(row);
    this.quarryForm.patchValue({
      code: row.code,
      name: row.name,
      description: row.description || '',
      locationAddress: row.locationAddress || '',
      ownerName: row.ownerName || '',
      contactNumber: row.contactNumber || '',
      status: row.status || 'ACTIVE'
    });
    this.showQuarryEditor.set(true);
  }

  saveQuarry() {
    if (this.quarryForm.invalid) return;
    this.loading.set(true);
    this.saveError.set('');
    const payload = {
      ...this.quarryForm.value,
      companyId: this.companyId
    };
    const existing = this.editingQuarry();
    const req$ = existing?.id
      ? this.masterService.updateMaster('quarries', existing.id, payload)
      : this.masterService.saveMaster('quarries', payload);

    req$.subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.notify.success(existing?.id ? 'Quarry updated successfully' : 'Quarry created successfully');
          this.showQuarryEditor.set(false);
          this.loadQuarries();
        } else {
          const errMsg = res.errors?.[0] || res.message || 'Failed to save quarry';
          this.saveError.set(errMsg);
          this.notify.error(errMsg);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const errMsg = err.error?.errors?.[0] || err.error?.message || 'Failed to save quarry';
        this.saveError.set(errMsg);
        this.notify.error(errMsg);
      }
    });
  }

  deleteQuarry(row: any) {
    if (!row?.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Quarry',
        message: `Delete quarry ${row.code} — ${row.name}?`,
        confirmText: 'Delete',
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.masterService.deleteMaster('quarries', row.id).subscribe({
        next: () => {
          this.notify.success('Quarry deleted successfully');
          this.loadQuarries();
        },
        error: () => this.notify.error('Failed to delete quarry')
      });
    });
  }

  openPriceEditor() {
    if (!this.materialId()) return;
    this.showPriceEditor.set(true);
  }

  openAddLocation() {
    this.editingLocation.set(null);
    this.locationForm.reset({ loadingCharges: 0 });
    this.showLocationEditor.set(true);
  }

  openEditLocation(loc: LoadingLocation) {
    this.editingLocation.set(loc);
    this.locationForm.patchValue(loc);
    this.showLocationEditor.set(true);
  }

  saveLocation() {
    if (this.locationForm.invalid) return;

    this.loading.set(true);
    const val = this.locationForm.value;
    const locObj = this.editingLocation();

    if (locObj?.id) {
      this.materialMgmtService.updateLocation(locObj.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Loading location updated successfully');
          this.loadLocations();
          this.showLocationEditor.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to update loading location');
        }
      });
    } else {
      this.materialMgmtService.createLocation(val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Loading location created successfully');
          this.loadLocations();
          this.showLocationEditor.set(false);
          this.locationForm.reset({ loadingCharges: 0 });
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(err.error?.message || 'Failed to create loading location');
        }
      });
    }
  }

  deleteLocation(loc: LoadingLocation) {
    if (!loc.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Loading Location',
        message: `Are you sure you want to remove loading location: ${loc.loadingPoint}?`,
        confirmText: 'Delete',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && loc.id) {
        this.materialMgmtService.deleteLocation(loc.id).subscribe({
          next: () => {
            this.notify.success('Loading location deleted successfully');
            this.loadLocations();
          },
          error: () => this.notify.error('Failed to delete loading location')
        });
      }
    });
  }

  savePrice() {
    const id = this.materialId();
    if (id == null || this.priceForm.invalid) return;

    this.loading.set(true);
    this.materialMgmtService.createPrice(id, this.priceForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.notify.success('Pricing rule saved successfully');
        this.loadPrices();
        this.showPriceEditor.set(false);
        this.priceForm.reset({
          materialRate: 0,
          transportRate: 0,
          royaltyRate: 0,
          loadingCharge: 0
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to save pricing rule');
      }
    });
  }

  deletePrice(price: MaterialPrice) {
    if (!price.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Price Rule',
        message: `Delete pricing rule for ${this.selectedMaterialLabel()}?`,
        confirmText: 'Delete',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && price.id) {
        this.materialMgmtService.deletePrice(price.id).subscribe({
          next: () => {
            this.notify.success('Pricing rule deleted successfully');
            this.loadPrices();
          },
          error: () => this.notify.error('Failed to delete pricing rule')
        });
      }
    });
  }
}
