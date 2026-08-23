import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { resolveTenantCompanyId } from '../shared/tenant-context';

export interface Company {
  id?: number;
  code: string;
  name: string;
  gstNumber?: string;
  panNumber?: string;
  cinNumber?: string;
  phone?: string;
  email?: string;
  website?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  logo?: string;
  digitalSignature?: string;
  status: string;
}

export interface Branch {
  id?: number;
  code: string;
  name: string;
  gstNumber?: string;
  manager?: string;
  phone?: string;
  email?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  status: string;
  companyId?: number;
}

export interface FinancialYear {
  id?: number;
  code: string;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
  isDefault: boolean;
  companyId?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class CompanyAdminService {
  private http = inject(HttpClient);

  /** Logged-in tenant company id (never hardcode company 1). */
  resolveCompanyId(): number {
    return resolveTenantCompanyId();
  }

  private withCompanyParams(params?: Record<string, unknown>): HttpParams {
    let httpParams = new HttpParams().set('companyId', String(this.resolveCompanyId()));
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && key !== 'companyId') {
          httpParams = httpParams.set(key, String(value));
        }
      });
    }
    return httpParams;
  }

  getCompany(): Observable<ApiResponse<Company>> {
    const id = this.resolveCompanyId();
    return this.http.get<ApiResponse<Company>>(`/api/v1/companies/${id}`);
  }

  updateCompany(company: Company): Observable<ApiResponse<Company>> {
    const id = this.resolveCompanyId();
    return this.http.put<ApiResponse<Company>>(`/api/v1/companies/${id}`, company);
  }

  getBranches(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/branches', {
      params: this.withCompanyParams(params)
    });
  }

  createBranch(branch: Branch): Observable<ApiResponse<Branch>> {
    return this.http.post<ApiResponse<Branch>>('/api/v1/branches', {
      ...branch,
      companyId: this.resolveCompanyId()
    });
  }

  updateBranch(id: number, branch: Branch): Observable<ApiResponse<Branch>> {
    return this.http.put<ApiResponse<Branch>>(`/api/v1/branches/${id}`, {
      ...branch,
      companyId: this.resolveCompanyId()
    });
  }

  deleteBranch(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/branches/${id}`);
  }

  getFinancialYears(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/financial-years', {
      params: this.withCompanyParams(params)
    });
  }

  createFinancialYear(fy: FinancialYear): Observable<ApiResponse<FinancialYear>> {
    return this.http.post<ApiResponse<FinancialYear>>('/api/v1/financial-years', {
      ...fy,
      companyId: this.resolveCompanyId()
    });
  }

  updateFinancialYear(id: number, fy: FinancialYear): Observable<ApiResponse<FinancialYear>> {
    return this.http.put<ApiResponse<FinancialYear>>(`/api/v1/financial-years/${id}`, {
      ...fy,
      companyId: this.resolveCompanyId()
    });
  }

  getSettings(): Observable<ApiResponse<Map<string, string>>> {
    return this.http.get<ApiResponse<Map<string, string>>>('/api/v1/settings', {
      params: this.withCompanyParams()
    });
  }

  saveSettings(settings: any): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>('/api/v1/settings', settings, {
      params: this.withCompanyParams()
    });
  }
}
