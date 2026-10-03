import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { SupportContextService, SupportService, TICKET_STATUSES, priorityLabel, statusLabel } from '../../services/support.service';
import { FfNotificationService } from '../../shared-ui/infrastructure/services/ff-notification.service';
import { AuthService } from '../../services/auth.service';

/** Help & Support for client users: My tickets (company admins: all company tickets), detail, reply, confirm / reopen. */
@Component({
  selector: 'app-support-console',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support-console.html'
})
export class SupportConsoleComponent implements OnInit {
  private api = inject(SupportService);
  private notify = inject(FfNotificationService);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  readonly ctx = inject(SupportContextService);

  readonly statuses = TICKET_STATUSES;
  readonly statusLabel = statusLabel;
  readonly priorityLabel = priorityLabel;
  readonly companyAdmin = (this.auth.currentUser()?.roles || []).some((r: string) => ['COMPANY_ADMIN', 'ADMIN', 'SUPER_ADMIN'].includes(r));

  rows = signal<any[]>([]);
  total = signal(0);
  loading = signal(false);
  status = '';
  search = '';
  page = 0;

  selected = signal<any | null>(null);
  reply = '';
  reopenText = '';
  busy = signal(false);

  ngOnInit(): void {
    this.load();
    const t = Number(this.route.snapshot.queryParamMap.get('t'));
    if (t) this.open(t);
  }

  load(): void {
    this.loading.set(true);
    this.api.myTickets(this.status, this.search.trim(), this.page).subscribe({
      next: r => { this.rows.set(r?.data?.content ?? []); this.total.set(r?.data?.totalElements ?? 0); this.loading.set(false); },
      error: () => { this.rows.set([]); this.loading.set(false); }
    });
  }

  open(id: number): void {
    this.reply = ''; this.reopenText = '';
    this.api.myTicket(id).subscribe({ next: r => this.selected.set(r?.data ?? null), error: e => this.fail(e) });
  }

  send(): void {
    const t = this.selected(); if (!t || !this.reply.trim() || this.busy()) return;
    this.run(this.api.reply(t.id, this.reply.trim()), 'Reply sent');
  }
  confirmFixed(): void {
    const t = this.selected(); if (!t || this.busy()) return;
    this.run(this.api.confirm(t.id), 'Thanks — ticket closed');
  }
  stillBroken(): void {
    const t = this.selected(); if (!t || !this.reopenText.trim() || this.busy()) return;
    this.run(this.api.reopen(t.id, this.reopenText.trim()), 'Ticket reopened — support will look again');
  }

  eventText(e: any): string {
    switch (e.action) {
      case 'CREATED': return 'Issue reported';
      case 'STATUS': return `Status: ${statusLabel(e.from)} → ${statusLabel(e.to)}`;
      case 'PRIORITY': return `Priority: ${priorityLabel(e.from)} → ${priorityLabel(e.to)}`;
      case 'REOPENED': return 'Reopened';
      default: return e.action;
    }
  }

  private run(obs: any, ok: string): void {
    this.busy.set(true);
    obs.subscribe({
      next: (r: any) => {
        this.busy.set(false);
        if (r?.success === false) { this.notify.error(r?.errors?.[0] || r?.message || 'Could not save'); return; }
        this.selected.set(r?.data ?? null); this.reply = ''; this.reopenText = '';
        this.notify.success(ok); this.load();
      },
      error: (e: any) => { this.busy.set(false); this.fail(e); }
    });
  }
  private fail(e: any): void { this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Something went wrong'); }
}
