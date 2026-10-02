import { Injectable, WritableSignal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { resolveTenantCompanyId } from '../shared/tenant-context';

/** Masters with a server-side name / code search and an ACTIVE filter. */
export type PickerMaster = 'customers' | 'vehicles' | 'drivers' | 'materials' | 'quarries' | 'suppliers';

/** search(q) / resolve(id) pair for an ff-dropdown ([remoteSearch] / [resolveMissing]). */
export interface PickerBinding { search: (q: string) => void; resolve: (id: unknown) => void; }

/**
 * Server-side search for large dropdown lists (docs: dropdown review). Results are merged into the screen's own list
 * signal, so the screen's option getters and lookups by id keep working unchanged; only what the user searches for or
 * has selected is loaded, never the whole table.
 */
@Injectable({ providedIn: 'root' })
export class PickerService {
  private http = inject(HttpClient);
  static readonly PAGE = 20;

  private params(extra: Record<string, string | number | boolean | null | undefined>): HttpParams {
    let p = new HttpParams().set('companyId', String(resolveTenantCompanyId()));
    for (const [k, v] of Object.entries(extra)) if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
    return p;
  }
  private rows(o: Observable<any>): Observable<any[]> {
    return o.pipe(map(r => (r?.data?.content ?? r?.data ?? []) as any[]), catchError(() => of([])));
  }

  /** Active records matching the text (name or code), first 20. */
  search(master: PickerMaster, q: string): Observable<any[]> {
    return this.rows(this.http.get(`/api/v1/${master}`, { params: this.params({ search: q, status: 'ACTIVE', size: PickerService.PAGE, page: 0 }) }));
  }
  /** One record by id (any status) — for a saved value that is not in the list. */
  byId(path: string, id: unknown): Observable<any | null> {
    if (id === null || id === undefined || id === '') return of(null);
    return this.http.get<any>(`/api/v1/${path}/${id}`).pipe(map(r => (r?.success === false ? null : r?.data ?? null)), catchError(() => of(null)));
  }
  /** Trips: billable=false → every trip except cancelled; billable=true → completed and not yet invoiced. */
  trips(q: string, billable: boolean): Observable<any[]> {
    return this.rows(this.http.get('/api/v1/trips/picker', { params: this.params({ search: q, billable, size: PickerService.PAGE }) }));
  }
  /** Bookings (not rejected / cancelled), optionally one customer's. */
  bookings(q: string, customerId?: number | null): Observable<any[]> {
    return this.rows(this.http.get('/api/v1/bookings/picker', { params: this.params({ search: q, customerId: customerId || null, size: PickerService.PAGE }) }));
  }
  spareParts(q: string): Observable<any[]> {
    return this.rows(this.http.get('/api/v1/spare-parts', { params: this.params({ search: q, size: PickerService.PAGE, page: 0 }) }));
  }

  /** Adds records to a list signal (by id; existing entries are replaced by the fresh copy). */
  static merge(list: WritableSignal<any[]>, items: any[]): void {
    if (!items?.length) return;
    const byId = new Map<any, any>(list().map(x => [x.id, x]));
    let changed = false;
    for (const it of items) { if (it && it.id != null) { byId.set(it.id, it); changed = true; } }
    if (changed) list.set([...byId.values()]);
  }

  /** Standard binding for a master dropdown whose options are built from `list`. */
  bind(master: PickerMaster, list: WritableSignal<any[]>): PickerBinding {
    return {
      search: (q: string) => this.search(master, q).subscribe(rows => PickerService.merge(list, rows)),
      resolve: (id: unknown) => this.byId(master, id).subscribe(x => x && PickerService.merge(list, [x]))
    };
  }
  bindTrips(list: WritableSignal<any[]>, billable: boolean): PickerBinding {
    return {
      search: (q: string) => this.trips(q, billable).subscribe(rows => PickerService.merge(list, rows)),
      resolve: (id: unknown) => this.byId('trips', id).subscribe(x => x && PickerService.merge(list, [x]))
    };
  }
}

/** Hides INACTIVE records from a dropdown except the one already selected (old records keep showing their value). */
export function activeOrSelected<T extends { id?: any; status?: string }>(rows: T[], selected: unknown | unknown[]): T[] {
  const keep = new Set((Array.isArray(selected) ? selected : [selected]).filter(v => v !== null && v !== undefined && v !== '').map(v => String(v)));
  return rows.filter(r => (r?.status || 'ACTIVE') !== 'INACTIVE' || keep.has(String(r.id)));
}

/** "TRP-2627/00012 — TN46AB1234 — 2026-10-02" */
export function tripLabel(t: any): string {
  return [t?.tripNumber, t?.vehicle?.code || t?.vehicle?.name, t?.tripDate].filter(Boolean).join(' — ');
}
