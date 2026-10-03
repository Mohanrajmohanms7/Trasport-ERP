import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SupportContextService, SupportService, TICKET_PRIORITIES } from '../../services/support.service';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { environment } from '../../../environments/environment';

/** "Report an issue" popup, opened from the header on any screen. Context is captured automatically. */
@Component({
  selector: 'app-report-issue-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
  <div class="fixed inset-0 z-[96] flex items-center justify-center p-2 bg-slate-950/70" (click)="closed.emit()">
    <div class="w-full max-w-xl max-h-full overflow-y-auto rounded-xl bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)] p-5 flex flex-col gap-4"
         (click)="$event.stopPropagation()" role="dialog" aria-label="Report an issue">
      @if (created(); as t) {
        <div class="flex flex-col items-center text-center gap-3 py-4">
          <span class="material-icons text-5xl text-emerald-500">check_circle</span>
          <h2 class="text-lg font-bold">Issue reported</h2>
          <p class="text-sm text-[var(--ff-text-secondary)]">Your ticket number is</p>
          <p class="text-2xl font-mono font-bold tracking-wide" data-testid="ticket-number">{{ t.ticketNumber }}</p>
          <p class="text-xs text-[var(--ff-text-muted)]">Support will reply here. You can follow it in Help &amp; Support → My tickets.</p>
          <div class="flex gap-2 mt-2">
            <a routerLink="/support" [queryParams]="{ t: t.id }" (click)="closed.emit()" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white font-semibold inline-flex items-center">Open ticket</a>
            <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)]" (click)="closed.emit()">Done</button>
          </div>
        </div>
      } @else {
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold flex items-center gap-2"><span class="material-symbols-outlined">support_agent</span>Report an issue</h2>
          <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="closed.emit()" aria-label="Close"><span class="material-icons">close</span></button>
        </div>
        <form (ngSubmit)="submit()" class="flex flex-col gap-3 text-[11px] font-semibold text-[var(--ff-text-muted)]">
          <label class="flex flex-col gap-1">Title *<input required maxlength="255" class="ri-in" name="subject" [(ngModel)]="subject" placeholder="e.g. Unable to complete trip" /></label>
          <label class="flex flex-col gap-1">What happened? *
            <textarea required rows="4" class="ri-in ri-area" name="description" [(ngModel)]="description" placeholder="What did you do, what did you expect, what happened instead?"></textarea></label>
          <label class="flex flex-col gap-1">Steps to repeat it (optional)
            <textarea rows="2" class="ri-in ri-area" name="steps" [(ngModel)]="steps"></textarea></label>
          <label class="flex flex-col gap-1">Priority
            <select class="ri-in" name="priority" [(ngModel)]="priority">
              @for (p of priorities; track p.value) { <option [value]="p.value">{{ p.label }}</option> }
            </select></label>
          @if (priority === 'CRITICAL') {
            <p class="text-[var(--ff-color-danger-500)] font-normal">Critical = work has stopped (e.g. nobody can log in, invoices cannot be saved). Support is alerted straight away.</p>
          }
          <div class="rounded-lg bg-[var(--ff-surface-hover)] p-3 font-normal text-[var(--ff-text-secondary)] grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
            <span class="font-semibold">Module</span><span>{{ module() || '—' }}</span>
            <span class="font-semibold">Screen</span><span>{{ screen() || '—' }}</span>
            @if (ctx.record(); as r) { <span class="font-semibold">Record</span><span>{{ r.recordLabel || (r.recordType + ' #' + r.recordId) }}</span> }
            <span class="font-semibold">Page</span><span class="truncate">{{ pageUrl }}</span>
            <span class="font-semibold">Version</span><span>{{ version }}</span>
            <span class="col-span-2 text-[10px] text-[var(--ff-text-muted)] mt-1">Sent with your report, with your company, user name, browser and the time. TransaFlow support can read what you write here.</span>
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)] text-[var(--ff-text-primary)]" (click)="closed.emit()">Cancel</button>
            <button type="submit" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white font-semibold disabled:opacity-60" [disabled]="saving() || !subject.trim() || !description.trim()">{{ saving() ? 'Sending…' : 'Send to support' }}</button>
          </div>
        </form>
      }
    </div>
  </div>`,
  styles: [`.ri-in { min-height: 38px; border: 1px solid var(--ff-border-default); border-radius: 8px; padding: 8px 10px; font-size: 13px; font-weight: 400; background: var(--ff-surface-card); color: var(--ff-text-primary); } .ri-area { resize: vertical; }`]
})
export class ReportIssueDialogComponent implements OnInit {
  private api = inject(SupportService);
  private notify = inject(FfNotificationService);
  readonly ctx = inject(SupportContextService);

  readonly module = input<string>('');
  readonly screen = input<string>('');
  readonly closed = output<void>();

  readonly priorities = TICKET_PRIORITIES;
  subject = '';
  description = '';
  steps = '';
  priority = 'MEDIUM';
  pageUrl = '';
  version = environment.version;
  saving = signal(false);
  created = signal<any | null>(null);

  ngOnInit(): void { this.pageUrl = location.pathname + location.search; }

  submit(): void {
    if (this.saving()) return;   // no double tickets on a double click
    const r = this.ctx.record();
    const description = this.description.trim() + (this.steps.trim() ? '\n\nSteps to repeat:\n' + this.steps.trim() : '');
    this.saving.set(true);
    this.api.report({
      subject: this.subject.trim(), description, priority: this.priority,
      module: this.module() || null, screen: this.screen() || null, pageUrl: this.pageUrl, appVersion: this.version,
      recordType: r?.recordType || null, recordId: r?.recordId ?? null, recordLabel: r?.recordLabel || null
    }).subscribe({
      next: res => {
        this.saving.set(false);
        if (res?.success === false) { this.notify.error(res?.errors?.[0] || res?.message || 'Could not send'); return; }
        this.created.set(res?.data);
      },
      error: e => { this.saving.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not send'); }
    });
  }
}
