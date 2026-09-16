import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { resolveTenantCompanyId } from '../shared/tenant-context';
import { ApiResponse } from './material-mgmt.service';

export interface UomMaster {
  id?: number;
  code: string;
  name: string;
  symbol?: string;
  category: string;
  isBaseUnit?: boolean;
  description?: string;
  status?: string;
  companyId?: number;
}

export interface UomConversion {
  id?: number;
  code?: string;
  name?: string;
  materialId?: number;
  material?: any;
  fromUom: any;
  toUom: any;
  conversionFactor: number;
  description?: string;
  status?: string;
  companyId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class UomMgmtService {
  private http = inject(HttpClient);

  private companyParams(): HttpParams {
    return new HttpParams().set('companyId', String(resolveTenantCompanyId()));
  }

  // --- UOM Master ---

  getUoms(): Observable<ApiResponse<UomMaster[]>> {
    return this.http.get<ApiResponse<UomMaster[]>>('/api/v1/uoms', {
      params: this.companyParams()
    });
  }

  createUom(uom: UomMaster): Observable<ApiResponse<UomMaster>> {
    return this.http.post<ApiResponse<UomMaster>>('/api/v1/uoms', {
      ...uom,
      companyId: resolveTenantCompanyId()
    });
  }

  updateUom(id: number, uom: UomMaster): Observable<ApiResponse<UomMaster>> {
    return this.http.put<ApiResponse<UomMaster>>(`/api/v1/uoms/${id}`, uom);
  }

  deleteUom(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/uoms/${id}`);
  }

  // --- UOM Conversion ---

  getConversions(): Observable<ApiResponse<UomConversion[]>> {
    return this.http.get<ApiResponse<UomConversion[]>>('/api/v1/uoms/conversions', {
      params: this.companyParams()
    });
  }

  createConversion(conversion: UomConversion): Observable<ApiResponse<UomConversion>> {
    return this.http.post<ApiResponse<UomConversion>>('/api/v1/uoms/conversions', {
      ...conversion,
      companyId: resolveTenantCompanyId()
    });
  }

  updateConversion(id: number, conversion: UomConversion): Observable<ApiResponse<UomConversion>> {
    return this.http.put<ApiResponse<UomConversion>>(`/api/v1/uoms/conversions/${id}`, conversion);
  }

  deleteConversion(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/v1/uoms/conversions/${id}`);
  }

  // --- Quantity Conversion Calculation ---

  convertQuantity(fromUomId: number, toUomId: number, quantity: number, materialId?: number): Observable<ApiResponse<any>> {
    let params = new HttpParams()
      .set('fromUomId', String(fromUomId))
      .set('toUomId', String(toUomId))
      .set('quantity', String(quantity));
    if (materialId) {
      params = params.set('materialId', String(materialId));
    }
    return this.http.get<ApiResponse<any>>('/api/v1/uoms/convert', { params });
  }
}
