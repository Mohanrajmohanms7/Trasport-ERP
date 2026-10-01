import { Component, OnChanges, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UomMgmtService, OrderUnit } from '../../services/uom-mgmt.service';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

/**
 * Order units of one company: which units of measure may be used on bookings / trips / invoices, and the default.
 * Used by Material & Quarry → UOM Master (company admin) and Platform Admin → Feature Access (per client).
 * Lines already saved keep their unit when a unit is switched off. Quantities are never converted.
 */
@Component({
  selector: 'app-order-units-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rounded-[10px] border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)]">
      <div class="px-4 py-3 border-b border-[var(--ff-border-divider)] flex flex-wrap items-center justify-between gap-2">
        <div>
          <div class="text-[11px] font-bold text-[var(--ff-text-muted)] uppercase tracking-[0.05em]">Order units</div>
          <div class="text-xs text-[var(--ff-text-secondary)]">
            Units that can be used on bookings, trips and invoices. Quantities are never converted between units.
          </div>
        </div>
        @if (!canEdit()) {
          <span class="text-[11px] text-[var(--ff-text-muted)]">Only a company admin can change these.</span>
        }
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs border-collapse">
          <thead>
            <tr class="bg-[var(--ff-surface-hover)] text-[var(--ff-text-muted)] uppercase tracking-[0.05em] font-bold">
              <th class="p-3">Unit</th>
              <th class="p-3">Name</th>
              <th class="p-3">Status for orders</th>
              <th class="p-3">Default</th>
            </tr>
          </thead>
          <tbody>
            @for (u of rows(); track u.id) {
              <tr class="border-b border-[var(--ff-border-divider)]">
                <td class="p-3 font-bold text-[var(--ff-text-primary)]">{{ u.label }} <span class="font-mono text-[10px] text-[var(--ff-text-muted)]">{{ u.code }}</span></td>
                <td class="p-3 text-[var(--ff-text-secondary)]">{{ u.name }}@if (u.masterStatus && u.masterStatus !== 'ACTIVE') { <span class="text-[10px] text-[var(--ff-text-muted)]"> (inactive in master)</span> }</td>
                <td class="p-3">
                  <button type="button" [disabled]="!canEdit() || busy() || u.isDefault || (u.masterStatus && u.masterStatus !== 'ACTIVE' && !u.enabled)"
                          (click)="toggle(u)" [attr.aria-pressed]="u.enabled"
                          class="inline-flex items-center gap-1 h-7 px-2.5 rounded-full text-[11px] font-semibold border transition disabled:opacity-60"
                          [ngClass]="u.enabled ? 'bg-[var(--ff-color-success-50)] text-[var(--ff-color-success-500)] border-transparent'
                                               : 'text-[var(--ff-text-secondary)] border-[var(--ff-border-default)]'"
                          [title]="u.isDefault ? 'The default unit is always active' : (u.enabled ? 'Switch off for new orders' : 'Switch on for orders')">
                    <span class="material-icons text-sm">{{ u.enabled ? 'check_circle' : 'radio_button_unchecked' }}</span>
                    {{ u.enabled ? 'Active' : 'Inactive' }}
                  </button>
                </td>
                <td class="p-3">
                  @if (u.isDefault) {
                    <span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[var(--ff-color-primary-50)] text-[var(--ff-color-primary-600)]">Default</span>
                  } @else if (canEdit() && (!u.masterStatus || u.masterStatus === 'ACTIVE')) {
                    <button type="button" [disabled]="busy()" (click)="makeDefault(u)"
                            class="text-[11px] font-semibold text-[var(--ff-color-primary-600)] underline disabled:opacity-60">Make default</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="p-4 text-center text-[var(--ff-text-muted)]">{{ loading() ? 'Loading…' : 'No units in the UOM master.' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class OrderUnitsPanelComponent implements OnChanges {
  private uoms = inject(UomMgmtService);
  private notify = inject(FfNotificationService);

  /** Client company (platform admin). Empty = the signed-in user's company. */
  companyId = input<number | null>(null);
  canEdit = input<boolean>(false);

  rows = signal<OrderUnit[]>([]);
  loading = signal(false);
  busy = signal(false);

  ngOnChanges(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.uoms.getOrderSettings(this.companyId() || undefined).subscribe({
      next: r => { this.rows.set(r?.data ?? []); this.loading.set(false); },
      error: () => { this.rows.set([]); this.loading.set(false); }
    });
  }

  toggle(u: OrderUnit): void { this.save(u, { enabled: !u.enabled }, u.enabled ? `${u.label} switched off for new orders` : `${u.label} can now be used on orders`); }
  makeDefault(u: OrderUnit): void { this.save(u, { isDefault: true }, `${u.label} is now the default order unit`); }

  private save(u: OrderUnit, change: { enabled?: boolean; isDefault?: boolean }, ok: string): void {
    this.busy.set(true);
    this.uoms.updateOrderSetting(u.id, change, this.companyId() || undefined).subscribe({
      next: r => {
        this.busy.set(false);
        if (r?.success === false) { this.notify.error(r?.errors?.[0] || r?.message || 'Could not save'); return; }
        this.rows.set(r?.data ?? []);
        this.notify.success(ok);
      },
      error: e => {
        this.busy.set(false);
        const errors = e?.error?.errors;
        this.notify.error((Array.isArray(errors) && errors[0]) || e?.error?.message || 'Could not save');
      }
    });
  }
}
