import { Component, OnChanges, WritableSignal, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { resolveTenantCompanyId } from '../tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

type Kind = 'vehicle' | 'customer';

/**
 * Create / edit dialog for the core masters that have their own screens (Vehicles, Customers),
 * so each master is created and maintained in exactly one place.
 */
@Component({
  selector: 'app-master-form-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-[95] flex items-center justify-center p-2 bg-slate-950/70" (click)="closed.emit()">
      <form (ngSubmit)="save()" (click)="$event.stopPropagation()"
            class="w-full max-w-2xl max-h-full overflow-y-auto rounded-xl bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)] p-5 flex flex-col gap-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold">{{ record()?.id ? 'Edit' : 'New' }} {{ kind() === 'vehicle' ? 'vehicle' : 'customer' }}</h2>
          <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="closed.emit()" aria-label="Close"><span class="material-icons">close</span></button>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-[11px] font-semibold text-[var(--ff-text-muted)]">
          @if (kind() === 'vehicle') {
            <label class="flex flex-col gap-1">Registration number *<input required class="mf-in uppercase" [(ngModel)]="f.code" name="code" placeholder="TN01AB1234" /></label>
            <label class="flex flex-col gap-1">Display name *<input required class="mf-in" [(ngModel)]="f.name" name="name" placeholder="e.g. Tipper 12 — TN01AB1234" /></label>
            <label class="flex flex-col gap-1">Type<select class="mf-in" [(ngModel)]="f.typeId" name="type"><option [ngValue]="null">—</option>@for (t of types(); track t.id) {<option [ngValue]="t.id">{{ t.name }}</option>}</select></label>
            <label class="flex flex-col gap-1">Category<select class="mf-in" [(ngModel)]="f.categoryId" name="category"><option [ngValue]="null">—</option>@for (t of categories(); track t.id) {<option [ngValue]="t.id">{{ t.name }}</option>}</select></label>
            <label class="flex flex-col gap-1">Capacity<select class="mf-in" [(ngModel)]="f.capacityId" name="capacity"><option [ngValue]="null">—</option>@for (t of capacities(); track t.id) {<option [ngValue]="t.id">{{ t.name }}</option>}</select></label>
            <div class="flex items-end text-[11px] font-normal">Add more types / capacities in Admin → Dropdown Lists.</div>
            <label class="flex flex-col gap-1">Brand<input class="mf-in" [(ngModel)]="f.brand" name="brand" placeholder="Tata, Ashok Leyland…" /></label>
            <label class="flex flex-col gap-1">Model<input class="mf-in" [(ngModel)]="f.model" name="model" /></label>
            <label class="flex flex-col gap-1">Chassis no<input class="mf-in" [(ngModel)]="f.chassisNumber" name="chassis" /></label>
            <label class="flex flex-col gap-1">Engine no<input class="mf-in" [(ngModel)]="f.engineNumber" name="engine" /></label>
            <label class="flex flex-col gap-1">Ownership<select class="mf-in" [(ngModel)]="f.ownerType" name="ownerType"><option value="SELF">Own vehicle</option><option value="HIRED">Hired / attached</option><option value="CLIENT">Client owned</option></select></label>
            <label class="flex flex-col gap-1">Owner name<input class="mf-in" [(ngModel)]="f.ownerName" name="ownerName" /></label>
            <label class="flex flex-col gap-1">Purchase date<input type="date" class="mf-in" [(ngModel)]="f.purchaseDate" name="pd" /></label>
            <label class="flex flex-col gap-1">Based at branch<select class="mf-in" [(ngModel)]="f.branchId" name="branch"><option [ngValue]="null">My branch / head office</option>@for (b of branches(); track b.id) {<option [ngValue]="b.id">{{ b.name }}</option>}</select></label>
            <label class="flex flex-col gap-1">Insurance valid till<input type="date" class="mf-in" [(ngModel)]="f.insuranceExpiryDate" name="ins" /></label>
            <label class="flex flex-col gap-1">Fitness (FC) valid till<input type="date" class="mf-in" [(ngModel)]="f.fitnessExpiryDate" name="fc" /></label>
            <label class="flex flex-col gap-1">Permit valid till<input type="date" class="mf-in" [(ngModel)]="f.permitExpiryDate" name="permit" /></label>
          } @else {
            <label class="flex flex-col gap-1">Customer code *<input required class="mf-in uppercase" [(ngModel)]="f.code" name="code" /></label>
            <label class="flex flex-col gap-1">Customer name *<input required class="mf-in" [(ngModel)]="f.name" name="name" placeholder="e.g. Karnataka Builders" /></label>
            <label class="flex flex-col gap-1">Phone<input class="mf-in" inputmode="tel" [(ngModel)]="f.phone" name="phone" /></label>
            <label class="flex flex-col gap-1">Email<input type="email" class="mf-in" [(ngModel)]="f.email" name="email" /></label>
            <label class="flex flex-col gap-1">GSTIN (decides CGST+SGST or IGST)<input class="mf-in uppercase" maxlength="15" [(ngModel)]="f.gstNumber" name="gst" placeholder="33ABCDE1234F1Z5" /></label>
            <label class="flex flex-col gap-1">Credit limit (₹)<input type="number" min="0" class="mf-in" [(ngModel)]="f.creditLimit" name="cl" /></label>
            <label class="flex flex-col gap-1 sm:col-span-2">Billing address<textarea rows="2" class="mf-in py-2 h-auto" [(ngModel)]="f.address" name="addr"></textarea></label>
          }
          <label class="flex flex-col gap-1">Status<select class="mf-in" [(ngModel)]="f.status" name="status"><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></label>
          <label class="flex flex-col gap-1 sm:col-span-2">Notes<textarea rows="2" class="mf-in py-2 h-auto" [(ngModel)]="f.description" name="desc"></textarea></label>
        </div>
        @if (kind() === 'customer') {
          <p class="text-xs text-[var(--ff-text-secondary)]">Delivery sites, contact persons and documents are managed on the Customers screen after saving.</p>
        }

        <div class="flex justify-end gap-2">
          <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)]" (click)="closed.emit()">Cancel</button>
          <button type="submit" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white font-semibold disabled:opacity-50" [disabled]="saving() || !f.code || !f.name">{{ saving() ? 'Saving…' : 'Save' }}</button>
        </div>
      </form>
    </div>
  `,
  styles: [`.mf-in { height: 40px; border: 1px solid var(--ff-border-default); border-radius: 8px; padding: 0 10px; font-size: 14px; font-weight: 400;
    background: var(--ff-surface-card); color: var(--ff-text-primary); }`]
})
export class MasterFormDialogComponent implements OnChanges {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private companyId = resolveTenantCompanyId();

  readonly kind = input.required<Kind>();
  readonly record = input<any | null>(null);
  readonly saved = output<any>();
  readonly closed = output<void>();

  types = signal<any[]>([]);
  categories = signal<any[]>([]);
  capacities = signal<any[]>([]);
  branches = signal<any[]>([]);
  f: any = {};
  saving = signal(false);

  ngOnChanges(): void {
    const r = this.record();
    this.f = r ? { ...r } : { status: 'ACTIVE', ownerType: 'SELF', creditLimit: 0 };
    if (this.kind() === 'vehicle') {
      this.f.typeId = r?.type?.id ?? null;
      this.f.categoryId = r?.category?.id ?? null;
      this.f.capacityId = r?.capacity?.id ?? null;
      const load = (type: string, target: WritableSignal<any[]>) =>
        this.http.get<any>('/api/v1/lookups/list', { params: { companyId: String(this.companyId), type } })
          .subscribe({ next: res => target.set(res?.data ?? []), error: () => target.set([]) });
      load('VEHICLE_TYPE', this.types);
      load('VEHICLE_CATEGORY', this.categories);
      load('VEHICLE_CAPACITY', this.capacities);
      this.http.get<any>('/api/v1/branches', { params: { companyId: String(this.companyId), size: '200' } })
        .subscribe({ next: res => this.branches.set(res?.data?.content ?? res?.data ?? []), error: () => {} });
      this.f.branchId = r?.branchId ?? null;
    }
  }

  save(): void {
    const url = this.kind() === 'vehicle' ? '/api/v1/vehicles' : '/api/v1/customers';
    const body: any = { ...this.f, code: String(this.f.code || '').trim().toUpperCase(), companyId: this.f.companyId ?? this.companyId };
    if (this.kind() === 'customer') {
      body.gstNumber = (this.f.gstNumber || '').trim().toUpperCase() || null;
      body.creditLimit = Number(this.f.creditLimit || 0);
    }
    if (this.kind() === 'vehicle') {
      body.type = this.f.typeId ? { id: this.f.typeId } : null;
      body.category = this.f.categoryId ? { id: this.f.categoryId } : null;
      body.capacity = this.f.capacityId ? { id: this.f.capacityId } : null;
      delete body.typeId; delete body.categoryId; delete body.capacityId;
    }
    ['purchaseDate', 'insuranceExpiryDate', 'fitnessExpiryDate', 'permitExpiryDate'].forEach(k => { if (body[k] === '') body[k] = null; });
    this.saving.set(true);
    const req = this.f.id ? this.http.put<any>(`${url}/${this.f.id}`, body) : this.http.post<any>(url, body);
    req.subscribe({
      next: r => {
        this.saving.set(false);
        if (r && r.success === false) {
          this.notify.error((r.errors && r.errors[0]) || r.message || 'Could not save');
          return;
        }
        this.notify.success(`${this.kind() === 'vehicle' ? 'Vehicle' : 'Customer'} saved`);
        this.saved.emit(r?.data);
      },
      error: e => {
        this.saving.set(false);
        this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not save');
      }
    });
  }
}
