import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DriverDocument {
  id?: number;
  docType: string;
  docNumber: string;
  filePath?: string;
  description?: string;
}

export interface DriverAttendance {
  id?: number;
  attendanceDate: string;
  status: string; // PRESENT, ABSENT, LEAVE, HALF_DAY
  description?: string;
}

export interface DriverSalary {
  id?: number;
  basicSalary: number;
  overtimeRate: number;
  advanceTaken: number;
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
export class DriverMgmtService {
  private http = inject(HttpClient);

  // Documents APIs
  getDocuments(driverId: number): Observable<ApiResponse<DriverDocument[]>> {
    return this.http.get<ApiResponse<DriverDocument[]>>(`/api/v1/drivers/${driverId}/documents`);
  }

  addDocument(driverId: number, doc: DriverDocument): Observable<ApiResponse<DriverDocument>> {
    return this.http.post<ApiResponse<DriverDocument>>(`/api/v1/drivers/${driverId}/documents`, doc);
  }

  deleteDocument(driverId: number, id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/drivers/${driverId}/documents/${id}`);
  }

  // Attendance APIs
  getAttendance(driverId: number): Observable<ApiResponse<DriverAttendance[]>> {
    return this.http.get<ApiResponse<DriverAttendance[]>>(`/api/v1/drivers/${driverId}/attendance`);
  }

  logAttendance(driverId: number, log: DriverAttendance): Observable<ApiResponse<DriverAttendance>> {
    return this.http.post<ApiResponse<DriverAttendance>>(`/api/v1/drivers/${driverId}/attendance`, log);
  }

  // Salary APIs
  getSalary(driverId: number): Observable<ApiResponse<DriverSalary>> {
    return this.http.get<ApiResponse<DriverSalary>>(`/api/v1/drivers/${driverId}/salary`);
  }

  saveSalary(driverId: number, salary: DriverSalary): Observable<ApiResponse<DriverSalary>> {
    return this.http.post<ApiResponse<DriverSalary>>(`/api/v1/drivers/${driverId}/salary`, salary);
  }
}
