import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Family Expenses add-on (docs/FAMILY_EXPENSES.md). Personal register, separate from business expenses. */
export interface FamilyOption { id?: number; code: string; name: string; status?: string; used?: number; }
export interface FamilyExpense {
  id?: number; expenseNumber?: string; expenseDate: string; category: string; categoryName?: string;
  amount: number; paymentMode: string; paymentModeName?: string; description?: string; memberName?: string; referenceNo?: string;
}
export interface FamilyColumn { key: string; label: string; type: 'text' | 'date' | 'money' | 'number' | 'percent'; }
export interface FamilyReport { key: string; title: string; period: string; columns: FamilyColumn[]; rows: Record<string, any>[]; totals: Record<string, number>; }
export interface FamilyFilter { from?: string; to?: string; category?: string; mode?: string; }
interface Api<T> { success: boolean; message?: string; data: T; errors?: string[]; }

@Injectable({ providedIn: 'root' })
export class FamilyExpenseService {
  private http = inject(HttpClient);
  private base = '/api/v1/family-expenses';

  private params(f: FamilyFilter = {}, extra: Record<string, string> = {}): HttpParams {
    let p = new HttpParams();
    for (const [k, v] of Object.entries({ ...f, ...extra })) if (v) p = p.set(k, String(v));
    return p;
  }

  list(f: FamilyFilter, page = 0, size = 200): Observable<Api<{ content: FamilyExpense[]; totalElements: number; totalAmount: number }>> {
    return this.http.get<any>(this.base, { params: this.params(f, { page: String(page), size: String(size) }) });
  }
  options(): Observable<Api<{ categories: FamilyOption[]; paymentModes: FamilyOption[] }>> { return this.http.get<any>(`${this.base}/options`); }
  summary(month?: string): Observable<Api<any>> { return this.http.get<any>(`${this.base}/summary`, { params: this.params({}, month ? { month } : {}) }); }
  create(e: FamilyExpense): Observable<Api<FamilyExpense>> { return this.http.post<any>(this.base, e); }
  update(id: number, e: FamilyExpense): Observable<Api<FamilyExpense>> { return this.http.put<any>(`${this.base}/${id}`, e); }
  remove(id: number): Observable<Api<void>> { return this.http.delete<any>(`${this.base}/${id}`); }

  categories(): Observable<Api<FamilyOption[]>> { return this.http.get<any>(`${this.base}/categories`); }
  addCategory(name: string): Observable<Api<FamilyOption[]>> { return this.http.post<any>(`${this.base}/categories`, { name }); }
  updateCategory(id: number, change: { name?: string; status?: string }): Observable<Api<FamilyOption[]>> { return this.http.put<any>(`${this.base}/categories/${id}`, change); }
  deleteCategory(id: number): Observable<Api<FamilyOption[]>> { return this.http.delete<any>(`${this.base}/categories/${id}`); }

  report(key: string, f: FamilyFilter): Observable<Api<FamilyReport>> { return this.http.get<any>(`${this.base}/reports/${key}`, { params: this.params(f) }); }
  export(key: string, format: 'xlsx' | 'pdf', f: FamilyFilter): Observable<Blob> {
    return this.http.get(`${this.base}/export/${key}`, { params: this.params(f, { format }), responseType: 'blob' });
  }
}
