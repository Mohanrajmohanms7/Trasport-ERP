import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from './work-order.service';

export interface SparePart {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status?: string;
  companyId?: number;
  defaultUomId?: number;
  defaultUomCode?: string;
  reorderLevel?: number | null;
  photoFile?: string | null;
  defaultUomName?: string;
  defaultRate?: number;
}

export interface SparePartPage {
  content: SparePart[];
  totalElements: number;
}

export interface SpareUom {
  id: number;
  code: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class SparePartService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/spare-parts';

  list(page = 0, size = 100): Observable<ApiResponse<SparePartPage>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<ApiResponse<SparePartPage>>(this.apiUrl, { params });
  }

  uoms(): Observable<ApiResponse<SpareUom[]>> {
    return this.http.get<ApiResponse<SpareUom[]>>(`${this.apiUrl}/uoms`);
  }

  create(body: Record<string, unknown>): Observable<ApiResponse<SparePart>> {
    return this.http.post<ApiResponse<SparePart>>(this.apiUrl, body);
  }

  update(id: number, body: Record<string, unknown>): Observable<ApiResponse<SparePart>> {
    return this.http.put<ApiResponse<SparePart>>(`${this.apiUrl}/${id}`, body);
  }
}
