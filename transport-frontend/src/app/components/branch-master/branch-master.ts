import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

/**
 * Branch Master: only branch-level information — code, name, GSTIN (used on invoices billed from the branch),
 * manager, contact, address, location and status. Vehicles, drivers and customers are managed in their own masters;
 * here we only show how many belong to each branch.
 */
@Component({
  selector: 'app-branch-master',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './branch-master.html'
})
export class BranchMasterComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private companyId = resolveTenantCompanyId();

  rows = signal<any[]>([]);
  counts = signal<Record<number, any>>({});
  loading = signal(false);
  search = signal('');
  showForm = signal(false);
  form: any = {};

  readonly filtered = computed(() => {
    const q = this.search().toLowerCase().trim();
    return this.rows().filter(b => !q || [b.code, b.name, b.manager, b.address, b.gstNumber].some(v => (v || '').toLowerCase().includes(q)));
  });

  ngOnInit(): void { this.load(); }

  private err(e: any, fb: string): string {
    const errs = e?.error?.errors;
    return (Array.isArray(errs) && errs[0]) || e?.error?.message || fb;
  }

  load(): void {
    const p = new HttpParams().set('companyId', String(this.companyId)).set('size', '200').set('sort', 'code,asc');
    this.loading.set(true);
    this.http.get<any>('/api/v1/branches', { params: p }).subscribe({
      next: r => {
        if (r?.success === false) this.notify.error(r.errors?.[0] || r.message);
        this.rows.set(r?.data?.content ?? r?.data ?? []);
        this.loading.set(false);
      },
      error: e => { this.loading.set(false); this.notify.error(this.err(e, 'Could not load branches')); }
    });
    this.http.get<any>('/api/v1/branches/summary', { params: new HttpParams().set('companyId', String(this.companyId)) }).subscribe({
      next: r => {
        const m: Record<number, any> = {};
        (r?.data ?? []).forEach((x: any) => m[x.branch_id] = x);
        this.counts.set(m);
      },
      error: () => {}
    });
  }

  add(): void {
    this.form = { code: '', name: '', gstNumber: '', manager: '', phone: '', email: '', address: '', latitude: null, longitude: null, status: 'ACTIVE' };
    this.showForm.set(true);
  }

  edit(b: any): void {
    this.form = { id: b.id, code: b.code, name: b.name, gstNumber: b.gstNumber, manager: b.manager, phone: b.phone, email: b.email,
      address: b.address, latitude: b.latitude, longitude: b.longitude, status: b.status, description: b.description };
    this.showForm.set(true);
  }

  save(): void {
    const body = { ...this.form, companyId: this.companyId, gstNumber: (this.form.gstNumber || '').trim().toUpperCase() || null,
      latitude: this.form.latitude === '' ? null : this.form.latitude, longitude: this.form.longitude === '' ? null : this.form.longitude };
    const req = this.form.id ? this.http.put<any>(`/api/v1/branches/${this.form.id}`, body) : this.http.post<any>('/api/v1/branches', body);
    req.subscribe({
      next: r => {
        if (r?.success === false) { this.notify.error(r.errors?.[0] || r.message); return; }
        this.showForm.set(false);
        this.notify.success('Branch saved');
        this.load();
      },
      error: e => this.notify.error(this.err(e, 'Branch could not be saved'))
    });
  }

  toggle(b: any): void {
    this.http.put<any>(`/api/v1/branches/${b.id}/toggle-status`, {}).subscribe({
      next: r => { if (r?.success === false) this.notify.error(r.errors?.[0] || r.message); this.load(); },
      error: e => this.notify.error(this.err(e, 'Could not change status'))
    });
  }

  remove(b: any): void {
    if (!confirm(`Delete branch ${b.name}?`)) return;
    this.http.delete<any>(`/api/v1/branches/${b.id}`).subscribe({
      next: r => {
        if (r?.success === false) { this.notify.error(r.errors?.[0] || r.message); return; }
        this.notify.success('Branch deleted');
        this.load();
      },
      error: e => this.notify.error(this.err(e, 'Could not delete branch'))
    });
  }

  c(b: any, k: string): number { return Number(this.counts()[b.id]?.[k] ?? 0); }
}
