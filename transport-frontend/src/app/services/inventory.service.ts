import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from './work-order.service';

export interface Warehouse {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status?: string;
  companyId?: number;
  branchId?: number;
  branchCode?: string;
  branchName?: string;
  version?: number;
}

export interface WarehouseStock {
  id: number;
  companyId?: number;
  branchId?: number;
  branchCode?: string;
  branchName?: string;
  warehouseId?: number;
  warehouseCode?: string;
  warehouseName?: string;
  sparePartId?: number;
  sparePartCode?: string;
  sparePartName?: string;
  uomCode?: string;
  uomName?: string;
  availableQuantity?: number;
  version?: number;
}

export interface InventoryTransaction {
  id: number;
  code?: string;
  createdDate?: string;
  transactionType?: string;
  branchId?: number;
  branchCode?: string;
  branchName?: string;
  warehouseId?: number;
  warehouseCode?: string;
  warehouseName?: string;
  sparePartId?: number;
  sparePartCode?: string;
  sparePartName?: string;
  quantity?: number;
  referenceType?: string;
  referenceId?: number;
  workOrderId?: number;
  workOrderNumber?: string;
  workOrderPartId?: number;
  createdBy?: string;
  description?: string;
}

export interface StockMovement {
  transactionId?: number;
  transactionCode?: string;
  transactionType?: string;
  workOrderId?: number;
  workOrderNumber?: string;
  workOrderPartId?: number;
  sparePartId?: number;
  sparePartCode?: string;
  sparePartName?: string;
  warehouseId?: number;
  warehouseCode?: string;
  warehouseName?: string;
  quantity?: number;
  issuedQuantity?: number;
  returnedQuantity?: number;
  netIssuedQuantity?: number;
  remainingToIssue?: number;
  returnableQuantity?: number;
  availableQuantity?: number;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private http = inject(HttpClient);
  private warehousesUrl = '/api/v1/warehouses';
  private inventoryUrl = '/api/v1/inventory';

  listWarehouses(filters: {
    branchId?: number | null;
    status?: string | null;
    code?: string | null;
    page?: number;
    size?: number;
  } = {}): Observable<ApiResponse<PageResult<Warehouse>>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 50));
    if (filters.branchId) params = params.set('branchId', String(filters.branchId));
    if (filters.status) params = params.set('status', filters.status);
    if (filters.code) params = params.set('code', filters.code);
    return this.http.get<ApiResponse<PageResult<Warehouse>>>(this.warehousesUrl, { params });
  }

  getWarehouse(id: number): Observable<ApiResponse<Warehouse>> {
    return this.http.get<ApiResponse<Warehouse>>(`${this.warehousesUrl}/${id}`);
  }

  createWarehouse(body: Record<string, unknown>): Observable<ApiResponse<Warehouse>> {
    return this.http.post<ApiResponse<Warehouse>>(this.warehousesUrl, body);
  }

  updateWarehouse(id: number, body: Record<string, unknown>): Observable<ApiResponse<Warehouse>> {
    return this.http.put<ApiResponse<Warehouse>>(`${this.warehousesUrl}/${id}`, body);
  }

  listStock(filters: {
    warehouseId?: number | null;
    sparePartId?: number | null;
    branchId?: number | null;
    code?: string | null;
    page?: number;
    size?: number;
  } = {}): Observable<ApiResponse<PageResult<WarehouseStock>>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 50));
    if (filters.warehouseId) params = params.set('warehouseId', String(filters.warehouseId));
    if (filters.sparePartId) params = params.set('sparePartId', String(filters.sparePartId));
    if (filters.branchId) params = params.set('branchId', String(filters.branchId));
    if (filters.code) params = params.set('code', filters.code);
    return this.http.get<ApiResponse<PageResult<WarehouseStock>>>(`${this.inventoryUrl}/stock`, { params });
  }

  openingBalance(body: {
    warehouseId: number;
    sparePartId: number;
    quantity: number;
    description?: string | null;
  }): Observable<ApiResponse<WarehouseStock>> {
    return this.http.post<ApiResponse<WarehouseStock>>(`${this.inventoryUrl}/stock/opening-balance`, body);
  }

  listTransactions(filters: {
    warehouseId?: number | null;
    sparePartId?: number | null;
    branchId?: number | null;
    transactionType?: string | null;
    workOrderId?: number | null;
    reference?: string | null;
    page?: number;
    size?: number;
  } = {}): Observable<ApiResponse<PageResult<InventoryTransaction>>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 50));
    if (filters.warehouseId) params = params.set('warehouseId', String(filters.warehouseId));
    if (filters.sparePartId) params = params.set('sparePartId', String(filters.sparePartId));
    if (filters.branchId) params = params.set('branchId', String(filters.branchId));
    if (filters.transactionType) params = params.set('transactionType', filters.transactionType);
    if (filters.workOrderId) params = params.set('workOrderId', String(filters.workOrderId));
    if (filters.reference) params = params.set('reference', filters.reference);
    return this.http.get<ApiResponse<PageResult<InventoryTransaction>>>(`${this.inventoryUrl}/transactions`, { params });
  }

  issue(body: {
    warehouseId: number;
    workOrderId: number;
    workOrderPartId: number;
    quantity: number;
  }): Observable<ApiResponse<StockMovement>> {
    return this.http.post<ApiResponse<StockMovement>>(`${this.inventoryUrl}/stock/issue`, body);
  }

  returnStock(body: {
    warehouseId: number;
    workOrderId: number;
    workOrderPartId: number;
    quantity: number;
  }): Observable<ApiResponse<StockMovement>> {
    return this.http.post<ApiResponse<StockMovement>>(`${this.inventoryUrl}/stock/return`, body);
  }
}
