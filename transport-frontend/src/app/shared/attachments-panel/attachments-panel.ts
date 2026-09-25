import { Component, OnChanges, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

interface AttachmentRow {
  id: number;
  fileName: string;
  originalName: string;
  mimeType: string;
  fileSize: number;
  category?: string;
  remarks?: string;
  createdBy?: string;
  createdDate?: string;
}

/**
 * Documents for one record: <app-attachments-panel entityType="INVOICE" [entityId]="invoice.id" />.
 * PDF, JPG, PNG, WEBP (PDF up to 10 MB, images up to 5 MB).
 */
@Component({
  selector: 'app-attachments-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="rounded-[10px] border border-[var(--ff-border-default)] p-3 flex flex-col gap-2">
      <div class="flex items-center justify-between gap-2 flex-wrap">
        <div class="text-sm font-semibold">{{ title() }} <span class="text-[var(--ff-text-muted)] font-normal">({{ rows().length }})</span></div>
        @if (!readonly()) {
          <div class="flex items-center gap-2">
            @if (categories().length) {
              <select class="h-9 rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-input)] text-xs px-2"
                      [value]="category()" (change)="category.set($any($event.target).value)" aria-label="Document type">
                @for (c of categories(); track c) { <option [value]="c">{{ c.replace('_', ' ') | titlecase }}</option> }
              </select>
            }
            <label class="h-9 px-3 inline-flex items-center gap-1.5 rounded-lg bg-[var(--ff-color-primary-600)] text-white text-xs font-semibold cursor-pointer"
                   [class.opacity-50]="uploading()">
              <span class="material-icons text-base">attach_file</span>{{ uploading() ? 'Uploading…' : 'Attach file' }}
              <input type="file" class="hidden" accept=".pdf,.jpg,.jpeg,.png,.webp" (change)="upload($event)" [disabled]="uploading()" />
            </label>
          </div>
        }
      </div>
      @for (a of rows(); track a.id) {
        <div class="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-[var(--ff-surface-hover)]">
          <span class="material-icons text-xl" [style.color]="a.mimeType === 'application/pdf' ? '#dc2626' : '#2653eb'">
            {{ a.mimeType === 'application/pdf' ? 'picture_as_pdf' : 'image' }}
          </span>
          <button type="button" class="min-w-0 flex-1 text-left" (click)="open(a)">
            <div class="text-sm font-medium truncate">{{ a.originalName }}</div>
            <div class="text-[11px] text-[var(--ff-text-muted)] truncate">
              {{ (a.category || 'OTHER').replace('_', ' ') | titlecase }} · {{ size(a.fileSize) }} · {{ a.createdBy }} · {{ a.createdDate | date:'dd MMM yyyy' }}
            </div>
          </button>
          @if (!readonly()) {
            <button type="button" (click)="remove(a)" class="w-9 h-9 flex items-center justify-center rounded-lg text-[var(--ff-text-muted)] hover:text-[var(--ff-color-danger-500)]" [attr.aria-label]="'Remove ' + a.originalName">
              <span class="material-icons text-base">delete</span>
            </button>
          }
        </div>
      } @empty {
        <p class="text-xs text-[var(--ff-text-muted)] px-1 py-2">{{ emptyText() }}</p>
      }
    </section>
  `
})
export class AttachmentsPanelComponent implements OnChanges {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  readonly entityType = input.required<string>();
  readonly entityId = input<number | null | undefined>(null);
  readonly title = input<string>('Attachments');
  readonly categories = input<string[]>([]);
  readonly readonly = input<boolean>(false);
  readonly emptyText = input<string>('No documents attached yet.');

  rows = signal<AttachmentRow[]>([]);
  uploading = signal(false);
  category = signal<string>('');

  ngOnChanges(): void {
    if (!this.category() && this.categories().length) this.category.set(this.categories()[0]);
    this.load();
  }

  load(): void {
    const id = this.entityId();
    if (!id) { this.rows.set([]); return; }
    const params = new HttpParams().set('entityType', this.entityType()).set('entityId', String(id));
    this.http.get<any>('/api/v1/attachments', { params }).subscribe({
      next: r => this.rows.set(r?.data ?? []),
      error: () => this.rows.set([])
    });
  }

  upload(ev: Event): void {
    const inputEl = ev.target as HTMLInputElement;
    const file = inputEl.files?.[0];
    inputEl.value = '';
    const id = this.entityId();
    if (!file || !id) return;
    const form = new FormData();
    form.append('entityType', this.entityType());
    form.append('entityId', String(id));
    form.append('file', file);
    if (this.category()) form.append('category', this.category());
    this.uploading.set(true);
    this.http.post<any>('/api/v1/attachments', form).subscribe({
      next: () => { this.uploading.set(false); this.notify.success('File attached'); this.load(); },
      error: e => {
        this.uploading.set(false);
        const errs = e?.error?.errors;
        this.notify.error((Array.isArray(errs) && errs[0]) || e?.error?.message || 'Upload failed');
      }
    });
  }

  open(a: AttachmentRow): void {
    this.http.get(`/api/v1/files/download/${a.fileName}`, { responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: () => this.notify.error('Could not open the file')
    });
  }

  remove(a: AttachmentRow): void {
    if (!confirm(`Remove ${a.originalName}?`)) return;
    this.http.delete<any>(`/api/v1/attachments/${a.id}`).subscribe({
      next: () => { this.notify.success('Attachment removed'); this.load(); },
      error: e => this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not remove')
    });
  }

  size(bytes: number): string {
    if (!bytes) return '0 KB';
    return bytes > 1024 * 1024 ? (bytes / 1024 / 1024).toFixed(1) + ' MB' : Math.max(1, Math.round(bytes / 1024)) + ' KB';
  }
}
