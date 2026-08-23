import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Expense {
  id?: number;
  expenseNumber?: string;
  expenseDate?: string;
  category: string; // TOLL, DRIVER_BATA, PARKING, VEHICLE_REPAIR, INSURANCE, OFFICE
  vehicle?: { id: number; registrationNumber?: string };
  driver?: { id: number; name?: string };
  trip?: { id: number; tripNumber?: string };
  description?: string;
  amount: number;
  gstAmount: number;
  totalAmount?: number;
  paymentMethod: string; // CASH, UPI, BANK_TRANSFER, CHEQUE, CREDIT
  status?: string; // DRAFT, SUBMITTED, APPROVED, REJECTED, PAID, CANCELLED
  remarks?: string;
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
export class ExpenseMgmtService {
  private http = inject(HttpClient);

  getExpenses(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/expenses', { params });
  }

  getExpenseById(id: number): Observable<ApiResponse<Expense>> {
    return this.http.get<ApiResponse<Expense>>(`/api/v1/expenses/${id}`);
  }

  createExpense(expense: Expense): Observable<ApiResponse<Expense>> {
    return this.http.post<ApiResponse<Expense>>('/api/v1/expenses', expense);
  }

  updateExpense(id: number, expense: Expense): Observable<ApiResponse<Expense>> {
    return this.http.put<ApiResponse<Expense>>(`/api/v1/expenses/${id}`, expense);
  }

  deleteExpense(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/expenses/${id}`);
  }

  approveExpense(id: number): Observable<ApiResponse<Expense>> {
    return this.http.post<ApiResponse<Expense>>(`/api/v1/expenses/${id}/approve`, {});
  }

  rejectExpense(id: number): Observable<ApiResponse<Expense>> {
    return this.http.post<ApiResponse<Expense>>(`/api/v1/expenses/${id}/reject`, {});
  }
}
