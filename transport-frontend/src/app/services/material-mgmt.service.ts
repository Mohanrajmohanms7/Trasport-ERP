import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { resolveTenantCompanyId } from '../shared/tenant-context';

export interface LoadingLocation {
  id?: number;
  locationCode: string;
  loadingPoint: string;
  loadingCharges: number;
  latitude?: number;
  longitude?: number;
  companyId?: number;
}

export interface MaterialPrice {
  id?: number;
  materialRate: number;
  transportRate: number;
  royaltyRate: number;
  loadingCharge: number;
  effectiveDate: string;
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
export class MaterialMgmtService {
  private http = inject(HttpClient);

  private companyParams(): HttpParams {
    return new HttpParams().set('companyId', String(resolveTenantCompanyId()));
  }

  // Loading Locations APIs — always scoped to logged-in company
  getLocations(): Observable<ApiResponse<LoadingLocation[]>> {
    return this.http.get<ApiResponse<LoadingLocation[]>>('/api/v1/loading-locations', {
      params: this.companyParams()
    });
  }

  createLocation(loc: LoadingLocation): Observable<ApiResponse<LoadingLocation>> {
    return this.http.post<ApiResponse<LoadingLocation>>('/api/v1/loading-locations', {
      ...loc,
      companyId: resolveTenantCompanyId()
    });
  }

  updateLocation(id: number, loc: LoadingLocation): Observable<ApiResponse<LoadingLocation>> {
    return this.http.put<ApiResponse<LoadingLocation>>(`/api/v1/loading-locations/${id}`, loc);
  }

  deleteLocation(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/loading-locations/${id}`);
  }

  // Pricing APIs
  getPrices(materialId: number): Observable<ApiResponse<MaterialPrice[]>> {
    return this.http.get<ApiResponse<MaterialPrice[]>>(`/api/v1/material-prices?materialId=${materialId}`);
  }

  createPrice(materialId: number, price: MaterialPrice): Observable<ApiResponse<MaterialPrice>> {
    return this.http.post<ApiResponse<MaterialPrice>>(`/api/v1/material-prices?materialId=${materialId}`, price);
  }

  deletePrice(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/material-prices/${id}`);
  }
}
