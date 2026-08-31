import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SalesInvoiceDetail {
  id?: number;
  trip?: { id: number; tripNumber?: string };
  material: { id: number; name?: string };
  quantity: number;
  rate: number;
  freightCharges: number;
  loadingCharges: number;
  royalty: number;
  gstPercentage: number;
  cgst?: number;
  sgst?: number;
  igst?: number;
  netAmount?: number;
}

export interface SalesInvoice {
  id?: number;
  invoiceNumber?: string;
  invoiceDate?: string;
  customer: { id: number; name?: string };
  status?: string; // DRAFT, PENDING, APPROVED, GENERATED, CANCELLED
  paymentTerms?: string;
  subtotal?: number;
  discount: number;
  netAmount?: number;
  details: SalesInvoiceDetail[];
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
export class InvoiceMgmtService {
  private http = inject(HttpClient);

  getInvoices(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/invoices', { params });
  }

  getInvoiceById(id: number): Observable<ApiResponse<SalesInvoice>> {
    return this.http.get<ApiResponse<SalesInvoice>>(`/api/v1/invoices/${id}`);
  }

  createInvoice(invoice: SalesInvoice): Observable<ApiResponse<SalesInvoice>> {
    return this.http.post<ApiResponse<SalesInvoice>>('/api/v1/invoices', invoice);
  }

  createInvoiceFromTrip(tripId: number): Observable<ApiResponse<SalesInvoice>> {
    return this.http.post<ApiResponse<SalesInvoice>>(`/api/v1/invoices/from-trip/${tripId}`, {});
  }


  updateInvoice(id: number, invoice: SalesInvoice): Observable<ApiResponse<SalesInvoice>> {
    return this.http.put<ApiResponse<SalesInvoice>>(`/api/v1/invoices/${id}`, invoice);
  }

  deleteInvoice(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/invoices/${id}`);
  }

  approveInvoice(id: number): Observable<ApiResponse<SalesInvoice>> {
    return this.http.post<ApiResponse<SalesInvoice>>(`/api/v1/invoices/${id}/approve`, {});
  }

  cancelInvoice(id: number): Observable<ApiResponse<SalesInvoice>> {
    return this.http.post<ApiResponse<SalesInvoice>>(`/api/v1/invoices/${id}/cancel`, {});
  }
}
