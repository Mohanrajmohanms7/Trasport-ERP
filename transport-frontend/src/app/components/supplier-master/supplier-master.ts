import { AppConfirmService } from '../../shared/confirmation-dialog/app-confirm.service';
import { FormValidationDirective } from '../../shared/form-validation.directive';
import { FeatureService } from '../../services/feature.service';
import { BulkUploadDialogComponent } from '../../shared/bulk-upload/bulk-upload-dialog';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { resolveTenantCompanyId } from '../../shared/tenant-context';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { ExportButtonsComponent } from '../../shared/export-buttons/export-buttons';
import { AttachmentsPanelComponent } from '../../shared/attachments-panel/attachments-panel';

/** Suppliers: workshops, spare-part shops, tyre dealers, fuel pumps. Credit days drive supplier bill due dates. */
@Component({
  selector: 'app-supplier-master',
  standalone: true,
  imports: [FormValidationDirective, BulkUploadDialogComponent, CommonModule, FormsModule, RouterLink, ExportButtonsComponent, AttachmentsPanelComponent],
  templateUrl: './supplier-master.html'
})
export class SupplierMasterComponent implements OnInit {
  private appConfirm = inject(AppConfirmService);
  /** Subscription feature access (hides tabs/buttons not in the client's plan). */
  readonly features = inject(FeatureService);
  /** Excel bulk creation dialog. */
  showUpload = signal(false);
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private companyId = resolveTenantCompanyId();

  rows = signal<any[]>([]);
  loading = signal(false);
  search = '';
  showForm = signal(false);
  form: any = {};
  docsFor = signal<number | null>(null);

  ngOnInit(): void { this.load(); }

  private err(e: any, fb: string): string {
    const errs = e?.error?.errors;
    return (Array.isArray(errs) && errs[0]) || e?.error?.message || fb;
  }

  load(): void {
    let p = new HttpParams().set('companyId', String(this.companyId)).set('size', '500').set('sort', 'name,asc');
    if (this.search.trim()) p = p.set('search', this.search.trim());
    this.loading.set(true);
    this.http.get<any>('/api/v1/suppliers', { params: p }).subscribe({
      next: r => { this.rows.set(r?.data?.content ?? r?.data ?? []); this.loading.set(false); },
      error: e => { this.loading.set(false); this.notify.error(this.err(e, 'Could not load suppliers')); }
    });
  }

  add(): void {
    const next = 'SUP-' + String(this.rows().length + 1).padStart(3, '0');
    this.form = { code: next, name: '', phone: '', email: '', gstNumber: '', address: '', creditDays: 30, status: 'ACTIVE' };
    this.showForm.set(true);
  }

  edit(r: any): void {
    this.form = { id: r.id, code: r.code, name: r.name, phone: r.phone, email: r.email, gstNumber: r.gstNumber, address: r.address,
      creditDays: r.creditDays, status: r.status, description: r.description };
    this.showForm.set(true);
  }

  save(): void {
    const body = { ...this.form, creditDays: this.form.creditDays === '' || this.form.creditDays == null ? null : Number(this.form.creditDays),
      gstNumber: (this.form.gstNumber || '').trim().toUpperCase() || null, companyId: this.companyId };
    const req = this.form.id ? this.http.put<any>(`/api/v1/suppliers/${this.form.id}`, body) : this.http.post<any>('/api/v1/suppliers', body);
    req.subscribe({
      next: () => { this.showForm.set(false); this.notify.success('Supplier saved'); this.load(); },
      error: e => this.notify.error(this.err(e, 'Supplier could not be saved'))
    });
  }

  toggle(r: any): void {
    this.http.put<any>(`/api/v1/suppliers/${r.id}/toggle-status`, {}).subscribe({
      next: () => this.load(),
      error: e => this.notify.error(this.err(e, 'Could not change status'))
    });
  }

  remove(r: any): void {
    this.appConfirm.ask({ title: 'Delete Supplier', message: `Delete supplier ${r.name}? This cannot be undone.`, type: 'danger', confirmText: 'Delete' }, () => {
      this.http.delete<any>(`/api/v1/suppliers/${r.id}`).subscribe({
        next: () => { this.notify.success('Supplier deleted'); this.load(); },
        error: e => this.notify.error(this.err(e, 'Could not delete'))
      });
    });
  }
}
