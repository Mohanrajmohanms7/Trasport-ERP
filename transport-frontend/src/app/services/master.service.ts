import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BaseMaster {
  id?: number;
  code: string;
  name: string;
  description?: string;
  status: string;
  companyId?: number;
  branchId?: number;
  version?: number;
}

export interface LookupValue extends BaseMaster {
  type: string;
  parent?: LookupValue;
}

export interface Vehicle extends BaseMaster {
  chassisNumber?: string;
  engineNumber?: string;
  model?: string;
  brand?: string;
  type?: LookupValue;
  category?: LookupValue;
  capacity?: LookupValue;
  ownerName?: string;
  ownerType?: string;
  purchaseDate?: string;
  insuranceExpiryDate?: string;
  fitnessExpiryDate?: string;
  permitExpiryDate?: string;
}

export interface Driver extends BaseMaster {
  licenseNumber: string;
  licenseExpiryDate?: string;
  phoneNumber?: string;
}

export interface Customer extends BaseMaster {
  email?: string;
  phone?: string;
  address?: string;
  gstNumber?: string;
  creditLimit: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class MasterService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1';

  // Generic Get with Pageable support
  getMasters<T>(endpoint: string, companyId: number, params: { [key: string]: any } = {}): Observable<ApiResponse<PageResponse<T>>> {
    let httpParams = new HttpParams().set('companyId', companyId.toString());
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        httpParams = httpParams.set(key, params[key].toString());
      }
    });
    return this.http.get<ApiResponse<PageResponse<T>>>(`${this.apiUrl}/${endpoint}`, { params: httpParams });
  }

  // Generic Get Single
  getMasterById<T>(endpoint: string, id: number): Observable<ApiResponse<T>> {
    return this.http.get<ApiResponse<T>>(`${this.apiUrl}/${endpoint}/${id}`);
  }

  // Generic Save (Create/Update)
  saveMaster<T>(endpoint: string, data: any): Observable<ApiResponse<T>> {
    if (data.id) {
      return this.http.put<ApiResponse<T>>(`${this.apiUrl}/${endpoint}/${data.id}`, data);
    }
    return this.http.post<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, data);
  }

  updateMaster<T>(endpoint: string, id: number, data: any): Observable<ApiResponse<T>> {
    return this.http.put<ApiResponse<T>>(`${this.apiUrl}/${endpoint}/${id}`, { ...data, id });
  }

  // Generic Delete
  deleteMaster(endpoint: string, id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${endpoint}/${id}`);
  }

  // Generic Toggle Status
  toggleStatus<T>(endpoint: string, id: number): Observable<ApiResponse<T>> {
    return this.http.put<ApiResponse<T>>(`${this.apiUrl}/${endpoint}/${id}/toggle-status`, {});
  }

  // Get Lookup List for dropdowns
  getLookupList(companyId: number, type: string): Observable<ApiResponse<LookupValue[]>> {
    const params = new HttpParams()
      .set('companyId', companyId.toString())
      .set('type', type);
    return this.http.get<ApiResponse<LookupValue[]>>(`${this.apiUrl}/lookups/list`, { params });
  }
}
