import { Component, Injectable, OnInit, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { resolveTenantCompanyId } from '../tenant-context';

/**
 * "+ Add New" for dropdowns whose values come from Admin → Dropdown Lists (lookup values).
 * Same rule as the Dropdown Lists screen: only admins may add; values are saved for the logged-in company.
 */
@Injectable({ providedIn: 'root' })
export class LookupAddService {
  private auth = inject(AuthService);
  canAdd(): boolean {
    const roles: string[] = this.auth.currentUser()?.roles || JSON.parse(localStorage.getItem('roles') || '[]');
    return roles.some(r => ['SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN'].includes(r));
  }
  /** "+ Add new payment mode" for admins, null (no button) for everyone else. */
  label(title: string): string | null { return this.canAdd() ? `+ Add new ${title}` : null; }
}

/** One open "Add value" popup per screen: start(type, title, text, apply) → popup → done(newValue) → apply. */
export class LookupAddHost {
  readonly open = signal<{ type: string; title: string; text: string } | null>(null);
  private apply: ((rec: any) => void) | null = null;
  start(type: string, title: string, text: string, apply: (rec: any) => void): void { this.apply = apply; this.open.set({ type, title, text: text || '' }); }
  done(rec: any): void { const a = this.apply; this.open.set(null); this.apply = null; if (rec && a) a(rec); }
  cancel(): void { this.open.set(null); this.apply = null; }
}

@Component({
  selector: 'app-lookup-add',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-[97] flex items-center justify-center p-2 bg-slate-950/70" (click)="closed.emit()">
      <form (ngSubmit)="save()" (click)="$event.stopPropagation()" role="dialog" [attr.aria-label]="'New ' + title()"
            class="w-full max-w-md rounded-xl bg-[var(--ff-surface-card)] border border-[var(--ff-border-default)] p-5 flex flex-col gap-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold">New {{ title() }}</h2>
          <button type="button" class="w-9 h-9 rounded-lg hover:bg-[var(--ff-surface-hover)]" (click)="closed.emit()" aria-label="Close"><span class="material-icons">close</span></button>
        </div>
        <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Name *
          <input required maxlength="150" class="la-in" name="name" [(ngModel)]="name" (ngModelChange)="autoCode()" /></label>
        <label class="flex flex-col gap-1 text-[11px] font-semibold text-[var(--ff-text-muted)]">Code *
          <input required maxlength="50" class="la-in uppercase" name="code" [(ngModel)]="code" (ngModelChange)="codeTouched = true" /></label>
        <p class="text-[11px] text-[var(--ff-text-muted)]">Saved in Admin → Dropdown Lists ({{ type() }}) and available everywhere this list is used.</p>
        <div class="flex justify-end gap-2">
          <button type="button" class="h-10 px-4 rounded-lg border border-[var(--ff-border-default)]" (click)="closed.emit()">Cancel</button>
          <button type="submit" class="h-10 px-4 rounded-lg bg-[var(--ff-color-primary-600)] text-white font-semibold disabled:opacity-60" [disabled]="saving() || !name.trim() || !code.trim()">{{ saving() ? 'Saving…' : 'Save & select' }}</button>
        </div>
      </form>
    </div>`,
  styles: [`.la-in { height: 38px; border: 1px solid var(--ff-border-default); border-radius: 8px; padding: 0 10px; font-size: 13px; font-weight: 400; background: var(--ff-surface-card); color: var(--ff-text-primary); }`]
})
export class LookupAddComponent implements OnInit {
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  readonly type = input.required<string>();
  readonly title = input<string>('value');
  readonly prefill = input<string>('');
  readonly saved = output<any>();
  readonly closed = output<void>();

  name = '';
  code = '';
  codeTouched = false;
  saving = signal(false);

  ngOnInit(): void { this.name = this.prefill() || ''; this.autoCode(); }
  autoCode(): void {
    if (this.codeTouched) return;
    this.code = this.name.trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 50);
  }

  save(): void {
    const name = this.name.trim(), code = this.code.trim().toUpperCase().replace(/[^A-Z0-9_]+/g, '_');
    if (!name || !code || this.saving()) return;
    const companyId = resolveTenantCompanyId();
    this.saving.set(true);
    // Duplicate check (name or code, any case) against the company's existing values of this list
    this.http.get<any>('/api/v1/lookups/list', { params: new HttpParams().set('companyId', String(companyId)).set('type', this.type()) }).subscribe({
      next: r => {
        const rows: any[] = r?.data || [];
        const dup = rows.find(x => String(x.code || '').toUpperCase() === code || String(x.name || '').trim().toLowerCase() === name.toLowerCase());
        if (dup) { this.saving.set(false); this.notify.error(`“${dup.name}” already exists in this list.`); return; }
        this.http.post<any>('/api/v1/lookups', { type: this.type(), code, name, status: 'ACTIVE', companyId }).subscribe({
          next: res => {
            this.saving.set(false);
            if (res?.success === false) { this.notify.error(res?.errors?.[0] || res?.message || 'Could not save'); return; }
            this.notify.success(`${name} added`);
            this.saved.emit(res?.data ?? { type: this.type(), code, name, status: 'ACTIVE' });
          },
          error: e => { this.saving.set(false); this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Could not save'); }
        });
      },
      error: e => { this.saving.set(false); this.notify.error(e?.error?.message || 'Could not check existing values'); }
    });
  }
}
