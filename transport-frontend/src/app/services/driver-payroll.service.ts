import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
}

export interface PaySlab {
  id?: number;
  tripsFrom: number;
  tripsTo: number | null;
  dailyAmount: number;
}

export interface PayrollDay {
  id?: number;
  workDate: string;
  tripCount: number;
  slabTripsFrom: number | null;
  slabTripsTo: number | null;
  dailyAmount: number;
}

export interface PayrollDeduction {
  id?: number;
  deductionType: 'FINE' | 'DAMAGE' | 'OTHER';
  amount: number;
  deductionDate?: string;
  remarks?: string;
}

/** All amounts and counts are calculated by the backend; the UI only displays them. */
export interface SlabPayroll {
  id: number;
  payrollNumber: string;
  driver: { id: number; name: string; code?: string };
  payYear: number;
  payMonth: number;
  status: 'DRAFT' | 'APPROVED' | 'POSTED' | 'PAID' | 'CANCELLED';
  totalTrips: number;
  tripDays: number;
  tripEarnings: number;
  basicSalary: number;
  allowanceAmount: number;
  grossAmount: number;
  deductionAmount: number;
  advanceAdjustment: number;
  netSalaryPayable: number;
  description?: string;
  approvedBy?: string;
  approvedAt?: string;
  postingDate?: string;
  postedBy?: string;
  accrualJvNumber?: string;
  recoveryJvNumber?: string;
  deductionJvNumber?: string;
  paymentJvNumber?: string;
  cancellationJvNumber?: string;
  paymentMethod?: string;
  paidDate?: string;
  paidBy?: string;
  paymentReference?: string;
  days: PayrollDay[];
  deductions: PayrollDeduction[];
}

export interface GeneratePayrollRequest {
  driverId: number;
  payYear: number;
  payMonth: number;
  allowanceAmount: number;
  advanceAdjustment: number;
  description?: string;
  deductions: PayrollDeduction[];
}

export interface DriverAdvance {
  id?: number;
  advanceNumber?: string;
  driver: { id: number; name?: string };
  advanceDate?: string;
  amount: number;
  recoveredAmount?: number;
  outstandingAmount?: number;
  paymentMethod: string;
  paymentReference?: string;
  status?: 'ISSUED' | 'CANCELLED';
  jvNumber?: string;
  remarks?: string;
}

@Injectable({ providedIn: 'root' })
export class DriverPayrollService {
  private http = inject(HttpClient);
  private base = '/api/v1/driver-payrolls';

  search(filters: { driverId?: number | null; payYear?: number | null; payMonth?: number | null; status?: string | null }): Observable<ApiResponse<any>> {
    let params = new HttpParams().set('size', '200').set('sort', 'id,desc');
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') params = params.set(k, String(v));
    });
    return this.http.get<ApiResponse<any>>(this.base, { params });
  }

  get(id: number): Observable<ApiResponse<SlabPayroll>> {
    return this.http.get<ApiResponse<SlabPayroll>>(`${this.base}/${id}`);
  }

  generate(req: GeneratePayrollRequest): Observable<ApiResponse<SlabPayroll>> {
    return this.http.post<ApiResponse<SlabPayroll>>(`${this.base}/generate`, req);
  }

  update(id: number, req: GeneratePayrollRequest): Observable<ApiResponse<SlabPayroll>> {
    return this.http.put<ApiResponse<SlabPayroll>>(`${this.base}/${id}`, req);
  }

  recalculate(id: number): Observable<ApiResponse<SlabPayroll>> {
    return this.http.post<ApiResponse<SlabPayroll>>(`${this.base}/${id}/recalculate`, {});
  }

  remove(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }

  approve(id: number): Observable<ApiResponse<SlabPayroll>> {
    return this.http.post<ApiResponse<SlabPayroll>>(`${this.base}/${id}/approve`, {});
  }

  post(id: number): Observable<ApiResponse<SlabPayroll>> {
    return this.http.post<ApiResponse<SlabPayroll>>(`${this.base}/${id}/post`, {});
  }

  pay(id: number, payment: { paymentMethod: string; paymentDate?: string; paymentReference?: string }): Observable<ApiResponse<SlabPayroll>> {
    return this.http.post<ApiResponse<SlabPayroll>>(`${this.base}/${id}/pay`, payment);
  }

  cancel(id: number): Observable<ApiResponse<SlabPayroll>> {
    return this.http.post<ApiResponse<SlabPayroll>>(`${this.base}/${id}/cancel`, {});
  }

  slipPdf(id: number, mine = false): Observable<Blob> {
    return this.http.get(mine ? `${this.base}/my/${id}/pdf` : `${this.base}/${id}/pdf`, { responseType: 'blob' });
  }

  myPayrolls(): Observable<ApiResponse<SlabPayroll[]>> {
    return this.http.get<ApiResponse<SlabPayroll[]>>(`${this.base}/my`);
  }

  getSlabs(): Observable<ApiResponse<PaySlab[]>> {
    return this.http.get<ApiResponse<PaySlab[]>>('/api/v1/driver-pay-slabs');
  }

  saveSlabs(slabs: PaySlab[]): Observable<ApiResponse<PaySlab[]>> {
    return this.http.put<ApiResponse<PaySlab[]>>('/api/v1/driver-pay-slabs', slabs);
  }

  getAdvances(driverId?: number | null, status?: string | null): Observable<ApiResponse<any>> {
    let params = new HttpParams().set('size', '200');
    if (driverId) params = params.set('driverId', String(driverId));
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<any>>('/api/v1/driver-advances', { params });
  }

  outstanding(driverId: number): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`/api/v1/driver-advances/driver/${driverId}/outstanding`);
  }

  issueAdvance(advance: DriverAdvance): Observable<ApiResponse<DriverAdvance>> {
    return this.http.post<ApiResponse<DriverAdvance>>('/api/v1/driver-advances', advance);
  }

  cancelAdvance(id: number): Observable<ApiResponse<DriverAdvance>> {
    return this.http.post<ApiResponse<DriverAdvance>>(`/api/v1/driver-advances/${id}/cancel`, {});
  }
}
