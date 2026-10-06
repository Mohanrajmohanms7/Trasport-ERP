import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

interface Column { key: string; header: string; required: boolean; kind: string; help: string; }
interface RowResult { rowNumber: number; values: Record<string, string>; errors: string[]; }

/**
 * Excel bulk creation: Choose file → Upload (server validates every row) → preview with row errors →
 * Remove invalid → Create (server re-validates; all rows are created together or none).
 * <app-bulk-upload-dialog module="vehicles" title="Vehicles" (done)="reload()" (closed)="..." />
 */
@Component({
  selector: 'app-bulk-upload-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed inset-0 z-[95] flex items-center justify-center p-2 bg-slate-950/70">
      <div class="w-full max-w-6xl max-h-full flex flex-col rounded-xl bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)] shadow-xl" (click)="$event.stopPropagation()" role="dialog" [attr.aria-label]="'Upload ' + title()">
        <header class="flex items-center justify-between gap-3 px-5 py-4 border-b border-[var(--ff-border-divider)]">
          <div>
            <h2 class="text-lg font-bold">Upload {{ title() }} from Excel</h2>
            <p class="text-xs text-[var(--ff-text-secondary)]">Download the template, fill one row per record, then upload. Nothing is saved until you click Create.</p>
          </div>
          <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="closed.emit()" aria-label="Close"><span class="material-icons">close</span></button>
        </header>

        <div class="px-5 py-4 flex flex-wrap items-end gap-3 border-b border-[var(--ff-border-divider)]">
          <button type="button" class="h-10 px-3 rounded-lg border border-[var(--ff-border-default)] text-sm font-semibold inline-flex items-center gap-1.5" (click)="downloadTemplate()">
            <span class="material-icons text-base text-emerald-600">download</span>Download template
          </button>
          <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Choose file (.xlsx)
            <input type="file" accept=".xlsx,.xls" class="text-sm" (change)="pick($event)" />
          </label>
          <button type="button" class="h-10 px-4 rounded-lg bg-slate-800 text-white text-sm font-semibold inline-flex items-center gap-1.5 disabled:opacity-50"
                  [disabled]="!file() || busy()" (click)="upload()">
            <span class="material-icons text-base">upload</span>{{ busy() === 'upload' ? 'Checking…' : 'Upload' }}
          </button>
        </div>

        <div class="flex-1 min-h-0 overflow-auto">
          @if (rows().length) {
            <table class="ff-sticky-actions w-full text-xs border-collapse">
              <thead>
                <tr class="bg-[var(--ff-surface-hover)] text-[var(--ff-text-muted)] font-semibold text-left">
                  <th class="p-2 sticky top-0 bg-[var(--ff-surface-hover)]">Row</th>
                  <th class="p-2 sticky top-0 bg-[var(--ff-surface-hover)]">Status</th>
                  @for (c of columns(); track c.key) { <th class="p-2 sticky top-0 bg-[var(--ff-surface-hover)] whitespace-nowrap">{{ c.header }}{{ c.required ? ' *' : '' }}</th> }
                </tr>
              </thead>
              <tbody>
                @for (r of rows(); track r.rowNumber) {
                  <tr class="border-t border-[var(--ff-border-divider)] align-top" [style.background]="r.errors.length ? 'rgba(220,38,38,0.05)' : null">
                    <td class="p-2 tabular-nums">{{ r.rowNumber }}</td>
                    <td class="p-2 min-w-[220px]">
                      @if (r.errors.length) {
                        <span class="inline-flex items-center gap-1 text-rose-600 font-semibold"><span class="material-icons text-sm">error</span>Invalid</span>
                        <ul class="mt-1 list-disc pl-4 text-rose-700">@for (e of r.errors; track $index) { <li>{{ e }}</li> }</ul>
                      } @else {
                        <span class="inline-flex items-center gap-1 text-emerald-700 font-semibold"><span class="material-icons text-sm">check_circle</span>Valid</span>
                      }
                    </td>
                    @for (c of columns(); track c.key) { <td class="p-2 whitespace-nowrap">{{ r.values[c.key] || '—' }}</td> }
                  </tr>
                }
              </tbody>
            </table>
          } @else {
            <div class="p-10 text-center text-sm text-[var(--ff-text-muted)]">
              Upload a file to see each row checked here — required fields, formats, dropdown values and duplicates.
            </div>
          }
        </div>

        <footer class="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-t border-[var(--ff-border-divider)]">
          <div class="text-sm">
            @if (rows().length) {
              <strong>{{ rows().length }}</strong> row(s): <span class="text-emerald-700 font-semibold">{{ validCount() }} valid</span>,
              <span class="text-rose-600 font-semibold">{{ invalidCount() }} invalid</span>
            }
          </div>
          <div class="flex gap-2">
            <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)] text-sm font-semibold disabled:opacity-50"
                    [disabled]="!invalidCount() || !!busy()" (click)="removeInvalid()">Remove invalid</button>
            <button type="button" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white text-sm font-semibold disabled:opacity-50"
                    [disabled]="!rows().length || invalidCount() > 0 || !!busy()" (click)="create()">
              {{ busy() === 'create' ? 'Creating…' : 'Create ' + validCount() + ' record(s)' }}
            </button>
          </div>
        </footer>
      </div>
    </div>
  `
})
export class BulkUploadDialogComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  readonly module = input.required<string>();
  readonly title = input<string>('records');
  readonly done = output<number>();
  readonly closed = output<void>();

  file = signal<File | null>(null);
  busy = signal<'upload' | 'create' | null>(null);
  columns = signal<Column[]>([]);
  rows = signal<RowResult[]>([]);
  readonly validCount = computed(() => this.rows().filter(r => !r.errors.length).length);
  readonly invalidCount = computed(() => this.rows().filter(r => r.errors.length).length);

  ngOnInit(): void {}

  private err(e: any, fb: string): string {
    const errs = e?.error?.errors;
    const d = Array.isArray(errs) && errs.length ? String(errs[0]) : '';
    const t = e?.error?.message || '';
    return t && d && !d.startsWith(t) ? `${t}: ${d}` : d || t || fb;
  }

  pick(ev: Event): void {
    const f = (ev.target as HTMLInputElement).files?.[0] ?? null;
    this.file.set(f);
    this.rows.set([]);
  }

  downloadTemplate(): void {
    this.http.get(`/api/v1/bulk-import/${this.module()}/template`, { responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.module()}_upload_template.xlsx`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: () => this.notify.error('Could not download the template')
    });
  }

  upload(): void {
    const f = this.file();
    if (!f) return;
    const form = new FormData();
    form.append('file', f);
    this.busy.set('upload');
    this.http.post<any>(`/api/v1/bulk-import/${this.module()}/validate`, form).subscribe({
      next: r => {
        this.busy.set(null);
        this.columns.set(r?.data?.columns ?? []);
        this.rows.set(r?.data?.rows ?? []);
        if (r?.data?.invalid) this.notify.error(`${r.data.invalid} row(s) have errors — fix them in Excel, or click Remove invalid.`);
        else this.notify.success(`All ${r?.data?.valid ?? 0} rows are valid.`);
      },
      error: e => { this.busy.set(null); this.notify.error(this.err(e, 'Upload failed')); }
    });
  }

  removeInvalid(): void {
    this.rows.set(this.rows().filter(r => !r.errors.length));
  }

  create(): void {
    const rows = this.rows();
    if (!rows.length || this.invalidCount()) return;
    this.busy.set('create');
    this.http.post<any>(`/api/v1/bulk-import/${this.module()}/create`, { rows: rows.map(r => r.values) }).subscribe({
      next: r => {
        this.busy.set(null);
        if (r && r.success === false) { this.notify.error(this.err({ error: r }, 'Create failed')); return; }
        const n = r?.data?.created ?? rows.length;
        this.notify.success(`${n} ${this.title().toLowerCase()} created`);
        this.done.emit(n);
      },
      error: e => { this.busy.set(null); this.notify.error(this.err(e, 'Create failed — nothing was saved')); }
    });
  }
}
