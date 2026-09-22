import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
}

export interface WorkOrder {
  id: number;
  code?: string;
  workOrderNumber: string;
  name: string;
  description?: string | null;
  status: string;
  companyId?: number;
  branchId?: number | null;
  source: string;
  vehicleId: number;
  vehicleCode?: string;
  vehicleName?: string;
  maintenanceRuleId?: number | null;
  maintenanceRuleName?: string | null;
  maintenanceType?: string;
  triggerMode?: string | null;
  dueStatusAtCreation?: string | null;
  dueKm?: number | null;
  dueDate?: string | null;
  baselineLastServiceKm?: number | null;
  baselineLastServiceDate?: string | null;
  priority: string;
  openedAt?: string;
  startedAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  requestedDate?: string | null;
  odometerAtOpen?: number | null;
  odometerAtComplete?: number | null;
  supplierId?: number | null;
  supplierName?: string | null;
  assignedUserId?: number | null;
  assignedUserName?: string | null;
  diagnosis?: string | null;
  completionNotes?: string | null;
  cancellationReason?: string | null;
  estimatedCost?: number | null;
  actualCost?: number | null;
  attachmentPath?: string | null;
  createdBy?: string | null;
  completedBy?: string | null;
  cancelledBy?: string | null;
}

export interface WorkOrderPage {
  content: WorkOrder[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface WorkOrderQuery {
  vehicleId?: number | null;
  status?: string | null;
  source?: string | null;
  maintenanceType?: string | null;
  branchId?: number | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/work-orders';

  list(query: WorkOrderQuery = {}): Observable<ApiResponse<WorkOrderPage>> {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 20));
    if (query.vehicleId != null) params = params.set('vehicleId', String(query.vehicleId));
    if (query.status) params = params.set('status', query.status);
    if (query.source) params = params.set('source', query.source);
    if (query.maintenanceType) params = params.set('maintenanceType', query.maintenanceType);
    if (query.branchId != null) params = params.set('branchId', String(query.branchId));
    return this.http.get<ApiResponse<WorkOrderPage>>(this.apiUrl, { params });
  }

  get(id: number): Observable<ApiResponse<WorkOrder>> {
    return this.http.get<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}`);
  }

  create(body: Record<string, unknown>): Observable<ApiResponse<WorkOrder>> {
    return this.http.post<ApiResponse<WorkOrder>>(this.apiUrl, body);
  }

  update(id: number, body: Record<string, unknown>): Observable<ApiResponse<WorkOrder>> {
    return this.http.put<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}`, body);
  }

  start(id: number): Observable<ApiResponse<WorkOrder>> {
    return this.http.post<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/start`, {});
  }

  complete(id: number, body: { completionNotes: string; actualCost?: number | null }): Observable<ApiResponse<WorkOrder>> {
    return this.http.post<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/complete`, body);
  }

  cancel(id: number, body: { cancellationReason: string }): Observable<ApiResponse<WorkOrder>> {
    return this.http.post<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/cancel`, body);
  }

  upload(file: File): Observable<ApiResponse<{ fileName: string; originalName?: string }>> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<ApiResponse<{ fileName: string; originalName?: string }>>('/api/v1/files/upload', data);
  }
}

export function workOrderError(err: any): string {
  const code = err?.error?.errorCode ? String(err.error.errorCode) + ': ' : '';
  const errors = err?.error?.errors;
  if (Array.isArray(errors) && errors.length) {
    return code + errors.filter((item: string) => item && !String(item).startsWith('Action:')).join(' ');
  }
  return code + (err?.error?.message || 'The work order request failed.');
}
