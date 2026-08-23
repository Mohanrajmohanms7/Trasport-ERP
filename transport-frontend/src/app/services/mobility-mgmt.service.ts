import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GpsTracking {
  id?: number;
  vehicle: { id: number; registrationNumber?: string };
  latitude: number;
  longitude: number;
  speed: number;
  pingTime?: string;
}

export interface AiPrediction {
  id?: number;
  targetType: string; // MAINTENANCE, FUEL, TRIP_DELAY
  predictionText: string;
  probability: number;
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
export class MobilityMgmtService {
  private http = inject(HttpClient);

  // GPS APIs
  getLiveRoute(vehicleId: number): Observable<ApiResponse<GpsTracking[]>> {
    return this.http.get<ApiResponse<GpsTracking[]>>('/api/v1/gps/live', { params: { vehicleId } });
  }

  recordPing(ping: GpsTracking): Observable<ApiResponse<GpsTracking>> {
    return this.http.post<ApiResponse<GpsTracking>>('/api/v1/gps/location', ping);
  }

  // AI APIs
  getAiDashboard(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/ai/dashboard', { params });
  }

  // Backup history is loaded via Platform Admin APIs from mobility console.
}
