import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { resolveTenantCompanyId } from '../shared/tenant-context';

export interface ReportTemplate {
  id?: number;
  templateName: string;
  reportType: string; // FLEET, REVENUE, EXPENSE, TRIP, FUEL
  columnsList: string;
  companyId?: number;
}

export interface ScheduledReport {
  id?: number;
  reportTemplate: { id: number; templateName?: string };
  cronExpression: string;
  recipientEmail: string;
  status?: string; // ACTIVE, INACTIVE
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
export class ReportMgmtService {
  private http = inject(HttpClient);

  private companyParams(extra?: Record<string, unknown>): HttpParams {
    let params = new HttpParams().set('companyId', String(resolveTenantCompanyId()));
    if (extra) {
      Object.entries(extra).forEach(([key, value]) => {
        if (value !== undefined && value !== null && key !== 'companyId') {
          params = params.set(key, String(value));
        }
      });
    }
    return params;
  }

  getTemplates(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/reports', {
      params: this.companyParams(params)
    });
  }

  createTemplate(template: ReportTemplate): Observable<ApiResponse<ReportTemplate>> {
    return this.http.post<ApiResponse<ReportTemplate>>('/api/v1/reports', {
      ...template,
      companyId: resolveTenantCompanyId()
    });
  }

  deleteTemplate(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/reports/${id}`);
  }

  getSchedules(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/reports/schedule', {
      params: this.companyParams(params)
    });
  }

  createSchedule(schedule: ScheduledReport): Observable<ApiResponse<ScheduledReport>> {
    return this.http.post<ApiResponse<ScheduledReport>>('/api/v1/reports/schedule', {
      ...schedule,
      companyId: resolveTenantCompanyId()
    });
  }

  deleteSchedule(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/reports/schedule/${id}`);
  }

  exportReport(data: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>('/api/v1/reports/export', {
      ...data,
      companyId: resolveTenantCompanyId()
    });
  }
}
