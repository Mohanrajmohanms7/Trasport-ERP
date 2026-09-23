import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
}

export interface MaintenanceRequest {
  id: number;
  requestNumber: string;
  vehicleId: number;
  vehicleCode?: string;
  vehicleName?: string;
  vehicleRegistrationNumber?: string;
  companyId?: number;
  branchId?: number | null;
  requestedByUserId?: number;
  requestedByUsername?: string;
  driverId?: number | null;
  driverName?: string | null;
  status: string;
  priority: string;
  title: string;
  description: string;
  requestedAt?: string;
  reportedOdometerKm?: number | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  reviewRemarks?: string | null;
  approvedBy?: string | null;
  approvedAt?: string | null;
  workOrderId?: number | null;
  workOrderNumber?: string | null;
  cancelledBy?: string | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
}

export interface AuthorizedVehicle {
  id: number;
  code?: string;
  name?: string;
  companyId?: number;
  branchId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class MaintenanceRequestService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/maintenance-requests';

  list(filters: {
    vehicleId?: number | null;
    status?: string | null;
    priority?: string | null;
    requestedById?: number | null;
    fromDate?: string | null;
    toDate?: string | null;
    branchId?: number | null;
    page?: number;
    size?: number;
  }): Observable<ApiResponse<{ content: MaintenanceRequest[] }>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 50));
    if (filters.vehicleId != null) params = params.set('vehicleId', String(filters.vehicleId));
    if (filters.status) params = params.set('status', filters.status);
    if (filters.priority) params = params.set('priority', filters.priority);
    if (filters.requestedById != null) params = params.set('requestedById', String(filters.requestedById));
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.branchId != null) params = params.set('branchId', String(filters.branchId));
    return this.http.get<ApiResponse<{ content: MaintenanceRequest[] }>>(this.apiUrl, { params });
  }

  get(id: number): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.get<ApiResponse<MaintenanceRequest>>(`${this.apiUrl}/${id}`);
  }

  authorizedVehicles(): Observable<ApiResponse<AuthorizedVehicle[]>> {
    return this.http.get<ApiResponse<AuthorizedVehicle[]>>(`${this.apiUrl}/authorized-vehicles`);
  }

  create(body: { vehicleId: number; title: string; description: string; priority: string }): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.post<ApiResponse<MaintenanceRequest>>(this.apiUrl, body);
  }

  update(id: number, body: { vehicleId?: number; title?: string; description?: string; priority?: string }): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.put<ApiResponse<MaintenanceRequest>>(`${this.apiUrl}/${id}`, body);
  }

  review(id: number, reviewRemarks?: string): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.post<ApiResponse<MaintenanceRequest>>(`${this.apiUrl}/${id}/review`, { reviewRemarks: reviewRemarks || null });
  }

  approve(id: number): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.post<ApiResponse<MaintenanceRequest>>(`${this.apiUrl}/${id}/approve`, {});
  }

  cancel(id: number, cancellationReason: string): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.post<ApiResponse<MaintenanceRequest>>(`${this.apiUrl}/${id}/cancel`, { cancellationReason });
  }

  convert(id: number): Observable<ApiResponse<MaintenanceRequest>> {
    return this.http.post<ApiResponse<MaintenanceRequest>>(`${this.apiUrl}/${id}/convert`, {});
  }

  linkDriver(driverId: number, appUserId: number | null): Observable<ApiResponse<{ appUserId?: number | null; appUserName?: string | null; companyId?: number | null; mappingStatus?: string | null }>> {
    return this.http.put<ApiResponse<{ appUserId?: number | null; appUserName?: string | null; companyId?: number | null; mappingStatus?: string | null }>>(
      `/api/v1/drivers/${driverId}/app-user`,
      { appUserId });
  }
}

export function maintenanceRequestError(err: any): string {
  const errors = err?.error?.errors;
  if (Array.isArray(errors) && errors.length) {
    return errors.filter((item: string) => item && !String(item).startsWith('Action:')).join(' ');
  }
  return err?.error?.message || 'The maintenance request failed.';
}
