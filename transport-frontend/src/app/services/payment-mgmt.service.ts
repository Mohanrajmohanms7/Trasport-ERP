import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CustomerReceipt {
  id?: number;
  receiptNumber?: string;
  receiptDate?: string;
  customer: { id: number; name?: string };
  booking?: { id: number; bookingNumber?: string };
  amountReceived: number;
  advanceAmount: number;
  paymentMethod: string; // CASH, UPI, GPAY, PHONEPE, NEFT, RTGS, IMPS, BANK_TRANSFER, CHEQUE
  referenceNumber?: string;
  remarks?: string;
}

export interface CustomerLedger {
  id?: number;
  debitAmount: number;
  creditAmount: number;
  runningBalance: number;
  remarks?: string;
  createdDate?: string;
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
export class PaymentMgmtService {
  private http = inject(HttpClient);

  // Receipts APIs
  getReceipts(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts', { params });
  }

  createReceipt(receipt: CustomerReceipt): Observable<ApiResponse<CustomerReceipt>> {
    return this.http.post<ApiResponse<CustomerReceipt>>('/api/v1/receipts', receipt);
  }

  updateReceipt(id: number, receipt: CustomerReceipt): Observable<ApiResponse<CustomerReceipt>> {
    return this.http.put<ApiResponse<CustomerReceipt>>(`/api/v1/receipts/${id}`, receipt);
  }

  deleteReceipt(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/receipts/${id}`);
  }

  // Ledger APIs
  getCustomerLedger(customerId: number): Observable<ApiResponse<CustomerLedger[]>> {
    return this.http.get<ApiResponse<CustomerLedger[]>>(`/api/v1/customer-ledger/${customerId}`);
  }
}
