import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CustomerContact {
  id?: number;
  contactName: string;
  designation?: string;
  email?: string;
  phone?: string;
}

export interface CustomerDeliverySite {
  id?: number;
  siteCode: string;
  siteName: string;
  address: string;
  managerName?: string;
}

export interface CustomerDocument {
  id?: number;
  docType: string; // GST_CERT, PAN_CARD, KYC, AGREEMENT
  docNumber: string;
  filePath?: string;
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
export class CustomerMgmtService {
  private http = inject(HttpClient);

  // Contacts APIs
  getContacts(customerId: number): Observable<ApiResponse<CustomerContact[]>> {
    return this.http.get<ApiResponse<CustomerContact[]>>(`/api/v1/customers/${customerId}/contacts`);
  }

  addContact(customerId: number, contact: CustomerContact): Observable<ApiResponse<CustomerContact>> {
    return this.http.post<ApiResponse<CustomerContact>>(`/api/v1/customers/${customerId}/contacts`, contact);
  }

  deleteContact(customerId: number, id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/customers/${customerId}/contacts/${id}`);
  }

  // Delivery Sites APIs
  getSites(customerId: number): Observable<ApiResponse<CustomerDeliverySite[]>> {
    return this.http.get<ApiResponse<CustomerDeliverySite[]>>(`/api/v1/customers/${customerId}/delivery-sites`);
  }

  addSite(customerId: number, site: CustomerDeliverySite): Observable<ApiResponse<CustomerDeliverySite>> {
    return this.http.post<ApiResponse<CustomerDeliverySite>>(`/api/v1/customers/${customerId}/delivery-sites`, site);
  }

  deleteSite(customerId: number, id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/customers/${customerId}/delivery-sites/${id}`);
  }

  // Documents APIs
  getDocuments(customerId: number): Observable<ApiResponse<CustomerDocument[]>> {
    return this.http.get<ApiResponse<CustomerDocument[]>>(`/api/v1/customers/${customerId}/documents`);
  }

  addDocument(customerId: number, doc: CustomerDocument): Observable<ApiResponse<CustomerDocument>> {
    return this.http.post<ApiResponse<CustomerDocument>>(`/api/v1/customers/${customerId}/documents`, doc);
  }

  deleteDocument(customerId: number, id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/customers/${customerId}/documents/${id}`);
  }
}
