import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_FOR_CLIENT' | 'RESOLVED' | 'CLOSED';
export const TICKET_STATUSES: { value: TicketStatus; label: string }[] = [
  { value: 'OPEN', label: 'Open' }, { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'WAITING_FOR_CLIENT', label: 'Waiting for client' }, { value: 'RESOLVED', label: 'Resolved' }, { value: 'CLOSED', label: 'Closed' }
];
export const TICKET_PRIORITIES = [
  { value: 'LOW', label: 'Low' }, { value: 'MEDIUM', label: 'Medium' }, { value: 'HIGH', label: 'High' }, { value: 'CRITICAL', label: 'Critical' }
];
export function statusLabel(s: string): string { return TICKET_STATUSES.find(x => x.value === s)?.label || s; }
export function priorityLabel(p: string): string { return TICKET_PRIORITIES.find(x => x.value === p)?.label || p; }

/** Help & Support API (client: /api/v1/support, platform admin: /api/v1/platform-admin/tickets). */
@Injectable({ providedIn: 'root' })
export class SupportService {
  private http = inject(HttpClient);

  // client
  report(body: any): Observable<any> { return this.http.post('/api/v1/support/tickets', body); }
  myTickets(status = '', search = '', page = 0, size = 20): Observable<any> {
    let p = new HttpParams().set('page', page).set('size', size);
    if (status) p = p.set('status', status);
    if (search) p = p.set('search', search);
    return this.http.get('/api/v1/support/tickets', { params: p });
  }
  myTicket(id: number): Observable<any> { return this.http.get(`/api/v1/support/tickets/${id}`); }
  reply(id: number, message: string): Observable<any> { return this.http.post(`/api/v1/support/tickets/${id}/replies`, { message }); }
  confirm(id: number): Observable<any> { return this.http.post(`/api/v1/support/tickets/${id}/confirm`, {}); }
  reopen(id: number, message: string): Observable<any> { return this.http.post(`/api/v1/support/tickets/${id}/reopen`, { message }); }

  attach(id: number, file: File): Observable<any> {
    const fd = new FormData(); fd.append('file', file);
    return this.http.post(`/api/v1/support/tickets/${id}/attachments`, fd);
  }
  /** File bytes (the auth interceptor adds the token, so a plain link would not work). */
  file(id: number, attachmentId: number, admin: boolean): Observable<Blob> {
    const base = admin ? '/api/v1/platform-admin/tickets' : '/api/v1/support/tickets';
    return this.http.get(`${base}/${id}/attachments/${attachmentId}`, { responseType: 'blob' });
  }

  // platform admin
  adminAttach(id: number, file: File, internal: boolean): Observable<any> {
    const fd = new FormData(); fd.append('file', file);
    return this.http.post(`/api/v1/platform-admin/tickets/${id}/attachments`, fd, { params: { internal: String(internal) } });
  }
  adminList(f: any, page = 0, size = 20): Observable<any> {
    let p = new HttpParams().set('page', page).set('size', size);
    for (const k of ['status', 'priority', 'companyId', 'module', 'assignedTo', 'search']) if (f?.[k]) p = p.set(k, f[k]);
    if (f?.overdue) p = p.set('overdue', 'true');
    return this.http.get('/api/v1/platform-admin/tickets', { params: p });
  }
  adminTicket(id: number): Observable<any> { return this.http.get(`/api/v1/platform-admin/tickets/${id}`); }
  adminReply(id: number, message: string, internal: boolean, status?: string): Observable<any> {
    return this.http.post(`/api/v1/platform-admin/tickets/${id}/replies`, { message, internal, status: status || null });
  }
  adminStatus(id: number, status: string, resolution?: string): Observable<any> {
    return this.http.put(`/api/v1/platform-admin/tickets/${id}/status`, { resolution: resolution || null }, { params: { status } });
  }
  adminPriority(id: number, priority: string): Observable<any> {
    return this.http.put(`/api/v1/platform-admin/tickets/${id}/priority`, {}, { params: { priority } });
  }
  adminAssign(id: number, username: string): Observable<any> {
    return this.http.put(`/api/v1/platform-admin/tickets/${id}/assign`, {}, { params: username ? { username } : {} });
  }
  assignees(): Observable<any> { return this.http.get('/api/v1/platform-admin/tickets/assignees'); }
  dashboard(): Observable<any> { return this.http.get('/api/v1/platform-admin/tickets/dashboard'); }

  /** Bell: unread support activity (platform admin: new tickets, client replies, assigned to me, critical count). */
  notifications(admin: boolean): Observable<any> {
    return this.http.get(admin ? '/api/v1/platform-admin/tickets/notifications' : '/api/v1/support/notifications');
  }
}

/**
 * What the user is looking at, for "Report an issue". The app shell sets module / screen from the menu; a screen may
 * add the open record (e.g. { recordType: 'TRIP', recordId: 12, recordLabel: 'TRP-2627/00012' }) and clear it on close.
 */
@Injectable({ providedIn: 'root' })
export class SupportContextService {
  readonly record = signal<{ recordType: string; recordId?: number | null; recordLabel?: string | null } | null>(null);
  readonly reportOpen = signal(false);
  setRecord(recordType: string, recordId?: number | null, recordLabel?: string | null): void { this.record.set({ recordType, recordId, recordLabel }); }
  clearRecord(): void { this.record.set(null); }
  openReport(): void { this.reportOpen.set(true); }
}
