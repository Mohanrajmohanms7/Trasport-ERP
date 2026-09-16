import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MaterialMgmtService, LoadingLocation, MaterialPrice } from '../../services/material-mgmt.service';
import { UomMgmtService, UomMaster, UomConversion } from '../../services/uom-mgmt.service';
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
  private uomMgmtService = inject(UomMgmtService);
  private masterService = inject(MasterService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  private companyId = resolveTenantCompanyId();

  /** materials | quarries | pricing | locations | uoms | conversions */
  activeTab = signal<string>('materials');
  materialId = signal<number | null>(null);
  materials = signal<any[]>([]);
  quarries = signal<any[]>([]);
  uoms = signal<UomMaster[]>([]);
  conversions = signal<UomConversion[]>([]);
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

  get uomOptions(): FfSelectOption[] {
    return [
      { label: '-- Select UOM --', value: '' },
      ...this.uoms().map(u => ({
        label: `${u.code} (${u.name})`,
        value: u.id!
      }))
    ];
  }

  get categoryOptions(): FfSelectOption[] {
    return [
      { label: 'WEIGHT', value: 'WEIGHT' },
      { label: 'VOLUME', value: 'VOLUME' },
      { label: 'PACKAGING', value: 'PACKAGING' },
      { label: 'COUNT', value: 'COUNT' },
      { label: 'LIQUID_VOLUME', value: 'LIQUID_VOLUME' },
      { label: 'CUSTOM', value: 'CUSTOM' }
    ];
  }

  loadingLocations = signal<LoadingLocation[]>([]);
  prices = signal<MaterialPrice[]>([]);

  materialSelectForm!: FormGroup;
  materialForm!: FormGroup;
  quarryForm!: FormGroup;
  locationForm!: FormGroup;
  priceForm!: FormGroup;
  uomForm!: FormGroup;
  conversionForm!: FormGroup;

  showMaterialEditor = signal<boolean>(false);
  showQuarryEditor = signal<boolean>(false);
  showLocationEditor = signal<boolean>(false);
  showPriceEditor = signal<boolean>(false);
  showUomEditor = signal<boolean>(false);
  showConversionEditor = signal<boolean>(false);

  editingMaterial = signal<any | null>(null);
  editingQuarry = signal<any | null>(null);
  editingLocation = signal<LoadingLocation | null>(null);
  editingUom = signal<UomMaster | null>(null);
  editingConversion = signal<UomConversion | null>(null);

  ngOnInit() {
    this.initForms();
    this.loadMaterials();
    this.loadQuarries();
    this.loadLocations();
    this.loadUoms();
    this.loadConversions();
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
      defaultUomId: [null],
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

    this.uomForm = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(50)]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      symbol: ['', [Validators.maxLength(20)]],
      category: ['VOLUME', [Validators.required]],
      isBaseUnit: [false],
      description: [''],
      status: ['ACTIVE']
    });

    this.conversionForm = this.fb.group({
      materialId: [null],
      fromUomId: ['', [Validators.required]],
      toUomId: ['', [Validators.required]],
      conversionFactor: [1, [Validators.required, Validators.min(0.000001)]],
      description: [''],
      status: ['ACTIVE']
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

  loadUoms() {
    this.uomMgmtService.getUoms().subscribe(res => {
      if (res.success && res.data) {
        this.uoms.set(res.data);
      }
    });
  }

  loadConversions() {
    this.uomMgmtService.getConversions().subscribe(res => {
      if (res.success && res.data) {
        this.conversions.set(res.data);
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

  // --- Material CRUD ---

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
      defaultUomId: row.defaultUom?.id || null,
      status: row.status || 'ACTIVE'
    });
    this.showMaterialEditor.set(true);
  }

  saveMaterial() {
    if (this.materialForm.invalid) return;
    this.loading.set(true);
    this.saveError.set('');
    const formVal = this.materialForm.value;
    const payload: any = {
      code: formVal.code,
      name: formVal.name,
      description: formVal.description,
      defaultRate: formVal.defaultRate,
      density: formVal.density,
      status: formVal.status,
      companyId: this.companyId
    };
    if (formVal.defaultUomId) {
      payload.defaultUom = { id: formVal.defaultUomId };
    }

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
      if (confirmed && row.id) {
        this.masterService.deleteMaster('materials', row.id).subscribe({
          next: () => {
            this.notify.success('Material deleted successfully');
            this.loadMaterials();
          },
          error: () => this.notify.error('Failed to delete material')
        });
      }
    });
  }

  // --- Quarry CRUD ---

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
      if (confirmed && row.id) {
        this.masterService.deleteMaster('quarries', row.id).subscribe({
          next: () => {
            this.notify.success('Quarry deleted successfully');
            this.loadQuarries();
          },
          error: () => this.notify.error('Failed to delete quarry')
        });
      }
    });
  }

  // --- UOM Master Methods ---

  openAddUom() {
    this.editingUom.set(null);
    this.uomForm.reset({ category: 'VOLUME', isBaseUnit: false, status: 'ACTIVE' });
    this.showUomEditor.set(true);
  }

  openEditUom(row: UomMaster) {
    this.editingUom.set(row);
    this.uomForm.patchValue({
      code: row.code,
      name: row.name,
      symbol: row.symbol || '',
      category: row.category || 'VOLUME',
      isBaseUnit: row.isBaseUnit || false,
      description: row.description || '',
      status: row.status || 'ACTIVE'
    });
    this.showUomEditor.set(true);
  }

  saveUom() {
    if (this.uomForm.invalid) return;
    this.loading.set(true);
    const existing = this.editingUom();
    const payload = this.uomForm.value;

    const req$ = existing?.id
      ? this.uomMgmtService.updateUom(existing.id, payload)
      : this.uomMgmtService.createUom(payload);

    req$.subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.notify.success(existing?.id ? 'UOM updated successfully' : 'UOM created successfully');
          this.showUomEditor.set(false);
          this.loadUoms();
        } else {
          this.notify.error(res.message || 'Failed to save UOM');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to save UOM');
      }
    });
  }

  deleteUom(row: UomMaster) {
    if (!row.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete UOM',
        message: `Delete Unit of Measurement: ${row.code} (${row.name})?`,
        confirmText: 'Delete',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && row.id) {
        this.uomMgmtService.deleteUom(row.id).subscribe({
          next: () => {
            this.notify.success('UOM deleted successfully');
            this.loadUoms();
          },
          error: () => this.notify.error('Failed to delete UOM')
        });
      }
    });
  }

  // --- UOM Conversion Methods ---

  openAddConversion() {
    this.editingConversion.set(null);
    this.conversionForm.reset({ conversionFactor: 1, status: 'ACTIVE' });
    this.showConversionEditor.set(true);
  }

  openEditConversion(row: UomConversion) {
    this.editingConversion.set(row);
    this.conversionForm.patchValue({
      materialId: row.materialId || (row.material?.id) || null,
      fromUomId: typeof row.fromUom === 'object' ? row.fromUom?.id : row.fromUom,
      toUomId: typeof row.toUom === 'object' ? row.toUom?.id : row.toUom,
      conversionFactor: row.conversionFactor,
      description: row.description || '',
      status: row.status || 'ACTIVE'
    });
    this.showConversionEditor.set(true);
  }

  saveConversion() {
    if (this.conversionForm.invalid) return;
    this.loading.set(true);
    const formVal = this.conversionForm.value;
    const existing = this.editingConversion();

    const payload: any = {
      fromUom: { id: Number(formVal.fromUomId) },
      toUom: { id: Number(formVal.toUomId) },
      conversionFactor: formVal.conversionFactor,
      description: formVal.description,
      status: formVal.status
    };
    if (formVal.materialId) {
      payload.material = { id: Number(formVal.materialId) };
    }

    const req$ = existing?.id
      ? this.uomMgmtService.updateConversion(existing.id, payload)
      : this.uomMgmtService.createConversion(payload);

    req$.subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success) {
          this.notify.success(existing?.id ? 'Conversion updated successfully' : 'Conversion created successfully');
          this.showConversionEditor.set(false);
          this.loadConversions();
        } else {
          this.notify.error(res.message || 'Failed to save conversion');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(err.error?.message || 'Failed to save conversion');
      }
    });
  }

  deleteConversion(row: UomConversion) {
    if (!row.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete UOM Conversion',
        message: `Delete UOM conversion rule ${row.code || row.name}?`,
        confirmText: 'Delete',
        type: 'danger'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && row.id) {
        this.uomMgmtService.deleteConversion(row.id).subscribe({
          next: () => {
            this.notify.success('UOM Conversion deleted successfully');
            this.loadConversions();
          },
          error: () => this.notify.error('Failed to delete conversion')
        });
      }
    });
  }

  // --- Location & Pricing Methods ---

  openAddPrice() {
    if (this.materialId() == null) return;
    this.priceForm.reset({
      materialRate: 0,
      transportRate: 0,
      royaltyRate: 0,
      loadingCharge: 0,
      effectiveDate: new Date().toISOString().split('T')[0]
    });
    this.showPriceEditor.set(true);
  }

  openAddLocation() {
    this.editingLocation.set(null);
    this.locationForm.reset({ loadingCharges: 0 });
    this.showLocationEditor.set(true);
  }

  openEditLocation(loc: LoadingLocation) {
    this.editingLocation.set(loc);
    this.locationForm.patchValue({
      locationCode: loc.locationCode,
      loadingPoint: loc.loadingPoint,
      loadingCharges: loc.loadingCharges,
      latitude: loc.latitude,
      longitude: loc.longitude
    });
    this.showLocationEditor.set(true);
  }

  saveLocation() {
    if (this.locationForm.invalid) return;
    this.loading.set(true);

    const val = this.locationForm.value;
    const editing = this.editingLocation();

    if (editing?.id) {
      this.materialMgmtService.updateLocation(editing.id, val).subscribe({
        next: () => {
          this.loading.set(false);
          this.notify.success('Loading location updated successfully');
          this.loadLocations();
          this.showLocationEditor.set(false);
          this.editingLocation.set(null);
          this.locationForm.reset({ loadingCharges: 0 });
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
