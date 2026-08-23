import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface SetupStatus {
  setupCompleted: boolean;
  hasBusinessData: boolean;
  companyCount: number;
  branchCount: number;
  vehicleCount: number;
  driverCount: number;
  customerCount: number;
  materialCount: number;
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
export class SetupService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/setup';

  status = signal<SetupStatus | null>(null);

  getStatus(): Observable<ApiResponse<SetupStatus>> {
    return this.http.get<ApiResponse<SetupStatus>>(`${this.apiUrl}/status`).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.status.set(res.data);
        }
      })
    );
  }

  completeSetup(): Observable<ApiResponse<SetupStatus>> {
    return this.http.post<ApiResponse<SetupStatus>>(`${this.apiUrl}/complete`, {}).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.status.set(res.data);
        }
      })
    );
  }

  seedDemoData(): Observable<ApiResponse<SetupStatus>> {
    return this.http.post<ApiResponse<SetupStatus>>(`${this.apiUrl}/seed-demo`, {}).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.status.set(res.data);
        }
      })
    );
  }

  /** Lookups + sample materials/quarry only — you still create 1 customer / vehicle / driver. */
  seedSupportingExampleData(): Observable<ApiResponse<Record<string, unknown>>> {
    return this.http.post<ApiResponse<Record<string, unknown>>>(`${this.apiUrl}/seed-supporting`, {});
  }

  /** Wizard is needed until the user finishes it or real business data exists. */
  needsSetup(status: SetupStatus): boolean {
    return !status.setupCompleted && !status.hasBusinessData;
  }
}

