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
  allocations?: { invoiceId: number; amount: number }[];
  status?: string;
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

  // Outstanding Invoices API
  getOutstandingInvoices(customerId: number): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`/api/v1/invoices/customer/${customerId}/outstanding`);
  }

  // Cancel Customer Receipt API
  cancelCustomerReceipt(receiptId: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`/api/v1/receipts/${receiptId}/cancel`, {});
  }

  // Approve Customer Receipt API
  approveCustomerReceipt(receiptId: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`/api/v1/receipts/${receiptId}/approve`, {});
  }

  // Phase 35 Read-Only Reporting API extensions
  getCustomerReceiptDetails(receiptId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`/api/v1/receipts/${receiptId}`);
  }

  getReceiptAllocations(receiptId: number): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`/api/v1/receipts/${receiptId}/allocations`);
  }

  getCustomerPaymentHistory(customerId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`/api/v1/receipts/customer/${customerId}/history`);
  }

  getReceiptAuditTrail(receiptId: number): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`/api/v1/receipts/${receiptId}/audit`);
  }

  // Phase 36 print & reprint audit API methods
  getCustomerReceiptPrintData(receiptId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`/api/v1/receipts/${receiptId}/print`);
  }

  getCustomerReceiptPrintHistory(receiptId: number): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`/api/v1/receipts/${receiptId}/print-history`);
  }

  recordCustomerReceiptPrintEvent(receiptId: number, eventType: string, format: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`/api/v1/receipts/${receiptId}/print-audit`, { eventType, format });
  }

  downloadCustomerReceiptPdf(receiptId: number): Observable<Blob> {
    return this.http.get(`/api/v1/receipts/${receiptId}/pdf`, { responseType: 'blob' });
  }

  // Phase 37 read-only dashboard & reporting API routes
  getReceiptDashboardSummary(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard/summary', { params });
  }

  getCustomerOutstandingSummary(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard/customers', { params });
  }

  getReceiptAgingSummary(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard/aging', { params });
  }

  getReceiptAgingInvoices(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard/aging/invoices', { params });
  }

  getReceiptReconciliation(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard/reconciliation', { params });
  }

  getCustomerPaymentPerformance(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard/payment-performance', { params });
  }

  getReceiptSettlementDashboard(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/receipts/dashboard');
  }
}



