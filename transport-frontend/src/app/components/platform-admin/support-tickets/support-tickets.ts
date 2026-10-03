import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { SupportService, TICKET_PRIORITIES, TICKET_STATUSES, priorityLabel, statusLabel } from '../../../services/support.service';
import { FfNotificationService } from '../../../shared-ui/infrastructure/services/ff-notification.service';
import { TicketFilesComponent } from '../../../shared/support/ticket-files';

/** Platform Admin → Support Tickets: dashboard, all clients' tickets, detail with replies, internal notes, status, priority, assignee. */
@Component({
  selector: 'app-support-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, TicketFilesComponent],
  templateUrl: './support-tickets.html'
})
export class SupportTicketsComponent implements OnInit {
  private api = inject(SupportService);
  private http = inject(HttpClient);
  private notify = inject(FfNotificationService);
  private route = inject(ActivatedRoute);

  readonly statuses = TICKET_STATUSES;
  readonly priorities = TICKET_PRIORITIES;
  readonly statusLabel = statusLabel;
  readonly priorityLabel = priorityLabel;

  dash = signal<any | null>(null);
  clients = signal<any[]>([]);
  assignees = signal<any[]>([]);
  rows = signal<any[]>([]);
  total = signal(0);
  loading = signal(false);
  f: any = { status: '', priority: '', companyId: '', module: '', assignedTo: '', search: '', overdue: false };
  page = 0;

  selected = signal<any | null>(null);
  message = '';
  internal = false;
  replyStatus = '';
  resolution = '';
  busy = signal(false);

  ngOnInit(): void {
    this.loadDashboard();
    // Opened from the bell (?t=) or the critical banner (?priority=CRITICAL)
    this.route.queryParamMap.subscribe(q => {
      const pr = q.get('priority'); const t = Number(q.get('t'));
      if (pr) { this.f.priority = pr; this.page = 0; }
      if (t) this.open(t);
      this.load();
    });
    this.http.get<any>('/api/v1/platform-admin/clients', { params: { page: '0', size: '500' } })
      .subscribe({ next: r => this.clients.set(r?.data?.content ?? r?.data ?? []), error: () => {} });
    this.api.assignees().subscribe({ next: r => this.assignees.set(r?.data ?? []), error: () => {} });
  }

  loadDashboard(): void { this.api.dashboard().subscribe({ next: r => this.dash.set(r?.data ?? null), error: () => {} }); }

  load(): void {
    this.loading.set(true);
    this.api.adminList(this.f, this.page).subscribe({
      next: r => { this.rows.set(r?.data?.content ?? []); this.total.set(r?.data?.totalElements ?? 0); this.loading.set(false); },
      error: () => { this.rows.set([]); this.loading.set(false); }
    });
  }
  quick(filter: any): void { this.f = { status: '', priority: '', companyId: '', module: '', assignedTo: '', search: '', overdue: false, ...filter }; this.page = 0; this.load(); }

  open(id: number): void {
    this.message = ''; this.internal = false; this.replyStatus = ''; this.resolution = '';
    this.api.adminTicket(id).subscribe({ next: r => this.selected.set(r?.data ?? null), error: e => this.fail(e) });
  }

  send(): void {
    const t = this.selected(); if (!t || !this.message.trim() || this.busy()) return;
    this.run(this.api.adminReply(t.id, this.message.trim(), this.internal, this.internal ? '' : this.replyStatus),
      this.internal ? 'Internal note added' : 'Reply sent to client');
  }
  setStatus(status: string): void {
    const t = this.selected(); if (!t || this.busy() || status === t.status) return;
    if (status === 'RESOLVED' && !this.resolution.trim()) { this.notify.error('Write the resolution first.'); return; }
    this.run(this.api.adminStatus(t.id, status, this.resolution.trim()), 'Status: ' + statusLabel(status));
  }
  setPriority(p: string): void { const t = this.selected(); if (t && p !== t.priority) this.run(this.api.adminPriority(t.id, p), 'Priority: ' + priorityLabel(p)); }
  assign(u: string): void { const t = this.selected(); if (t && (u || null) !== (t.assignedTo || null)) this.run(this.api.adminAssign(t.id, u), u ? 'Assigned to ' + u : 'Unassigned'); }

  eventText(e: any): string {
    switch (e.action) {
      case 'CREATED': return 'Reported by client';
      case 'STATUS': return `Status ${statusLabel(e.from)} → ${statusLabel(e.to)}`;
      case 'PRIORITY': return `Priority ${priorityLabel(e.from)} → ${priorityLabel(e.to)}`;
      case 'ASSIGNED': return `Assigned ${e.from || '—'} → ${e.to || 'nobody'}`;
      case 'REOPENED': return 'Reopened by client';
      case 'AUTO_CLOSED': return 'Auto-closed (no client answer for 7 days)';
      default: return e.action;
    }
  }

  private run(obs: any, ok: string): void {
    this.busy.set(true);
    obs.subscribe({
      next: (r: any) => {
        this.busy.set(false);
        if (r?.success === false) { this.notify.error(r?.errors?.[0] || r?.message || 'Could not save'); return; }
        this.selected.set(r?.data ?? null); this.message = ''; this.replyStatus = '';
        this.notify.success(ok); this.load(); this.loadDashboard();
      },
      error: (e: any) => { this.busy.set(false); this.fail(e); }
    });
  }
  private fail(e: any): void { this.notify.error(e?.error?.errors?.[0] || e?.error?.message || 'Something went wrong'); }
}
