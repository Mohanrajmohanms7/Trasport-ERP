import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChartOfAccount {
  id?: number;
  accountCode: string;
  accountName: string;
  accountType: string; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
  openingBalance: number;
  runningBalance?: number;
}

export interface JournalVoucher {
  id?: number;
  voucherNumber?: string;
  voucherDate?: string;
  referenceNumber?: string;
  description?: string;
  debitAccount: { id: number; accountName?: string; accountCode?: string };
  creditAccount: { id: number; accountName?: string; accountCode?: string };
  amount: number;
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
export class AccountsMgmtService {
  private http = inject(HttpClient);

  // Accounts APIs
  getAccounts(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/accounts', { params });
  }

  createAccount(account: ChartOfAccount): Observable<ApiResponse<ChartOfAccount>> {
    return this.http.post<ApiResponse<ChartOfAccount>>('/api/v1/accounts', account);
  }

  updateAccount(id: number, account: ChartOfAccount): Observable<ApiResponse<ChartOfAccount>> {
    return this.http.put<ApiResponse<ChartOfAccount>>(`/api/v1/accounts/${id}`, account);
  }

  deleteAccount(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/accounts/${id}`);
  }

  // Journal APIs
  getVouchers(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/journal', { params });
  }

  createVoucher(voucher: JournalVoucher): Observable<ApiResponse<JournalVoucher>> {
    return this.http.post<ApiResponse<JournalVoucher>>('/api/v1/journal', voucher);
  }
}
