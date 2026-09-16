import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FuelEntry {
  id?: number;
  fuelEntryNumber?: string;
  fuelDate?: string;
  vehicle: { id: number; registrationNumber?: string };
  driver: { id: number; name?: string };
  trip?: { id: number; tripNumber?: string };
  fuelRequest?: { id: number; requestNumber?: string };
  fuelStation: string;
  fuelQuantity: number;
  ratePerLitre: number;
  totalAmount?: number;
  paymentMethod: string; // CASH, UPI, BANK, CREDIT
  invoiceNumber?: string;
  currentOdometer: number;
  previousOdometer: number;
  remarks?: string;
  status?: string;
}

export interface FuelRequest {
  id?: number;
  requestNumber?: string;
  trip: { id: number; tripNumber?: string };
  requestedQuantity: number;
  requestedAmount: number;
  fulfilledQuantity?: number;
  fulfilledAmount?: number;
  status?: string; // PENDING, APPROVED, REJECTED, FULFILLED, CANCELLED
  requestedBy?: string;
  approvedBy?: string;
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
export class FuelMgmtService {
  private http = inject(HttpClient);

  // Fuel Entries APIs
  getFuelEntries(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/fuel', { params });
  }

  getFuelEntryById(id: number): Observable<ApiResponse<FuelEntry>> {
    return this.http.get<ApiResponse<FuelEntry>>(`/api/v1/fuel/${id}`);
  }

  createFuelEntry(entry: FuelEntry): Observable<ApiResponse<FuelEntry>> {
    return this.http.post<ApiResponse<FuelEntry>>('/api/v1/fuel', entry);
  }

  updateFuelEntry(id: number, entry: FuelEntry): Observable<ApiResponse<FuelEntry>> {
    return this.http.put<ApiResponse<FuelEntry>>(`/api/v1/fuel/${id}`, entry);
  }

  deleteFuelEntry(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/fuel/${id}`);
  }

  approveFuelEntry(id: number): Observable<ApiResponse<FuelEntry>> {
    return this.http.post<ApiResponse<FuelEntry>>(`/api/v1/fuel/${id}/approve`, {});
  }

  cancelFuelEntry(id: number): Observable<ApiResponse<FuelEntry>> {
    return this.http.post<ApiResponse<FuelEntry>>(`/api/v1/fuel/${id}/cancel`, {});
  }

  // Fuel Requests APIs
  getFuelRequests(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/fuel/request', { params });
  }

  getFuelRequestById(id: number): Observable<ApiResponse<FuelRequest>> {
    return this.http.get<ApiResponse<FuelRequest>>(`/api/v1/fuel/request/${id}`);
  }

  createFuelRequest(req: FuelRequest): Observable<ApiResponse<FuelRequest>> {
    return this.http.post<ApiResponse<FuelRequest>>('/api/v1/fuel/request', req);
  }

  approveFuelRequest(id: number): Observable<ApiResponse<FuelRequest>> {
    return this.http.post<ApiResponse<FuelRequest>>(`/api/v1/fuel/request/${id}/approve`, {});
  }

  rejectFuelRequest(id: number): Observable<ApiResponse<FuelRequest>> {
    return this.http.post<ApiResponse<FuelRequest>>(`/api/v1/fuel/request/${id}/reject`, {});
  }

  cancelFuelRequest(id: number): Observable<ApiResponse<FuelRequest>> {
    return this.http.post<ApiResponse<FuelRequest>>(`/api/v1/fuel/request/${id}/cancel`, {});
  }

  exportXlsx(): Observable<Blob> {
    return this.http.get('/api/v1/fuel-entries/export/xlsx', { responseType: 'blob' });
  }
}
