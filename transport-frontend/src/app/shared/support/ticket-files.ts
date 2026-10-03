import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupportService } from '../../services/support.service';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';

export const TICKET_FILE_TYPES = 'image/png,image/jpeg,image/webp,application/pdf';

/** Checks a file before upload (the server checks the real content again). */
export function ticketFileProblem(f: File): string | null {
  const okType = ['image/png', 'image/jpeg', 'image/webp', 'application/pdf'].includes(f.type);
  if (!okType) return `${f.name}: only PNG, JPG, WEBP or PDF`;
  const max = f.type === 'application/pdf' ? 10 : 5;
  if (f.size > max * 1024 * 1024) return `${f.name}: larger than ${max} MB`;
  return null;
}

/** Files on a support ticket: list, open, attach. Admins can attach internal files (never shown to the client). */
@Component({
  selector: 'app-ticket-files',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="flex flex-col gap-2">
    <div class="flex items-center justify-between">
      <span class="text-xs font-bold">Files ({{ files().length }})</span>
      @if (canUpload()) {
        <span class="flex items-center gap-2 text-xs">
          @if (admin()) { <label class="flex items-center gap-1"><input type="checkbox" [(ngModel)]="internal" />Internal</label> }
          <label class="h-8 px-3 rounded-lg border border-[var(--ff-border-default)] inline-flex items-center gap-1 cursor-pointer font-semibold" [class.opacity-60]="busy()">
            <span class="material-icons text-sm">attach_file</span>{{ busy() ? 'Uploading…' : 'Attach' }}
            <input type="file" class="hidden" [accept]="types" multiple (change)="pick($event)" [disabled]="busy()" data-testid="ticket-file-input" />
          </label>
        </span>
      }
    </div>
    @if (files().length) {
      <ul class="flex flex-wrap gap-2">
        @for (f of files(); track f.id) {
          <li>
            <button type="button" (click)="open(f)" class="text-xs rounded-lg border px-2 py-1 inline-flex items-center gap-1 max-w-[260px]"
                    [class.st-note]="f.isInternal" [title]="f.fileName + ' · ' + kb(f.sizeBytes) + ' · ' + f.uploadedBy">
              <span class="material-icons text-sm">{{ f.contentType === 'application/pdf' ? 'picture_as_pdf' : 'image' }}</span>
              <span class="truncate">{{ f.fileName }}</span>
              @if (f.isInternal) { <span class="material-icons text-sm" title="Internal">lock</span> }
            </button>
          </li>
        }
      </ul>
    }
  </div>`
})
export class TicketFilesComponent {
  private api = inject(SupportService);
  private notify = inject(FfNotificationService);

  readonly ticketId = input.required<number>();
  readonly files = input<any[]>([]);
  readonly admin = input<boolean>(false);
  readonly canUpload = input<boolean>(true);
  /** The ticket as returned after the upload (with the new file list). */
  readonly updated = output<any>();

  readonly types = TICKET_FILE_TYPES;
  internal = false;
  busy = signal(false);

  kb(n: number): string { return n > 1024 * 1024 ? (n / 1024 / 1024).toFixed(1) + ' MB' : Math.ceil(n / 1024) + ' KB'; }

  pick(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const list = Array.from(input.files || []);
    input.value = '';
    this.upload(list);
  }

  upload(list: File[]): void {
    const bad = list.map(ticketFileProblem).filter(Boolean);
    if (bad.length) { this.notify.error(bad.join('; ')); }
    const good = list.filter(f => !ticketFileProblem(f));
    if (!good.length) return;
    this.busy.set(true);
    const next = (i: number) => {
      if (i >= good.length) { this.busy.set(false); return; }
      const req = this.admin() ? this.api.adminAttach(this.ticketId(), good[i], this.internal) : this.api.attach(this.ticketId(), good[i]);
      req.subscribe({
        next: r => {
          if (r?.success === false) this.notify.error(r?.errors?.[0] || r?.message || 'Upload failed');
          else { this.updated.emit(r?.data); this.notify.success(good[i].name + ' attached'); }
          next(i + 1);
        },
        error: e => { this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Upload failed'); next(i + 1); }
      });
    };
    next(0);
  }

  open(f: any): void {
    this.api.file(this.ticketId(), f.id, this.admin()).subscribe({
      next: blob => { const url = URL.createObjectURL(blob); window.open(url, '_blank', 'noopener'); setTimeout(() => URL.revokeObjectURL(url), 60000); },
      error: () => this.notify.error('Could not open the file')
    });
  }
}
