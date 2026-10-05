import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FfNotificationService } from '../../../shared-ui/infrastructure/services/ff-notification.service';

/** Platform Admin → Client Plans: plan, dates, usage vs limits, modules, overrides, change plan with preview, history. */
@Component({
  selector: 'app-client-plans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './client-plans.html'
})
export class ClientPlansComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  plans = signal<any[]>([]);
  rows = signal<any[]>([]);
  search = signal('');
  selected = signal<any | null>(null);
  targetPlanId = signal<number | null>(null);
  note = signal('');
  preview = signal<any | null>(null);
  busy = signal(false);
  extras: any = { extraVehicles: 0, extraUsers: 0, extraBranches: 0, billingCycle: 'MONTHLY' };

  readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.rows().filter(r => !q || String(r.name).toLowerCase().includes(q) || String(r.planName || '').toLowerCase().includes(q));
  });

  ngOnInit(): void {
    this.http.get<any>('/api/v1/platform-admin/subscription-plans').subscribe(r => this.plans.set(r?.data ?? []));
    this.load();
  }

  load(): void {
    this.http.get<any>('/api/v1/platform-admin/client-plans').subscribe(r => this.rows.set(r?.data ?? []));
  }

  open(row: any): void {
    this.preview.set(null);
    this.note.set('');
    this.targetPlanId.set(row.planId);
    this.http.get<any>(`/api/v1/platform-admin/client-plans/${row.companyId}`).subscribe(r => {
      const d = r?.data ?? null;
      this.selected.set(d);
      this.extras = { extraVehicles: d?.extras?.vehicles ?? 0, extraUsers: d?.extras?.users ?? 0, extraBranches: d?.extras?.branches ?? 0, billingCycle: d?.billingCycle || 'MONTHLY' };
    });
  }

  close(): void { this.selected.set(null); this.preview.set(null); }

  doPreview(): void {
    const s = this.selected(); const p = this.targetPlanId();
    if (!s || !p) return;
    this.http.get<any>(`/api/v1/platform-admin/client-plans/${s.companyId}/preview`, { params: { planId: String(p) } })
      .subscribe({ next: r => this.preview.set(r?.data ?? null), error: e => this.notify.error(e?.error?.errors?.[0] || 'Preview failed') });
  }

  apply(): void {
    const s = this.selected(); const p = this.targetPlanId();
    if (!s || !p) return;
    this.busy.set(true);
    this.http.put<any>(`/api/v1/platform-admin/client-plans/${s.companyId}`, { planId: p, note: this.note() }).subscribe({
      next: r => { this.busy.set(false); this.selected.set(r?.data ?? null); this.preview.set(null); this.notify.success('Plan applied'); this.load(); },
      error: e => { this.busy.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not apply the plan'); }
    });
  }

  saveExtras(): void {
    const s = this.selected();
    if (!s) return;
    this.http.put<any>(`/api/v1/platform-admin/client-plans/${s.companyId}/extras`, this.extras).subscribe({
      next: r => { this.selected.set(r?.data ?? null); this.notify.success('Extras and billing saved'); this.load(); },
      error: e => this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not save')
    });
  }

  money(v: any): string { return v === null || v === undefined ? '—' : '₹' + Number(v).toLocaleString('en-IN', { maximumFractionDigits: 0 }); }

  limitText(u: any): string {
    if (!u) return '—';
    return u.limit ? `${u.used} / ${u.limit}` : `${u.used} / ∞`;
  }

  over(u: any): boolean { return !!u?.limit && u.used >= u.limit; }

  planLimit(v: number | null | undefined): string { return v && v > 0 ? String(v) : 'Unlimited'; }
}
