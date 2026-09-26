import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { tryResolveTenantCompanyId } from '../shared/tenant-context';

export interface LoginResponse {
  token: string;
  refreshToken: string;
  username: string;
  name: string;
  email?: string;
  roles: string[];
  companyId?: number;
  branchId?: number;
  subscriptionExpired?: boolean;
  description?: string;
  companyName?: string;
  companyShortName?: string;
}

/** Company identity shown in the header ("PKC" above "TransaFlow"). */
export interface TenantBrand {
  companyName?: string;
  shortName?: string;
}

export interface UserProfile {
  id?: number;
  code: string;
  name: string;
  username: string;
  email?: string;
  phone?: string;
  description?: string;
  status: string;
  companyId?: number;
  branchId?: number;
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
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/auth';

  // Auth State signals
  currentUser = signal<LoginResponse | null>(null);
  isAuthenticated = signal<boolean>(false);
  tenantBrand = signal<TenantBrand | null>(this.readBrand());

  private readBrand(): TenantBrand | null {
    try {
      const raw = localStorage.getItem('tenantBrand');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  private setBrand(brand: TenantBrand | null): void {
    this.tenantBrand.set(brand);
    if (brand && (brand.shortName || brand.companyName)) localStorage.setItem('tenantBrand', JSON.stringify(brand));
    else localStorage.removeItem('tenantBrand');
  }

  /** Re-reads the company initials (picks up renames made in Platform Admin). */
  refreshTenantBrand(): void {
    if (!localStorage.getItem('token')) return;
    this.http.get<ApiResponse<any>>(`${this.apiUrl}/tenant-brand`).subscribe({
      next: r => this.setBrand(r?.data ? { companyName: r.data.companyName, shortName: r.data.companyShortName } : null),
      error: () => {}
    });
  }

  constructor() {
    this.loadTokenState();
  }

  private loadTokenState() {
    const token = localStorage.getItem('token');
    const refreshToken = localStorage.getItem('refreshToken');
    const username = localStorage.getItem('username');
    const name = localStorage.getItem('name');
    const email = localStorage.getItem('email') || undefined;
    const roles = JSON.parse(localStorage.getItem('roles') || '[]');
    const subscriptionExpired = localStorage.getItem('subscriptionExpired') === 'true';
    const companyIdRaw = localStorage.getItem('companyId');
    const branchIdRaw = localStorage.getItem('branchId');
    const companyId = companyIdRaw ? Number(companyIdRaw) : undefined;
    const branchId = branchIdRaw ? Number(branchIdRaw) : undefined;
    const description = localStorage.getItem('description') || undefined;

    if (token && username) {
      this.currentUser.set({
        token,
        refreshToken: refreshToken || '',
        username,
        name: name || username,
        email,
        roles,
        companyId: companyId && !Number.isNaN(companyId) ? companyId : undefined,
        branchId: branchId && !Number.isNaN(branchId) ? branchId : undefined,
        subscriptionExpired,
        description
      });
      this.isAuthenticated.set(true);
    }
  }

  private persistSession(authData: LoginResponse): void {
    this.setBrand({ companyName: authData.companyName, shortName: authData.companyShortName });
    localStorage.setItem('token', authData.token);
    localStorage.setItem('refreshToken', authData.refreshToken);
    localStorage.setItem('username', authData.username);
    localStorage.setItem('name', authData.name);
    localStorage.setItem('roles', JSON.stringify(authData.roles || []));
    localStorage.setItem('subscriptionExpired', authData.subscriptionExpired ? 'true' : 'false');
    if (authData.email) {
      localStorage.setItem('email', authData.email);
    } else {
      localStorage.removeItem('email');
    }
    if (authData.companyId != null) {
      localStorage.setItem('companyId', String(authData.companyId));
    } else {
      localStorage.removeItem('companyId');
    }
    if (authData.branchId != null) {
      localStorage.setItem('branchId', String(authData.branchId));
    } else {
      localStorage.removeItem('branchId');
    }
    if (authData.description) {
      localStorage.setItem('description', authData.description);
    } else {
      localStorage.removeItem('description');
    }

    this.currentUser.set(authData);
    this.isAuthenticated.set(true);
  }

  setSubscriptionExpired(expired: boolean): void {
    localStorage.setItem('subscriptionExpired', expired ? 'true' : 'false');
    const user = this.currentUser();
    if (user) {
      this.currentUser.set({
        ...user,
        subscriptionExpired: expired
      });
    }
  }

  login(credentials: { username: string; password: string }): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.persistSession(res.data);
        }
      })
    );
  }

  getActivePlans(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/plans`);
  }

  renewSubscription(planId: number, paymentMethod: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/renew-subscription?planId=${planId}&paymentMethod=${paymentMethod}`, {});
  }

  logout(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/logout`, {}).pipe(
      tap(() => {
        this.clearLocalSession();
      })
    );
  }

  refreshToken(token: string): Observable<ApiResponse<{ token: string; refreshToken: string }>> {
    return this.http.post<ApiResponse<{ token: string; refreshToken: string }>>(`${this.apiUrl}/refresh`, { refreshToken: token }).pipe(
      tap(res => {
        if (res.success && res.data) {
          localStorage.setItem('token', res.data.token);
          localStorage.setItem('refreshToken', res.data.refreshToken);
          const syncedCompanyId = tryResolveTenantCompanyId() ?? undefined;
          const current = this.currentUser();
          if (current) {
            this.currentUser.set({
              ...current,
              token: res.data.token,
              refreshToken: res.data.refreshToken,
              companyId: syncedCompanyId ?? current.companyId
            });
          }
        }
      })
    );
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(payload: any): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/reset-password`, payload);
  }

  changePassword(payload: any): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/change-password`, payload);
  }

  getProfile(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.apiUrl}/profile`).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.applyProfileToSession(res.data);
        }
      })
    );
  }

  updateProfile(profile: UserProfile): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(`${this.apiUrl}/profile`, profile).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.applyProfileToSession(res.data);
        }
      })
    );
  }

  private applyProfileToSession(profile: UserProfile): void {
    localStorage.setItem('name', profile.name);
    if (profile.email) {
      localStorage.setItem('email', profile.email);
    }
    if (profile.username) {
      localStorage.setItem('username', profile.username);
    }
    if (profile.companyId != null) {
      localStorage.setItem('companyId', String(profile.companyId));
    }
    if (profile.branchId != null) {
      localStorage.setItem('branchId', String(profile.branchId));
    }
    if (profile.description) {
      localStorage.setItem('description', profile.description);
    } else {
      localStorage.removeItem('description');
    }

    const current = this.currentUser();
    if (current) {
      this.currentUser.set({
        ...current,
        name: profile.name,
        username: profile.username || current.username,
        email: profile.email || current.email,
        companyId: profile.companyId ?? current.companyId,
        branchId: profile.branchId ?? current.branchId,
        description: profile.description || ''
      });
    }
  }

  clearLocalSession() {
    this.setBrand(null);
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    localStorage.removeItem('name');
    localStorage.removeItem('email');
    localStorage.removeItem('roles');
    localStorage.removeItem('companyId');
    localStorage.removeItem('branchId');
    localStorage.removeItem('description');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }
}
