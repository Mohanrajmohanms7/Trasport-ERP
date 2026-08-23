import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { resolveTenantCompanyId } from '../shared/tenant-context';

export interface User {
  id?: number;
  code: string;
  name: string;
  username: string;
  password?: string;
  email?: string;
  phone?: string;
  status: string;
  companyId?: number;
  branchId?: number;
  roles?: Role[];
  description?: string;
}

export interface Role {
  id?: number;
  code: string;
  name: string;
  description?: string;
  status: string;
  companyId?: number;
  branchId?: number;
  permissions?: Permission[];
}

export interface Permission {
  id?: number;
  code: string;
  name: string;
  description?: string;
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
export class UserRoleService {
  private http = inject(HttpClient);

  private companyParams(extra?: Record<string, unknown>): HttpParams {
    let params = new HttpParams().set('companyId', String(resolveTenantCompanyId()));
    if (extra) {
      Object.entries(extra).forEach(([key, value]) => {
        if (value !== undefined && value !== null && key !== 'companyId') {
          params = params.set(key, String(value));
        }
      });
    }
    return params;
  }

  getUsers(params?: any): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>('/api/v1/users', {
      params: this.companyParams(params)
    });
  }

  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`/api/v1/users/${id}`);
  }

  createUser(user: User): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>('/api/v1/users', {
      ...user,
      companyId: resolveTenantCompanyId()
    });
  }

  updateUser(id: number, user: User): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`/api/v1/users/${id}`, {
      ...user,
      companyId: resolveTenantCompanyId()
    });
  }

  deleteUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/users/${id}`);
  }

  getRoles(): Observable<ApiResponse<Role[]>> {
    return this.http.get<ApiResponse<Role[]>>('/api/v1/roles', {
      params: this.companyParams()
    });
  }

  getRoleById(id: number): Observable<ApiResponse<Role>> {
    return this.http.get<ApiResponse<Role>>(`/api/v1/roles/${id}`);
  }

  createRole(role: Role): Observable<ApiResponse<Role>> {
    return this.http.post<ApiResponse<Role>>('/api/v1/roles', {
      ...role,
      companyId: resolveTenantCompanyId()
    });
  }

  updateRole(id: number, role: Role): Observable<ApiResponse<Role>> {
    return this.http.put<ApiResponse<Role>>(`/api/v1/roles/${id}`, {
      ...role,
      companyId: resolveTenantCompanyId()
    });
  }

  deleteRole(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/roles/${id}`);
  }

  getPermissions(): Observable<ApiResponse<Permission[]>> {
    return this.http.get<ApiResponse<Permission[]>>('/api/v1/permissions');
  }
}
