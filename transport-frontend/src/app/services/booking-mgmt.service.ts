import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BookingDetail {
  id?: number;
  material: { id: number; name?: string; code?: string };
  quantity: number;
  rate: number;
  transportRate: number;
  royaltyRate: number;
  loadingCharge: number;
  gstPercentage: number;
  netAmount?: number;
}

export interface Booking {
  id?: number;
  bookingNumber?: string;
  bookingDate?: string;
  customer: { id: number; name?: string };
  deliverySite?: { id: number; siteName?: string };
  status?: string; // DRAFT, PENDING, APPROVED, REJECTED, ON_HOLD
  priority: string; // HIGH, MEDIUM, LOW
  remarks?: string;
  details: BookingDetail[];
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
export class BookingMgmtService {
  private http = inject(HttpClient);

  getBookings(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/bookings', { params });
  }

  getBookingById(id: number): Observable<ApiResponse<Booking>> {
    return this.http.get<ApiResponse<Booking>>(`/api/v1/bookings/${id}`);
  }

  createBooking(booking: Booking): Observable<ApiResponse<Booking>> {
    return this.http.post<ApiResponse<Booking>>('/api/v1/bookings', booking);
  }

  updateBooking(id: number, booking: Booking): Observable<ApiResponse<Booking>> {
    return this.http.put<ApiResponse<Booking>>(`/api/v1/bookings/${id}`, booking);
  }

  deleteBooking(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/bookings/${id}`);
  }

  approveBooking(id: number): Observable<ApiResponse<Booking>> {
    return this.http.post<ApiResponse<Booking>>(`/api/v1/bookings/${id}/approve`, {});
  }

  rejectBooking(id: number): Observable<ApiResponse<Booking>> {
    return this.http.post<ApiResponse<Booking>>(`/api/v1/bookings/${id}/reject`, {});
  }
}
