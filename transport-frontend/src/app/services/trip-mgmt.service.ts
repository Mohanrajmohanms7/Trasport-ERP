import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TripDetail {
  id?: number;
  material: { id: number; name?: string };
  quantity: number;
  rate: number;
  loadingCharges: number;
  royalty: number;
  dispatchTime?: string;
  arrivalTime?: string;
}

export interface Trip {
  id?: number;
  tripNumber?: string;
  tripDate?: string;
  booking: { id: number; bookingNumber?: string };
  vehicle?: { id: number; registrationNumber?: string };
  driver?: { id: number; name?: string };
  status?: string; // PLANNED, DISPATCHED, COMPLETED, CANCELLED
  remarks?: string;
  details: TripDetail[];
  billingStatus?: string;
  associatedInvoiceNumber?: string;
  associatedInvoiceId?: number;
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
export class TripMgmtService {
  private http = inject(HttpClient);

  getTrips(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/trips', { params });
  }

  getTripsReadyForBilling(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/trips/ready-for-billing', { params });
  }


  getTripById(id: number): Observable<ApiResponse<Trip>> {
    return this.http.get<ApiResponse<Trip>>(`/api/v1/trips/${id}`);
  }

  createTrip(trip: Trip): Observable<ApiResponse<Trip>> {
    return this.http.post<ApiResponse<Trip>>('/api/v1/trips', trip);
  }

  updateTrip(id: number, trip: Trip): Observable<ApiResponse<Trip>> {
    return this.http.put<ApiResponse<Trip>>(`/api/v1/trips/${id}`, trip);
  }

  deleteTrip(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/trips/${id}`);
  }

  dispatchTrip(id: number): Observable<ApiResponse<Trip>> {
    return this.http.post<ApiResponse<Trip>>(`/api/v1/trips/${id}/dispatch`, {});
  }

  completeTrip(id: number): Observable<ApiResponse<Trip>> {
    return this.http.post<ApiResponse<Trip>>(`/api/v1/trips/${id}/complete`, {});
  }
}
