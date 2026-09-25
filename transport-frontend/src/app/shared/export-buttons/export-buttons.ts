import { Component, inject, input, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

/** "Excel" and "PDF" buttons for any list: <app-export-buttons module="trips" />. Files are built on the server. */
@Component({
  selector: 'app-export-buttons',
  standalone: true,
  template: `
    <div class="inline-flex rounded-lg border border-[var(--ff-border-default)] bg-[var(--ff-surface-card)] overflow-hidden" role="group" aria-label="Export">
      <button type="button" (click)="download('xlsx')" [disabled]="busy() !== null"
              class="h-10 px-3 inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--ff-text-secondary)] hover:bg-[var(--ff-surface-hover)] disabled:opacity-50"
              title="Download as Excel">
        <span class="material-icons text-base text-emerald-600">table_view</span>{{ busy() === 'xlsx' ? 'Preparing…' : 'Excel' }}
      </button>
      <button type="button" (click)="download('pdf')" [disabled]="busy() !== null"
              class="h-10 px-3 inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--ff-text-secondary)] hover:bg-[var(--ff-surface-hover)] border-l border-[var(--ff-border-default)] disabled:opacity-50"
              title="Download as PDF">
        <span class="material-icons text-base text-rose-600">picture_as_pdf</span>{{ busy() === 'pdf' ? 'Preparing…' : 'PDF' }}
      </button>
    </div>
  `
})
export class ExportButtonsComponent {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);

  readonly module = input.required<string>();
  readonly label = input<string>('');
  busy = signal<'xlsx' | 'pdf' | null>(null);

  download(format: 'xlsx' | 'pdf'): void {
    this.busy.set(format);
    const params = new HttpParams().set('format', format);
    this.http.get(`/api/v1/exports/${this.module()}`, { params, responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.label() || this.module()}_${new Date().toISOString().slice(0, 10)}.${format}`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.busy.set(null);
      },
      error: e => {
        this.busy.set(null);
        this.notify.error(e?.status === 403 ? 'Your role cannot export this list.' : 'Export failed. Try again.');
      }
    });
  }
}
