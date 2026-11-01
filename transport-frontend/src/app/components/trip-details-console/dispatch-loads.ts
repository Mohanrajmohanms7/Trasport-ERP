import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

interface BLine { materialId: number; materialName: string; uomId: number | null; unit: string; booked: number; onTrips: number; pending: number; perLoad: boolean; transportRate: number; }
interface LLine { materialId: number | null; qty: number | null; transport: number | null; }
interface Load { vehicleId: number | null; driverId: number | null; regularDriverId: number | null; date: string; lines: LLine[]; }

/**
 * Dispatch loads: one lorry load = one trip. Shows each booking material's progress, lets the user add lorry loads
 * (lorry → payload + regular driver + quantity filled in), repeat a lorry, and saves all loads as planned trips in one
 * all-or-nothing request (POST /api/v1/trips/plan-loads). Every server rule still applies.
 */
@Component({
  selector: 'app-dispatch-loads',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div [class]="fullPage ? 'h-full min-h-0 flex flex-col' : 'fixed inset-0 z-[90] flex items-end sm:items-center justify-center bg-slate-950/70 p-0 sm:p-3'" [attr.role]="fullPage ? 'main' : 'dialog'" aria-label="Dispatch loads">
    <div [class]="(fullPage ? 'w-full flex-1 min-h-0 rounded-[10px]' : 'w-full sm:max-w-3xl max-h-[100dvh] sm:max-h-[92vh] rounded-t-2xl sm:rounded-2xl') + ' flex flex-col bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)]'">
      <div class="flex items-start justify-between gap-2 p-4 border-b border-[var(--ff-border-default)]">
        <div class="min-w-0">
          <h2 class="text-base font-bold">Dispatch loads</h2>
          <p class="text-xs text-[var(--ff-text-secondary)]">One lorry load = one trip. Add a load for every lorry you send.</p>
        </div>
        <button type="button" class="h-10 shrink-0 rounded-lg hover:bg-[var(--ff-surface-hover)] px-2 inline-flex items-center gap-1 text-sm" (click)="closed.emit()" [attr.aria-label]="fullPage ? 'Back' : 'Close'">
          <span class="material-icons">{{ fullPage ? 'arrow_back' : 'close' }}</span>@if (fullPage) { <span class="hidden sm:inline">Back</span> }</button>
      </div>

      <div class="flex-1 overflow-y-auto p-4 flex flex-col gap-4" [class.lg:px-8]="fullPage">
        <h3 class="text-xs font-bold uppercase tracking-wider text-[var(--ff-text-muted)]">1 · Booking</h3>
        @if (!bookingId) {
          <label class="flex flex-col gap-1 text-xs font-semibold">Booking
            <select class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm" [ngModel]="chosenBooking()" (ngModelChange)="chooseBooking($event)" data-testid="dl-booking">
              <option [ngValue]="null">Choose an approved booking…</option>
              @for (b of bookings(); track b.id) { <option [ngValue]="b.id">{{ b.bookingNumber }} · {{ b.customer?.name }}</option> }
            </select>
          </label>
        }
        @if (loading()) { <p class="text-sm text-[var(--ff-text-muted)]" role="status">Loading the booking…</p> }
        @if (!loading() && !bookingId && bookingsLoaded() && !bookings().length) { <p class="text-sm text-amber-700">No approved bookings to dispatch. Approve a booking first.</p> }
        @if (bookingTitle()) { <p class="text-sm font-semibold">{{ bookingTitle() }}</p> }

        @if (lines().length) {
          <div class="flex flex-col gap-2" data-testid="dl-progress">
            @for (l of lines(); track l.materialId) {
              <div class="rounded-xl border border-[var(--ff-border-default)] p-3">
                <div class="flex flex-wrap items-baseline justify-between gap-2">
                  <b class="text-sm">{{ l.materialName }}</b>
                  <span class="text-xs" [class.text-rose-600]="remaining(l) < 0" [class.text-emerald-700]="remaining(l) === 0">
                    Remaining <b>{{ fmt(remaining(l)) }} {{ l.unit }}</b>{{ remaining(l) === 0 ? ' — fully planned' : '' }}</span>
                </div>
                <div class="mt-2 h-2 rounded-full bg-[var(--ff-surface-hover)] overflow-hidden" aria-hidden="true">
                  <div class="h-full bg-[var(--ff-color-primary-600)]" [style.width.%]="pct(l)"></div>
                </div>
                <div class="mt-1 text-[11px] text-[var(--ff-text-muted)]">Booked {{ fmt(l.booked) }} · on trips {{ fmt(l.onTrips) }} · this plan {{ fmt(planned(l.materialId)) }} {{ l.unit }}
                  · transport {{ l.perLoad ? 'per load ₹' + fmt(l.transportRate) : 'per ' + l.unit }}</div>
              </div>
            }
          </div>

          <h3 class="text-xs font-bold uppercase tracking-wider text-[var(--ff-text-muted)] mt-2">2 · Lorry loads <span class="normal-case font-normal">(one load = one trip)</span></h3>
          @if (!listsLoaded()) { <p class="text-sm text-[var(--ff-text-muted)]" role="status">Loading lorries and drivers…</p> }
          @if (listsLoaded() && !vehicles().length) { <p class="text-sm text-amber-700">No active lorries found. Add one in Vehicle Master.</p> }
          @if (listsLoaded() && !drivers().length) { <p class="text-sm text-amber-700">No active drivers found. Add one in Driver Master.</p> }
          @for (ld of loads(); track $index; let i = $index) {
            <div class="rounded-xl border border-[var(--ff-border-default)] p-3 flex flex-col gap-3" [attr.data-testid]="'dl-load-' + i">
              <div class="flex items-center justify-between gap-2">
                <b class="text-sm">Load {{ i + 1 }}</b>
                <div class="flex items-center gap-1">
                  <input type="number" min="1" max="20" class="w-14 h-9 rounded-lg border px-1.5 text-center text-sm bg-[var(--ff-surface-card)]" [ngModel]="repeatN[i] ?? 1" (ngModelChange)="repeatN[i] = $event" [attr.aria-label]="'Repeat load ' + (i + 1) + ' times'" />
                  <button type="button" class="h-9 px-2.5 rounded-lg border border-[var(--ff-border-default)] text-xs font-semibold hover:bg-[var(--ff-surface-hover)]" (click)="repeat(i)" title="Add more loads with the same lorry">Repeat ×{{ repeatN[i] || 1 }}</button>
                  <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="removeLoad(i)" [attr.aria-label]="'Remove load ' + (i + 1)"><span class="material-icons text-base">delete</span></button>
                </div>
              </div>
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Lorry *
                  <select class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-[var(--ff-text-primary)]" [ngModel]="ld.vehicleId" (ngModelChange)="setVehicle(i, $event)">
                    <option [ngValue]="null">Choose lorry…</option>
                    @for (v of vehicles(); track v.id) { <option [ngValue]="v.id">{{ v.code }}{{ payloadText(v) }}</option> }
                  </select></label>
                <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Driver *
                  <select class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-[var(--ff-text-primary)]" [ngModel]="ld.driverId" (ngModelChange)="patchLoad(i, { driverId: $event })">
                    <option [ngValue]="null">Choose driver…</option>
                    @for (d of drivers(); track d.id) { <option [ngValue]="d.id">{{ d.name }}{{ d.id === ld.regularDriverId ? ' (regular)' : '' }}</option> }
                  </select>
                  @if (ld.regularDriverId && ld.driverId && ld.driverId !== ld.regularDriverId) { <span class="text-[10px] text-amber-700">Not this lorry's regular driver — used for this trip only.</span> }
                </label>
                <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Date *
                  <input type="date" class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-[var(--ff-text-primary)]" [max]="today" [ngModel]="ld.date" (ngModelChange)="patchLoad(i, { date: $event })" /></label>
              </div>
              @for (ln of ld.lines; track $index; let k = $index) {
                <div class="grid grid-cols-1 sm:grid-cols-[1fr_8rem_9rem_auto] gap-2 items-end">
                  <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Material
                    <select class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-[var(--ff-text-primary)]" [ngModel]="ln.materialId" (ngModelChange)="setLine(i, k, { materialId: $event })">
                      @for (l of lines(); track l.materialId) { <option [ngValue]="l.materialId">{{ l.materialName }}</option> }
                    </select></label>
                  <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Quantity ({{ unitOf(ln.materialId) }})
                    <input type="number" min="0" step="0.001" class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-right text-[var(--ff-text-primary)]" [ngModel]="ln.qty" (ngModelChange)="setLine(i, k, { qty: num($event) })" /></label>
                  @if (perLoad(ln.materialId)) {
                    <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Transport ₹ (this load)
                      <input type="number" min="0" step="0.01" class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-right text-[var(--ff-text-primary)]" [ngModel]="ln.transport" (ngModelChange)="setLine(i, k, { transport: num($event) })" /></label>
                  } @else { <span class="hidden sm:block"></span> }
                  @if (ld.lines.length > 1) {
                    <button type="button" class="h-11 w-11 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="removeLine(i, k)" aria-label="Remove material from this load"><span class="material-icons text-base">remove_circle_outline</span></button>
                  }
                </div>
              }
              @if (loadError(ld); as e) { <p class="text-xs text-rose-600" role="alert">{{ e }}</p> }
              <button type="button" class="self-start text-[11px] font-semibold text-[var(--ff-color-primary-600)] hover:underline" (click)="addLine(i)">+ material in this load (mixed load)</button>
            </div>
          }
          <button type="button" class="h-11 rounded-xl border-2 border-dashed border-[var(--ff-border-default)] text-sm font-semibold hover:bg-[var(--ff-surface-hover)] disabled:opacity-50"
                  [disabled]="allPlanned()" (click)="addLoad()" data-testid="dl-add-load">{{ allPlanned() ? 'Booking fully planned' : '+ Add load' }}</button>
        }
      </div>

      <div class="flex flex-wrap items-center justify-between gap-2 p-4 border-t border-[var(--ff-border-default)]">
        <span class="text-xs text-[var(--ff-text-secondary)]"><b>3 · Review:</b> {{ loads().length }} load(s) → {{ loads().length }} trip(s), saved as Planned{{ summaryText() }}</span>
        <div class="flex gap-2">
          <button type="button" class="h-11 px-4 rounded-lg border border-[var(--ff-border-default)] text-sm" (click)="closed.emit()">Cancel</button>
          <button type="button" class="h-11 px-5 rounded-lg bg-[var(--ff-color-primary-600)] text-white text-sm font-semibold disabled:opacity-50" [disabled]="!canSave()" (click)="save()" data-testid="dl-save">{{ busy() ? 'Saving…' : 'Plan ' + loads().length + ' trip(s)' }}</button>
        </div>
      </div>
    </div>
  </div>`
})
export class DispatchLoadsComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  @Input() bookingId: number | null = null;
  /** Full-screen page (route /dispatch-loads) instead of a popup. */
  @Input() fullPage = false;
  @Output() closed = new EventEmitter<void>();
  @Output() done = new EventEmitter<void>();

  readonly today = new Date().toISOString().slice(0, 10);
  bookings = signal<any[]>([]);
  chosenBooking = signal<number | null>(null);
  lines = signal<BLine[]>([]);
  vehicles = signal<any[]>([]);
  drivers = signal<any[]>([]);
  loads = signal<Load[]>([]);
  loading = signal(false);
  bookingsLoaded = signal(false);
  private vehiclesLoaded = signal(false);
  private driversLoaded = signal(false);
  listsLoaded = computed(() => this.vehiclesLoaded() && this.driversLoaded());
  bookingTitle = signal('');
  busy = signal(false);
  repeatN: Record<number, number> = {};

  ngOnInit(): void {
    const fail = (what: string) => (e: any) => this.notify.error(`Could not load ${what}: ` + (e?.error?.errors?.[0] || e?.error?.message || e?.status || 'error'));
    this.http.get<any>('/api/v1/vehicles', { params: { size: 500, sort: 'code,asc' } }).subscribe({
      next: r => { this.vehicles.set(list(r).filter((v: any) => (v.status || 'ACTIVE') === 'ACTIVE')); this.vehiclesLoaded.set(true); },
      error: e => { this.vehiclesLoaded.set(true); fail('lorries')(e); } });
    this.http.get<any>('/api/v1/drivers', { params: { size: 500, sort: 'name,asc' } }).subscribe({
      next: r => { this.drivers.set(list(r).filter((d: any) => (d.status || 'ACTIVE') === 'ACTIVE')); this.driversLoaded.set(true); },
      error: e => { this.driversLoaded.set(true); fail('drivers')(e); } });
    if (this.bookingId) this.chooseBooking(this.bookingId);
    else this.http.get<any>('/api/v1/bookings', { params: { status: 'APPROVED', size: 200 } }).subscribe({
      next: r => { this.bookings.set(list(r).filter((b: any) => b.status === 'APPROVED')); this.bookingsLoaded.set(true); },
      error: e => { this.bookingsLoaded.set(true); fail('bookings')(e); } });
  }

  chooseBooking(id: number | null): void {
    this.chosenBooking.set(id);
    this.lines.set([]); this.loads.set([]);
    if (!id) return;
    this.loading.set(true);
    this.http.get<any>(`/api/v1/trips/booking-balance/${id}`).subscribe({
      next: r => {
        this.loading.set(false);
        const b = r?.data || {};
        this.bookingTitle.set([b.bookingNumber, b.customerName || b.customer?.name, b.siteName || b.deliverySite?.siteName].filter(Boolean).join(' · '));
        this.lines.set((r?.data?.lines || []).map((l: any) => ({
          materialId: Number(l.materialId), materialName: l.materialName, uomId: l.uom?.id ?? null, unit: l.uom?.label || l.uom?.symbol || 'Unit',
          booked: Number(l.booked || 0), onTrips: Number(l.moved || 0), pending: Number(l.pending ?? l.remaining ?? 0),
          perLoad: l.transportBasis === 'PER_TRIP', transportRate: Number(l.transportRate || 0)
        })));
        if (this.lines().some(l => l.pending > 0)) this.addLoad();
      },
      error: e => { this.loading.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not load the booking'); }
    });
  }

  // ---------- quantities
  planned(materialId: number): number {
    return round3(this.loads().reduce((s, ld) => s + ld.lines.filter(x => x.materialId === materialId).reduce((a, x) => a + (x.qty || 0), 0), 0));
  }
  remaining(l: BLine): number { return round3(l.pending - this.planned(l.materialId)); }
  pct(l: BLine): number { return l.booked > 0 ? Math.min(100, ((l.onTrips + this.planned(l.materialId)) / l.booked) * 100) : 0; }
  allPlanned = computed(() => this.lines().length > 0 && this.lines().every(l => this.remainingOf(l) <= 0));
  private remainingOf(l: BLine): number { return round3(l.pending - this.loads().reduce((s, ld) => s + ld.lines.filter(x => x.materialId === l.materialId).reduce((a, x) => a + (x.qty || 0), 0), 0)); }
  private line(materialId: number | null): BLine | undefined { return this.lines().find(l => l.materialId === materialId); }
  unitOf(materialId: number | null): string { return this.line(materialId)?.unit || ''; }
  perLoad(materialId: number | null): boolean { return !!this.line(materialId)?.perLoad; }
  private vehicle(id: number | null): any { return this.vehicles().find(v => v.id === id); }
  payloadText(v: any): string {
    const p = Number(v?.payloadCapacity || 0);
    return (p > 0 ? ` · ${fmtN(p)} ${v.payloadUom?.symbol || v.payloadUom?.code || ''}` : '') + (v?.wheelCount ? ` · ${v.wheelCount === 'EQUIPMENT' ? 'Equip' : v.wheelCount + 'W'}` : '');
  }
  /** Quantity a new load of this material starts with: what is left, but not more than the lorry carries (same unit). */
  private defaultQty(materialId: number | null, vehicleId: number | null, excludeLoad = -1): number {
    const l = this.line(materialId); if (!l) return 0;
    const used = this.loads().reduce((s, ld, idx) => idx === excludeLoad ? s : s + ld.lines.filter(x => x.materialId === materialId).reduce((a, x) => a + (x.qty || 0), 0), 0);
    let q = Math.max(0, round3(l.pending - used));
    const v = this.vehicle(vehicleId);
    if (v && Number(v.payloadCapacity) > 0 && v.payloadUom?.id && v.payloadUom.id === l.uomId) q = Math.min(q, Number(v.payloadCapacity));
    return round3(q);
  }

  // ---------- editing
  addLoad(): void {
    const m = this.lines().find(l => this.remainingOf(l) > 0) || this.lines()[0];
    const prev = this.loads()[this.loads().length - 1];
    this.loads.update(ls => [...ls, { vehicleId: null, driverId: null, regularDriverId: null, date: prev?.date || this.today,
      lines: [{ materialId: m?.materialId ?? null, qty: null, transport: m?.perLoad ? m.transportRate : null }] }]);
  }
  removeLoad(i: number): void { this.loads.update(ls => ls.filter((_, k) => k !== i)); }
  patchLoad(i: number, p: Partial<Load>): void { this.loads.update(ls => ls.map((x, k) => k === i ? { ...x, ...p } : x)); }
  setLine(i: number, k: number, p: Partial<LLine>): void {
    this.loads.update(ls => ls.map((x, a) => a !== i ? x : { ...x, lines: x.lines.map((y, b) => {
      if (b !== k) return y;
      const n = { ...y, ...p };
      if (p.materialId !== undefined) { const l = this.line(p.materialId); n.transport = l?.perLoad ? l.transportRate : null; n.qty = null; }
      return n;
    }) }));
    if (p.materialId !== undefined) this.fillQty(i, k);
  }
  addLine(i: number): void {
    const used = new Set(this.loads()[i].lines.map(x => x.materialId));
    const m = this.lines().find(l => !used.has(l.materialId) && this.remainingOf(l) > 0) || this.lines().find(l => !used.has(l.materialId));
    if (!m) { this.notify.info('Every material of the booking is already in this load.'); return; }
    this.loads.update(ls => ls.map((x, a) => a !== i ? x : { ...x, lines: [...x.lines, { materialId: m.materialId, qty: null, transport: m.perLoad ? m.transportRate : null }] }));
  }
  removeLine(i: number, k: number): void { this.loads.update(ls => ls.map((x, a) => a !== i ? x : { ...x, lines: x.lines.filter((_, b) => b !== k) })); }

  /** Choosing the lorry fills its regular driver (unless another was picked) and a sensible quantity. */
  setVehicle(i: number, vehicleId: number | null): void {
    const before = this.loads()[i];
    this.patchLoad(i, { vehicleId });
    before.lines.forEach((_, k) => this.fillQty(i, k));
    if (!vehicleId) return;
    this.http.get<any>(`/api/v1/vehicles/${vehicleId}/driver`).subscribe({
      next: r => {
        const a = (r?.data || []).find((x: any) => !x.removalDate && !x.isDeleted && x.driver?.id);
        const cur = this.loads()[i];
        if (!cur || cur.vehicleId !== vehicleId) return;
        const reg = a?.driver?.id ?? null;
        this.patchLoad(i, { regularDriverId: reg, driverId: !cur.driverId || cur.driverId === cur.regularDriverId ? reg : cur.driverId });
      }
    });
  }
  private fillQty(i: number, k: number): void {
    const ld = this.loads()[i]; if (!ld) return;
    const ln = ld.lines[k]; if (!ln) return;
    this.setQtySilently(i, k, this.defaultQty(ln.materialId, ld.vehicleId, i));
  }
  private setQtySilently(i: number, k: number, q: number): void {
    this.loads.update(ls => ls.map((x, a) => a !== i ? x : { ...x, lines: x.lines.map((y, b) => b === k ? { ...y, qty: q || null } : y) }));
  }
  /** Repeat: more loads with the same lorry, driver, date and material until the booking (or N) is reached. */
  repeat(i: number): void {
    const n = Math.max(1, Math.min(20, Number(this.repeatN[i] || 1)));
    const src = this.loads()[i]; if (!src) return;
    for (let r = 0; r < n; r++) {
      const lines = src.lines.map(y => ({ ...y, qty: null as number | null }));
      this.loads.update(ls => [...ls, { ...src, lines }]);
      const idx = this.loads().length - 1;
      lines.forEach((_, k) => this.fillQty(idx, k));
      if (this.loads()[idx].lines.every(y => !y.qty)) { this.removeLoad(idx); break; }   // nothing left to carry
    }
  }

  // ---------- checks (the server checks the same again)
  loadError(ld: Load): string | null {
    if (!ld.vehicleId) return 'Choose the lorry.';
    if (!ld.driverId) return 'Choose the driver.';
    if (!ld.date) return 'Choose the date.';
    if (ld.date > this.today) return 'The trip date cannot be in the future.';
    const seen = new Set<number | null>();
    for (const ln of ld.lines) {
      const l = this.line(ln.materialId);
      if (!l) return 'Choose the material.';
      if (seen.has(ln.materialId)) return `${l.materialName} is already in this load — add another load instead.`;
      seen.add(ln.materialId);
      if (!ln.qty || ln.qty <= 0) return `Enter the quantity of ${l.materialName}.`;
      if (this.remaining(l) < 0) return `Only ${fmtN(l.pending)} ${l.unit} of ${l.materialName} left on this booking — reduce a load.`;
      if (l.perLoad && (ln.transport === null || ln.transport < 0)) return `Enter the transport charge for this load.`;
    }
    const v = this.vehicle(ld.vehicleId);
    if (v && Number(v.payloadCapacity) > 0 && v.payloadUom?.id) {
      const load = ld.lines.filter(x => this.line(x.materialId)?.uomId === v.payloadUom.id).reduce((a, x) => a + (x.qty || 0), 0);
      if (load > Number(v.payloadCapacity)) return `${v.code} carries ${fmtN(Number(v.payloadCapacity))} ${v.payloadUom.symbol || v.payloadUom.code}; this load is ${fmtN(load)}. Add another load for the rest.`;
    }
    return null;
  }
  canSave(): boolean { return !this.busy() && this.loads().length > 0 && this.loads().every(ld => !this.loadError(ld)); }

  save(): void {
    if (!this.canSave()) return;
    const bookingId = this.bookingId || this.chosenBooking();
    this.busy.set(true);
    const body = { bookingId, loads: this.loads().map(ld => ({
      vehicleId: ld.vehicleId, driverId: ld.driverId, tripDate: ld.date,
      confirmDriverOverride: !!(ld.regularDriverId && ld.driverId !== ld.regularDriverId),
      lines: ld.lines.map(x => ({ materialId: x.materialId, quantity: x.qty, transportAmount: this.perLoad(x.materialId) ? x.transport : null }))
    })) };
    this.http.post<any>('/api/v1/trips/plan-loads', body).subscribe({
      next: r => {
        this.busy.set(false);
        const nos = (r?.data || []).map((t: any) => t.tripNumber).join(', ');
        this.notify.success(`${(r?.data || []).length} trip(s) planned: ${nos}`);
        this.done.emit();
      },
      error: e => { this.busy.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not plan the loads'); }
    });
  }

  /** " · M-Sand 10 Unit, Blue Metal 3 Unit" for the review line. */
  summaryText(): string {
    const parts = this.lines().map(l => ({ l, q: this.planned(l.materialId) })).filter(x => x.q > 0).map(x => `${x.l.materialName} ${fmtN(x.q)} ${x.l.unit}`);
    return parts.length ? ' · ' + parts.join(', ') : '';
  }

  num(v: any): number | null { return v === '' || v === null || v === undefined ? null : Number(v); }
  fmt(v: number): string { return fmtN(v); }
}

function list(r: any): any[] { const d = r?.data; return Array.isArray(d) ? d : (d?.content || []); }
function round3(n: number): number { return Math.round((n + Number.EPSILON) * 1000) / 1000; }
function fmtN(n: number): string { return Number.isInteger(n) ? String(n) : String(round3(n)); }
