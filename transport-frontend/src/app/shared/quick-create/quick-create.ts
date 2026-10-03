import { FormValidationDirective } from '../form-validation.directive';
import { Component, OnChanges, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { MasterFormDialogComponent } from '../master-forms/master-form-dialog';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { FeatureService } from '../../services/feature.service';
import { AuthService } from '../../services/auth.service';
import { resolveTenantCompanyId } from '../tenant-context';

export type QuickKind = 'customer' | 'vehicle' | 'driver' | 'supplier' | 'sparePart' | 'material' | 'quarry' | 'deliverySite' | 'expenseCategory';

const LABEL: Record<QuickKind, string> = {
  customer: 'customer', vehicle: 'vehicle', driver: 'driver', supplier: 'supplier', sparePart: 'spare part',
  material: 'material', quarry: 'quarry', deliverySite: 'delivery site', expenseCategory: 'expense category'
};
/** Client feature (Platform Admin → Feature Access) that must be on to create each record. */
const FEATURE: Record<QuickKind, string> = {
  customer: 'customers.create', vehicle: 'vehicles.create', driver: 'drivers.create', supplier: 'suppliers.create', sparePart: 'spare-parts',
  material: 'materials.materials', quarry: 'materials.quarries', deliverySite: 'customers.edit', expenseCategory: 'expenses'
};

/**
 * Who may Quick Create from a dropdown: the client has that master's Add feature, and the user's role may create it
 * (the same rules the API enforces). Viewer / driver logins never see it.
 */
@Injectable({ providedIn: 'root' })
export class QuickCreateService {
  private features = inject(FeatureService);
  private auth = inject(AuthService);

  canCreate(kind: QuickKind): boolean {
    const roles: string[] = this.auth.currentUser()?.roles || JSON.parse(localStorage.getItem('roles') || '[]');
    const staff = roles.some(r => !['VIEWER', 'DRIVER'].includes(r));
    if (!staff || !this.features.has(FEATURE[kind])) return false;
    if (kind === 'sparePart') return roles.some(r => ['COMPANY_ADMIN', 'BRANCH_MANAGER'].includes(r));
    // Dropdown lists (lookups) are changed by admins only (same rule as the API).
    if (kind === 'expenseCategory') return roles.some(r => ['SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN'].includes(r));
    return true;
  }
  /** "+ Create new customer" when allowed, else null (no button). */
  label(kind: QuickKind): string | null {
    return this.canCreate(kind) ? `+ Create new ${LABEL[kind]}` : null;
  }
}

/**
 * Quick Create popup opened from a dropdown. Saves through the normal API (same validations and duplicate checks)
 * and emits the new record so the screen can add it to the list and select it.
 */
@Component({
  selector: 'app-quick-create',
  standalone: true,
  imports: [FormValidationDirective, CommonModule, FormsModule, MasterFormDialogComponent],
  template: `
    @if (kind() === 'customer' || kind() === 'vehicle') {
      <app-master-form-dialog [kind]="$any(kind())" [record]="prefillRecord()" (saved)="saved.emit($event)" (closed)="closed.emit()" />
    } @else {
      <div class="fixed inset-0 z-[95] flex items-center justify-center p-2 bg-slate-950/70">
        <form (ngSubmit)="save()" (click)="$event.stopPropagation()"
              class="w-full max-w-lg max-h-full overflow-y-auto rounded-xl bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)] p-5 flex flex-col gap-4">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-bold">New {{ title() }}</h2>
            <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="closed.emit()" aria-label="Close"><span class="material-icons">close</span></button>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-[11px] font-semibold text-[var(--ff-text-muted)]">
            <label class="flex flex-col gap-1">{{ codeLabel() }}
              <input required class="qc-in uppercase" [(ngModel)]="f.code" name="code" /></label>
            <label class="flex flex-col gap-1">Name *<input required class="qc-in" [(ngModel)]="f.name" name="name" /></label>
            @if (kind() === 'driver') {
              <label class="flex flex-col gap-1">Licence number *<input required class="qc-in uppercase" [(ngModel)]="f.licenseNumber" name="lic" /></label>
              <label class="flex flex-col gap-1">Licence valid till<input type="date" class="qc-in" [(ngModel)]="f.licenseExpiryDate" name="licExp" /></label>
              <label class="flex flex-col gap-1">Phone<input class="qc-in" inputmode="tel" [(ngModel)]="f.phoneNumber" name="phone" /></label>
            }
            @if (kind() === 'supplier') {
              <label class="flex flex-col gap-1">Phone<input class="qc-in" inputmode="tel" [(ngModel)]="f.phone" name="phone" /></label>
              <label class="flex flex-col gap-1">GSTIN<input class="qc-in uppercase" maxlength="15" [(ngModel)]="f.gstNumber" name="gst" /></label>
            }
            @if (kind() === 'material') {
              <label class="flex flex-col gap-1">Default order unit
                <select class="qc-in" [(ngModel)]="f.defaultUomId" name="muom">
                  <option [ngValue]="null">Company default</option>
                  @for (u of uoms(); track u.id) { <option [ngValue]="u.id">{{ u.label || u.name }}</option> }
                </select></label>
              <label class="flex flex-col gap-1">Material rate (₹ / unit)<input type="number" min="0" step="0.01" class="qc-in" [(ngModel)]="f.defaultRate" name="mrate" /></label>
              <label class="flex flex-col gap-1">Transport rate (₹ / unit)<input type="number" min="0" step="0.01" class="qc-in" [(ngModel)]="f.transportRate" name="trate" /></label>
              <label class="flex flex-col gap-1">Royalty (₹ / unit)<input type="number" min="0" step="0.01" class="qc-in" [(ngModel)]="f.royaltyRate" name="roy" /></label>
              <label class="flex flex-col gap-1">Loading (₹ / unit)<input type="number" min="0" step="0.01" class="qc-in" [(ngModel)]="f.loadingCharge" name="load" /></label>
            }
            @if (kind() === 'quarry') {
              <label class="flex flex-col gap-1 sm:col-span-2">Location / address<input class="qc-in" [(ngModel)]="f.locationAddress" name="qaddr" /></label>
              <label class="flex flex-col gap-1">Contact number<input class="qc-in" inputmode="tel" [(ngModel)]="f.contactNumber" name="qphone" /></label>
            }
            @if (kind() === 'deliverySite') {
              <label class="flex flex-col gap-1 sm:col-span-2">Address *<input required class="qc-in" [(ngModel)]="f.address" name="saddr" /></label>
              <label class="flex flex-col gap-1">Site contact<input class="qc-in" [(ngModel)]="f.managerName" name="smgr" /></label>
            }
            @if (kind() === 'expenseCategory') {
              <p class="sm:col-span-2 font-normal">A new category is posted to <b>Office &amp; Administrative Expenses</b> in the accounts.</p>
            }
            @if (kind() === 'sparePart') {
              <label class="flex flex-col gap-1">Unit *
                <select required class="qc-in" [(ngModel)]="f.defaultUomId" name="uom">
                  <option [ngValue]="null">Select unit</option>
                  @for (u of uoms(); track u.id) { <option [ngValue]="u.id">{{ u.name || u.code }}</option> }
                </select></label>
              <label class="flex flex-col gap-1">Default rate (₹)<input type="number" min="0" step="0.01" class="qc-in" [(ngModel)]="f.defaultRate" name="rate" /></label>
              <label class="flex flex-col gap-1">Reorder level<input type="number" min="0" step="0.01" class="qc-in" [(ngModel)]="f.reorderLevel" name="reorder" /></label>
            }
          </div>
          <p class="text-[11px] text-[var(--ff-text-muted)]">More details can be added later in the {{ title() }} master.</p>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)]" (click)="closed.emit()">Cancel</button>
            <button type="submit" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white font-semibold disabled:opacity-60" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save & select' }}</button>
          </div>
        </form>
      </div>
    }
  `,
  styles: [`.qc-in { height: 38px; border: 1px solid var(--ff-border-default); border-radius: 8px; padding: 0 10px; font-size: 13px; font-weight: 400; background: var(--ff-surface-card); color: var(--ff-text-primary); }`]
})
export class QuickCreateComponent implements OnChanges {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private companyId = resolveTenantCompanyId();

  readonly kind = input.required<QuickKind>();
  /** Text typed in the dropdown search: pre-fills the name. */
  readonly prefill = input<string>('');
  readonly saved = output<any>();
  readonly closed = output<void>();

  f: any = {};
  saving = signal(false);
  uoms = signal<any[]>([]);

  /** Screen context, e.g. { customerId } for a delivery site. */
  readonly context = input<any>(null);

  title(): string { return LABEL[this.kind()]; }
  codeLabel(): string {
    const k = this.kind();
    return k === 'driver' ? 'Driver code *' : k === 'supplier' ? 'Supplier code *' : k === 'sparePart' ? 'Part code *'
      : k === 'deliverySite' ? 'Site code *' : k === 'expenseCategory' ? 'Category code *' : k === 'quarry' ? 'Quarry code *' : 'Material code *';
  }
  /** Built once per prefill text: a new object on every change detection would re-run the form's ngOnChanges and wipe what the user typed. */
  readonly prefillRecord = computed(() => ({ name: this.prefill() || '', status: 'ACTIVE', ownerType: 'SELF', creditLimit: 0 }));

  ngOnChanges(): void {
    this.f = { name: this.prefill() || '', defaultUomId: null, defaultRate: 0, reorderLevel: 0 };
    if (this.kind() === 'material') {
      this.http.get<any>('/api/v1/uoms/order-units', { params: { companyId: String(this.companyId) } })
        .subscribe({ next: r => this.uoms.set(r?.data ?? []), error: () => this.uoms.set([]) });
    }
    if (this.kind() === 'sparePart') {
      this.http.get<any>('/api/v1/spare-parts/uoms').subscribe({ next: r => this.uoms.set(r?.data ?? []), error: () => this.uoms.set([]) });
    }
  }

  save(): void {
    const k = this.kind();
    const code = String(this.f.code || '').trim().toUpperCase();
    const name = String(this.f.name || '').trim();
    if (!code || !name) { this.notify.error('Code and name are required.'); return; }
    let url = '', body: any = { code, name, status: 'ACTIVE', companyId: this.companyId };
    if (k === 'driver') {
      const lic = String(this.f.licenseNumber || '').trim().toUpperCase();
      if (!lic) { this.notify.error('Licence number is required.'); return; }
      url = '/api/v1/drivers';
      body = { ...body, licenseNumber: lic, licenseExpiryDate: this.f.licenseExpiryDate || null, phoneNumber: this.f.phoneNumber || null };
    } else if (k === 'supplier') {
      url = '/api/v1/suppliers';
      body = { ...body, phone: this.f.phone || null, gstNumber: String(this.f.gstNumber || '').trim().toUpperCase() || null };
    } else if (k === 'material') {
      url = '/api/v1/materials';
      body = { ...body, defaultUom: this.f.defaultUomId ? { id: this.f.defaultUomId } : null, defaultRate: Number(this.f.defaultRate || 0),
        transportRate: Number(this.f.transportRate || 0), royaltyRate: Number(this.f.royaltyRate || 0), loadingCharge: Number(this.f.loadingCharge || 0) };
    } else if (k === 'quarry') {
      url = '/api/v1/quarries';
      body = { ...body, locationAddress: this.f.locationAddress || null, contactNumber: this.f.contactNumber || null };
    } else if (k === 'deliverySite') {
      const cid = this.context()?.customerId;
      const address = String(this.f.address || '').trim();
      if (!cid) { this.notify.error('Choose the customer first.'); return; }
      if (!address) { this.notify.error('Address is required.'); return; }
      url = `/api/v1/customers/${cid}/delivery-sites`;
      body = { siteCode: code, siteName: name, address, managerName: this.f.managerName || null, status: 'ACTIVE', companyId: this.companyId };
    } else if (k === 'expenseCategory') {
      url = '/api/v1/lookups';
      body = { ...body, type: 'EXPENSE_TYPE' };
    } else {
      if (!this.f.defaultUomId) { this.notify.error('Choose the unit.'); return; }
      url = '/api/v1/spare-parts';
      body = { ...body, defaultUomId: this.f.defaultUomId, defaultRate: Number(this.f.defaultRate || 0), reorderLevel: Number(this.f.reorderLevel || 0) };
    }
    this.saving.set(true);
    this.http.post<any>(url, body).subscribe({
      next: r => {
        this.saving.set(false);
        if (r && r.success === false) { this.notify.error((r.errors && r.errors[0]) || r.message || 'Could not save'); return; }
        this.notify.success(`${name} added`);
        this.saved.emit(r?.data);
      },
      error: e => { this.saving.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not save'); }
    });
  }
}

/** Per-screen state for one open Quick Create popup: start(kind, text, apply) → popup → done(newRecord) → apply. */
export class QuickCreateHost {
  readonly open = signal<{ kind: QuickKind; text: string; context?: any } | null>(null);
  private apply: ((rec: any) => void) | null = null;
  start(kind: QuickKind, text: string, apply: (rec: any) => void, context?: any): void { this.apply = apply; this.open.set({ kind, text: text || '', context }); }
  done(rec: any): void { const a = this.apply; this.open.set(null); this.apply = null; if (rec && rec.id != null && a) a(rec); }
  cancel(): void { this.open.set(null); this.apply = null; }
}
