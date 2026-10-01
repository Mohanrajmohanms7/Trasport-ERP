import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FfNotificationService } from '../../../shared-ui/infrastructure/services/ff-notification.service';
import { OrderUnitsPanelComponent } from '../../../shared/order-units/order-units-panel';

interface Feature { code: string; parent: string | null; group: string; label: string; description: string; kind: 'MODULE' | 'TAB' | 'ACTION'; core: boolean; }
interface Row { code: string; plan?: boolean; override?: boolean | null; effective?: boolean; enabled?: boolean; }

/**
 * Platform Admin → Feature Access. Decide which modules, inner tabs and actions a plan includes, and override them per client.
 * The server enforces the result on every API call; menus, tabs and buttons are hidden for the client.
 */
@Component({
  selector: 'app-feature-access',
  standalone: true,
  imports: [CommonModule, FormsModule, OrderUnitsPanelComponent],
  templateUrl: './feature-access.html'
})
export class FeatureAccessComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  mode = signal<'client' | 'plan'>('client');
  catalog = signal<Feature[]>([]);
  clients = signal<any[]>([]);
  plans = signal<any[]>([]);
  selectedId = signal<number | null>(null);
  rows = signal<Record<string, Row>>({});
  draft = signal<Record<string, boolean>>({});
  saving = signal(false);
  filter = signal('');

  readonly groups = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const mods = this.catalog().filter(f => f.kind === 'MODULE');
    const byGroup: { group: string; modules: { m: Feature; children: Feature[] }[] }[] = [];
    for (const m of mods) {
      const children = this.catalog().filter(f => f.parent === m.code);
      if (q && !m.label.toLowerCase().includes(q) && !children.some(c => c.label.toLowerCase().includes(q))) continue;
      let g = byGroup.find(x => x.group === m.group);
      if (!g) { g = { group: m.group, modules: [] }; byGroup.push(g); }
      g.modules.push({ m, children });
    }
    return byGroup;
  });

  readonly dirty = computed(() => {
    const d = this.draft(); const r = this.rows();
    return Object.keys(d).some(k => d[k] !== this.baseValue(r[k]));
  });

  readonly offCount = computed(() => Object.values(this.draft()).filter(v => !v).length);

  ngOnInit(): void {
    this.http.get<any>('/api/v1/platform-admin/features/catalog').subscribe(r => this.catalog.set(r?.data ?? []));
    this.http.get<any>('/api/v1/platform-admin/clients', { params: new HttpParams().set('size', '500') })
      .subscribe(r => this.clients.set(r?.data?.content ?? r?.data ?? []));
    this.http.get<any>('/api/v1/platform-admin/plans', { params: new HttpParams().set('size', '200') })
      .subscribe(r => this.plans.set(r?.data?.content ?? r?.data ?? []));
  }

  private baseValue(r?: Row): boolean {
    if (!r) return true;
    return this.mode() === 'plan' ? r.enabled !== false : r.effective !== false;
  }

  setMode(m: 'client' | 'plan'): void {
    this.mode.set(m);
    this.selectedId.set(null);
    this.rows.set({});
    this.draft.set({});
  }

  select(id: any): void {
    const n = id ? Number(id) : null;
    this.selectedId.set(n);
    if (!n) return;
    const url = this.mode() === 'client' ? `/api/v1/platform-admin/companies/${n}/features` : `/api/v1/platform-admin/plans/${n}/features`;
    this.http.get<any>(url).subscribe(r => this.apply(r?.data));
  }

  private apply(data: any): void {
    const rows: Record<string, Row> = {};
    const draft: Record<string, boolean> = {};
    for (const f of data?.features ?? []) {
      rows[f.code] = f;
      // For a client, start from what the client may use today, but keep children's own settings (a module switch-off hides them anyway).
      draft[f.code] = this.mode() === 'plan' ? f.enabled !== false
        : (f.override !== null && f.override !== undefined ? f.override : f.plan !== false);
    }
    this.rows.set(rows);
    this.draft.set(draft);
  }

  value(code: string): boolean { return this.draft()[code] !== false; }

  toggle(f: Feature, on: boolean): void {
    if (f.core) return;
    const d = { ...this.draft(), [f.code]: on };
    if (f.kind === 'MODULE' && on) {
      // switching a module back on also switches its tabs/actions on
      this.catalog().filter(c => c.parent === f.code).forEach(c => d[c.code] = true);
    }
    this.draft.set(d);
  }

  setAll(on: boolean): void {
    const d: Record<string, boolean> = {};
    this.catalog().forEach(f => d[f.code] = f.core ? true : on);
    this.draft.set(d);
  }

  source(code: string): string {
    if (this.mode() !== 'client') return '';
    const r = this.rows()[code];
    if (!r) return '';
    return r.override !== null && r.override !== undefined ? 'client setting' : 'from plan';
  }

  save(): void {
    const id = this.selectedId();
    if (!id) return;
    const url = this.mode() === 'client' ? `/api/v1/platform-admin/companies/${id}/features` : `/api/v1/platform-admin/plans/${id}/features`;
    this.saving.set(true);
    this.http.put<any>(url, { features: this.draft() }).subscribe({
      next: r => { this.saving.set(false); this.apply(r?.data); this.notify.success(r?.message || 'Saved'); },
      error: e => { this.saving.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not save'); }
    });
  }

  resetToPlan(): void {
    const id = this.selectedId();
    if (!id || this.mode() !== 'client') return;
    if (!confirm('Remove all client-specific settings? The client will get exactly what its plan includes.')) return;
    this.http.post<any>(`/api/v1/platform-admin/companies/${id}/features/reset`, {}).subscribe({
      next: r => { this.apply(r?.data); this.notify.success('Client now follows its plan'); },
      error: () => this.notify.error('Could not reset')
    });
  }

  kindLabel(k: string): string { return k === 'TAB' ? 'Tab' : k === 'ACTION' ? 'Action' : 'Module'; }
}
