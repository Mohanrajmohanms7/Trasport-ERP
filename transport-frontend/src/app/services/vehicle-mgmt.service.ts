import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VehicleDocument {
  id?: number;
  docType: string;
  docNumber: string;
  expiryDate: string;
  filePath?: string;
  description?: string;
}

export interface VehicleServiceLog {
  id?: number;
  serviceType: string;
  serviceDate: string;
  nextServiceDate?: string;
  workshop?: string;
  cost: number;
  remarks?: string;
}

export interface VehicleDriverAssignment {
  id?: number;
  driverId: number;
  driverName?: string;
  assignmentDate: string;
  removalDate?: string;
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
export class VehicleMgmtService {
  private http = inject(HttpClient);

  // Documents APIs
  getDocuments(vehicleId: number): Observable<ApiResponse<VehicleDocument[]>> {
    return this.http.get<ApiResponse<VehicleDocument[]>>(`/api/v1/vehicles/${vehicleId}/documents`);
  }

  addDocument(vehicleId: number, doc: VehicleDocument): Observable<ApiResponse<VehicleDocument>> {
    return this.http.post<ApiResponse<VehicleDocument>>(`/api/v1/vehicles/${vehicleId}/documents`, doc);
  }

  deleteDocument(vehicleId: number, id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/vehicles/${vehicleId}/documents/${id}`);
  }

  // Maintenance Service logs APIs
  getMaintenanceHistory(vehicleId: number): Observable<ApiResponse<VehicleServiceLog[]>> {
    return this.http.get<ApiResponse<VehicleServiceLog[]>>(`/api/v1/vehicles/${vehicleId}/maintenance`);
  }

  addServiceLog(vehicleId: number, log: VehicleServiceLog): Observable<ApiResponse<VehicleServiceLog>> {
    return this.http.post<ApiResponse<VehicleServiceLog>>(`/api/v1/vehicles/${vehicleId}/service`, log);
  }

  // Driver Assignments APIs
  getAssignments(vehicleId: number): Observable<ApiResponse<VehicleDriverAssignment[]>> {
    return this.http.get<ApiResponse<VehicleDriverAssignment[]>>(`/api/v1/vehicles/${vehicleId}/driver`);
  }

  assignDriver(vehicleId: number, driverId: number): Observable<ApiResponse<VehicleDriverAssignment>> {
    return this.http.post<ApiResponse<VehicleDriverAssignment>>(`/api/v1/vehicles/${vehicleId}/driver/${driverId}`, {});
  }

  unassignDriver(vehicleId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/vehicles/${vehicleId}/driver`);
  }
}
