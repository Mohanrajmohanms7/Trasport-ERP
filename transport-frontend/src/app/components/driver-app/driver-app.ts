import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

/**
 * Driver App (DRIVER login): own trips, own bata (owner + customer paid) and fuel requests for own open trips.
 * Phone-first, English with Tamil labels. All data comes from /api/v1/driver-app (own data only, enforced by the server).
 */
@Component({
  selector: 'app-driver-app',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="console-page h-full min-h-0 flex flex-col gap-3 font-sans text-[var(--ff-text-primary)] max-w-3xl w-full mx-auto">
    <div class="flex items-center justify-between gap-2">
      <div>
        <h1 class="text-lg font-bold">{{ me()?.name || 'Driver' }}</h1>
        <p class="text-xs text-[var(--ff-text-muted)]">{{ me()?.code }}{{ lorries() ? ' · ' + lorries() : '' }}</p>
      </div>
    </div>
    <div class="grid grid-cols-3 gap-2" role="tablist">
      @for (t of tabs; track t.key) {
        <button type="button" role="tab" [attr.aria-selected]="tab() === t.key" (click)="go(t.key)"
                class="h-14 rounded-xl border text-sm font-semibold flex flex-col items-center justify-center leading-tight"
                [class.bg-[var(--ff-color-primary-600)]]="tab() === t.key" [class.text-white]="tab() === t.key"
                [class.border-[var(--ff-border-default)]]="tab() !== t.key" [attr.data-testid]="'da-tab-' + t.key">
          <span>{{ t.en }}</span><span class="text-[11px] font-normal opacity-80">{{ t.ta }}</span>
        </button>
      }
    </div>

    <div class="flex-1 min-h-0 overflow-y-auto flex flex-col gap-3 pb-4">
      @if (loading()) { <p class="text-sm text-[var(--ff-text-muted)]" role="status">Loading… · ஏற்றுகிறது…</p> }
      @if (error()) { <p class="text-sm text-rose-600" role="alert">{{ error() }}</p> }

      @if (tab() === 'trips') {
        @if (!loading() && !trips().length && !error()) { <p class="text-sm text-[var(--ff-text-muted)]">No trips in the last 30 days · கடந்த 30 நாட்களில் பயணம் இல்லை</p> }
        @for (t of trips(); track t.id) {
          <div class="rounded-xl border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] p-3 flex flex-col gap-1" data-testid="da-trip">
            <div class="flex items-center justify-between gap-2">
              <b class="text-sm">{{ t.tripNumber }}</b>
              <span class="text-[11px] px-2 py-0.5 rounded-full border">{{ statusText(t.status) }}</span>
            </div>
            <div class="text-xs text-[var(--ff-text-secondary)]">{{ t.tripDate | date:'dd-MM-yyyy' }} · {{ t.vehicle }} · {{ t.customer }}{{ t.site ? ' · ' + t.site : '' }}</div>
            @for (l of t.lines; track $index) {
              <div class="text-xs flex flex-wrap gap-x-3">
                <b>{{ l.material }}</b>
                <span>Planned · திட்டம் {{ l.quantity | number:'1.0-3' }} {{ l.unit }}</span>
                @if (l.loadedQuantity !== null) { <span>Loaded · ஏற்றியது {{ l.loadedQuantity | number:'1.0-3' }}</span> }
                @if (l.deliveredQuantity !== null) { <span>Delivered · இறக்கியது {{ l.deliveredQuantity | number:'1.0-3' }}</span> }
              </div>
            }
            @if (t.customerPaidBata) { <div class="text-xs text-emerald-700">Customer bata · வாடிக்கையாளர் பேட்டா ₹{{ t.customerPaidBata | number:'1.2-2' }}</div> }
            @if (t.remarks) { <div class="text-[11px] text-[var(--ff-text-muted)]">{{ t.remarks }}</div> }
          </div>
        }
      }

      @if (tab() === 'bata') {
        <label class="flex items-center gap-2 text-xs font-semibold">Month · மாதம்
          <input type="month" class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm" [ngModel]="month()" (ngModelChange)="month.set($event); loadBata()" /></label>
        @if (bata(); as b) {
          <div class="grid grid-cols-2 gap-2">
            <div class="rounded-xl border p-3"><div class="text-[11px] text-[var(--ff-text-muted)]">From owner · உரிமையாளர்</div><b class="text-lg">₹{{ b.ownerPaidTotal | number:'1.2-2' }}</b></div>
            <div class="rounded-xl border p-3"><div class="text-[11px] text-[var(--ff-text-muted)]">From customers · வாடிக்கையாளர்</div><b class="text-lg" data-testid="da-bata-customer">₹{{ b.customerPaidTotal | number:'1.2-2' }}</b></div>
          </div>
          @for (r of b.customerPaid; track $index) { <div class="text-xs flex justify-between border-b py-1"><span>{{ r.date | date:'dd-MM' }} · {{ r.tripNumber }} · {{ r.customer }}</span><b>₹{{ r.amount | number:'1.2-2' }}</b></div> }
          @for (r of b.ownerPaid; track $index) { <div class="text-xs flex justify-between border-b py-1"><span>{{ r.date | date:'dd-MM' }} · {{ r.tripNumber || '—' }} · owner</span><b>₹{{ r.amount | number:'1.2-2' }}</b></div> }
          @if (!b.customerPaid.length && !b.ownerPaid.length) { <p class="text-sm text-[var(--ff-text-muted)]">No bata this month · இந்த மாதம் பேட்டா இல்லை</p> }
        }
      }

      @if (tab() === 'fuel') {
        <div class="rounded-xl border border-[var(--ff-border-default)] p-3 flex flex-col gap-3">
          <b class="text-sm">Request fuel · டீசல் கோரிக்கை</b>
          <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Trip · பயணம் *
            <select class="h-11 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] px-2 text-sm text-[var(--ff-text-primary)]" [(ngModel)]="fuelTrip" data-testid="da-fuel-trip">
              <option [ngValue]="null">Choose your open trip…</option>
              @for (t of openTrips(); track t.id) { <option [ngValue]="t.id">{{ t.tripNumber }} · {{ t.vehicle }}</option> }
            </select></label>
          <div class="grid grid-cols-2 gap-2">
            <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Litres · லிட்டர் *
              <input type="number" min="1" step="1" inputmode="numeric" class="h-11 rounded-lg border px-2 text-sm text-right bg-[var(--ff-surface-card)] text-[var(--ff-text-primary)]" [(ngModel)]="fuelLitres" /></label>
            <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Amount ₹ · தொகை
              <input type="number" min="0" step="1" inputmode="numeric" class="h-11 rounded-lg border px-2 text-sm text-right bg-[var(--ff-surface-card)] text-[var(--ff-text-primary)]" [(ngModel)]="fuelAmount" /></label>
          </div>
          @if (!openTrips().length) { <p class="text-xs text-amber-700">No open trip. Fuel can be requested only for a planned or dispatched trip.</p> }
          <button type="button" class="h-12 rounded-xl bg-[var(--ff-color-primary-600)] text-white font-semibold disabled:opacity-50"
                  [disabled]="busy() || !fuelTrip || !(fuelLitres > 0)" (click)="requestFuel()" data-testid="da-fuel-send">{{ busy() ? 'Sending…' : 'Send request · அனுப்பு' }}</button>
        </div>
        @for (f of fuel(); track f.id) {
          <div class="text-xs flex justify-between border-b py-2"><span>{{ f.requestNumber || ('#' + f.id) }} · {{ f.tripNumber }} · {{ f.requestedQuantity | number:'1.0-2' }} L</span><b>{{ statusText(f.status) }}</b></div>
        }
      }
    </div>
  </div>`
})
export class DriverAppComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly tabs = [
    { key: 'trips', en: 'My Trips', ta: 'என் பயணங்கள்' },
    { key: 'bata', en: 'My Bata', ta: 'என் பேட்டா' },
    { key: 'fuel', en: 'Fuel', ta: 'டீசல்' }
  ];
  tab = signal<string>('trips');
  me = signal<any>(null);
  trips = signal<any[]>([]);
  bata = signal<any>(null);
  fuel = signal<any[]>([]);
  loading = signal(false);
  busy = signal(false);
  error = signal('');
  month = signal(new Date().toISOString().slice(0, 7));
  fuelTrip: number | null = null;
  fuelLitres: any = null;
  fuelAmount: any = null;

  ngOnInit(): void {
    this.http.get<any>('/api/v1/driver-app/me').subscribe({ next: r => this.me.set(r?.data), error: e => this.fail(e) });
    this.route.queryParamMap.subscribe(q => { const t = q.get('tab'); this.show(t === 'bata' || t === 'fuel' ? t : 'trips'); });
  }

  lorries(): string { return (this.me()?.vehicles || []).map((v: any) => v.code || v.vehicleCode).filter(Boolean).join(', '); }
  openTrips(): any[] { return this.trips().filter(t => t.status === 'PLANNED' || t.status === 'DISPATCHED'); }
  statusText(s: string): string {
    const m: Record<string, string> = { PLANNED: 'Planned · திட்டம்', DISPATCHED: 'On the way · பயணத்தில்', COMPLETED: 'Completed · முடிந்தது',
      CANCELLED: 'Cancelled · ரத்து', PENDING: 'Waiting · காத்திருப்பு', APPROVED: 'Approved · அனுமதி', REJECTED: 'Rejected · நிராகரிப்பு', FULFILLED: 'Filled · நிரப்பப்பட்டது' };
    return m[s] || s;
  }

  go(key: string): void { this.router.navigate([], { relativeTo: this.route, queryParams: { tab: key === 'trips' ? null : key }, queryParamsHandling: 'merge' }); }

  private show(key: string): void {
    this.tab.set(key);
    this.error.set('');
    if (key === 'trips' || key === 'fuel') this.loadTrips();
    if (key === 'bata') this.loadBata();
    if (key === 'fuel') this.loadFuel();
  }
  private loadTrips(): void {
    this.loading.set(true);
    this.http.get<any>('/api/v1/driver-app/trips').subscribe({ next: r => { this.trips.set(r?.data || []); this.loading.set(false); }, error: e => this.fail(e) });
  }
  loadBata(): void {
    this.loading.set(true);
    this.http.get<any>('/api/v1/driver-app/bata', { params: { month: this.month() } }).subscribe({ next: r => { this.bata.set(r?.data); this.loading.set(false); }, error: e => this.fail(e) });
  }
  private loadFuel(): void {
    this.http.get<any>('/api/v1/driver-app/fuel-requests').subscribe({ next: r => this.fuel.set(r?.data || []), error: e => this.fail(e) });
  }
  requestFuel(): void {
    if (this.busy() || !this.fuelTrip || !(Number(this.fuelLitres) > 0)) return;
    this.busy.set(true);
    this.http.post<any>('/api/v1/driver-app/fuel-requests', { tripId: this.fuelTrip, litres: Number(this.fuelLitres), amount: this.fuelAmount ? Number(this.fuelAmount) : null }).subscribe({
      next: () => { this.busy.set(false); this.notify.success('Fuel request sent · கோரிக்கை அனுப்பப்பட்டது'); this.fuelLitres = null; this.fuelAmount = null; this.loadFuel(); },
      error: e => { this.busy.set(false); this.fail(e); }
    });
  }
  private fail(e: any): void {
    this.loading.set(false);
    this.error.set(e?.error?.errors?.[0] || e?.error?.message || 'Could not load · ஏற்ற முடியவில்லை');
  }
}
