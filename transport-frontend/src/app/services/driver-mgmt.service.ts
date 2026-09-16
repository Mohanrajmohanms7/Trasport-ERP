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

export interface DriverPayroll {
  id?: number;
  payrollNumber?: string;
  driverId: number;
  payYear: number;
  payMonth: number;
  basicSalary: number;
  allowanceAmount?: number;
  deductionAmount?: number;
  advanceAdjustment?: number;
  netSalaryPayable?: number;
  paymentMethod?: string;
  status?: string; // DRAFT, APPROVED, PAID, CANCELLED
  accrualJvNumber?: string;
  paymentJvNumber?: string;
  cancellationJvNumber?: string;
}

export interface DriverPayrollPrintDTO {
  payrollId: number;
  payrollNumber: string;
  payYear: number;
  payMonth: number;
  payPeriod: string;
  status: string;
  paymentMethod: string;
  createdDate: string;

  driverId: number;
  driverName: string;
  driverCode: string;
  driverPhone?: string;
  licenseNumber?: string;

  basicSalary: number;
  allowanceAmount: number;
  grossEarnings: number;

  deductionAmount: number;
  advanceAdjustment: number;
  totalDeductions: number;

  netSalaryPayable: number;

  accrualJvNumber?: string;
  paymentJvNumber?: string;
  cancellationJvNumber?: string;

  companyId: number;
  companyName: string;
  companyAddress?: string;
  companyPhone?: string;
  companyEmail?: string;
  companyGSTIN?: string;

  branchId?: number;
  branchName?: string;
  branchAddress?: string;
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

  // Payroll APIs
  getPayrolls(status?: string): Observable<ApiResponse<DriverPayroll[]>> {
    const url = status ? `/api/v1/driver-payrolls?status=${status}` : `/api/v1/driver-payrolls`;
    return this.http.get<ApiResponse<DriverPayroll[]>>(url);
  }

  getPayrollsByDriver(driverId: number): Observable<ApiResponse<DriverPayroll[]>> {
    return this.http.get<ApiResponse<DriverPayroll[]>>(`/api/v1/driver-payrolls/driver/${driverId}`);
  }

  createPayroll(payroll: DriverPayroll): Observable<ApiResponse<DriverPayroll>> {
    return this.http.post<ApiResponse<DriverPayroll>>(`/api/v1/driver-payrolls`, payroll);
  }

  updatePayroll(id: number, payroll: DriverPayroll): Observable<ApiResponse<DriverPayroll>> {
    return this.http.put<ApiResponse<DriverPayroll>>(`/api/v1/driver-payrolls/${id}`, payroll);
  }

  deletePayroll(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/driver-payrolls/${id}`);
  }

  approvePayroll(id: number): Observable<ApiResponse<DriverPayroll>> {
    return this.http.post<ApiResponse<DriverPayroll>>(`/api/v1/driver-payrolls/${id}/approve`, {});
  }

  payPayroll(id: number, payment: { paymentMethod: string; remarks?: string }): Observable<ApiResponse<DriverPayroll>> {
    return this.http.post<ApiResponse<DriverPayroll>>(`/api/v1/driver-payrolls/${id}/pay`, payment);
  }

  cancelPayroll(id: number): Observable<ApiResponse<DriverPayroll>> {
    return this.http.post<ApiResponse<DriverPayroll>>(`/api/v1/driver-payrolls/${id}/cancel`, {});
  }

  // Salary Slip PDF & Print APIs
  getSalarySlipPrint(id: number): Observable<ApiResponse<DriverPayrollPrintDTO>> {
    return this.http.get<ApiResponse<DriverPayrollPrintDTO>>(`/api/v1/driver-payrolls/${id}/print`);
  }

  downloadSalarySlipPdf(id: number): Observable<Blob> {
    return this.http.get(`/api/v1/driver-payrolls/${id}/pdf`, { responseType: 'blob' });
  }

  exportPayrollsXlsx(): Observable<Blob> {
    return this.http.get('/api/v1/driver-payrolls/export/xlsx', { responseType: 'blob' });
  }
}
