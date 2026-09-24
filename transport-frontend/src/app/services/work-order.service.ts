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
  parts?: WorkOrderPartLine[];
  labour?: WorkOrderLabourLine[];
  partsTotal?: number | null;
  labourTotal?: number | null;
  operationalCost?: number | null;
  financialAmount?: number | null;
  accountingStatus?: string | null;
  journalVoucherId?: number | null;
  journalVoucherReference?: string | null;
  postedAt?: string | null;
}

export interface ServiceHistoryRow {
  id: number;
  vehicleId?: number;
  vehicleRegistrationNumber?: string | null;
  sourceType: string;
  sourceId: number;
  serviceDate?: string | null;
  serviceType?: string | null;
  description?: string | null;
  workOrderNumber?: string | null;
  supplierName?: string | null;
  odometerKm?: number | null;
  estimatedCost?: number | null;
  actualCost?: number | null;
  operationalCost?: number | null;
  financialAmount?: number | null;
  status?: string | null;
}

export interface WorkOrderPartLine {
  id: number;
  sparePartId?: number;
  sparePartCode?: string;
  sparePartName?: string;
  quantity: number;
  issuedQuantity?: number;
  returnedQuantity?: number;
  netIssuedQuantity?: number;
  remainingToIssue?: number;
  returnableQuantity?: number;
  unitRate: number;
  lineTotal: number;
  uomId?: number;
  uomCode?: string;
  uomName?: string;
  notes?: string | null;
}

export interface WorkOrderLabourLine {
  id: number;
  appUserId?: number | null;
  appUserName?: string | null;
  description: string;
  hours: number;
  rate: number;
  lineTotal: number;
  workDate?: string | null;
  notes?: string | null;
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

  addPart(id: number, body: Record<string, unknown>): Observable<ApiResponse<WorkOrder>> {
    return this.http.post<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/parts`, body);
  }

  updatePart(id: number, lineId: number, body: Record<string, unknown>): Observable<ApiResponse<WorkOrder>> {
    return this.http.put<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/parts/${lineId}`, body);
  }

  removePart(id: number, lineId: number): Observable<ApiResponse<WorkOrder>> {
    return this.http.delete<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/parts/${lineId}`);
  }

  addLabour(id: number, body: Record<string, unknown>): Observable<ApiResponse<WorkOrder>> {
    return this.http.post<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/labour`, body);
  }

  updateLabour(id: number, lineId: number, body: Record<string, unknown>): Observable<ApiResponse<WorkOrder>> {
    return this.http.put<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/labour/${lineId}`, body);
  }

  removeLabour(id: number, lineId: number): Observable<ApiResponse<WorkOrder>> {
    return this.http.delete<ApiResponse<WorkOrder>>(`${this.apiUrl}/${id}/labour/${lineId}`);
  }

  serviceHistory(vehicleId: number, page = 0, size = 50): Observable<ApiResponse<{ content: ServiceHistoryRow[] }>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<ApiResponse<{ content: ServiceHistoryRow[] }>>(`/api/v1/vehicles/${vehicleId}/service-history`, { params });
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
