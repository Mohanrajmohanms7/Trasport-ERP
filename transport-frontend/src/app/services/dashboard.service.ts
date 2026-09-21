import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
}

export interface MaintenanceDueDashboardSummary {
  overdueCount: number;
  dueCount: number;
  dueSoonCount: number;
  unknownCount: number;
}

export interface MaintenanceDueDashboardItem {
  vehicleId: number;
  vehicleCode?: string;
  vehicleName?: string;
  maintenanceType?: string;
  triggerMode?: string;
  currentOdometerKm?: number | null;
  lastServiceKm?: number | null;
  lastServiceDate?: string | null;
  nextDueKm?: number | null;
  nextDueDate?: string | null;
  remainingKm?: number | null;
  remainingDays?: number | null;
  dueStatus: string;
}

export interface MaintenanceDueDashboardResponse {
  summary: MaintenanceDueDashboardSummary;
  alerts: MaintenanceDueDashboardItem[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/dashboard';

  getAdminMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/admin`);
  }

  getOwnerMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/owner`);
  }

  getOperationsMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/operations`);
  }

  getVehicleMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/vehicle`);
  }

  getAccountMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/account`);
  }

  getDriverMetrics(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/driver`);
  }
}
