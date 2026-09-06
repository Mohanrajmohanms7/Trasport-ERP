import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors: any;
}

export interface TrialBalanceRow {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  openingBalance: number;
  periodDebit: number;
  periodCredit: number;
  closingDebit: number;
  closingCredit: number;
  closingBalance: number;
}

export interface TrialBalanceResponse {
  startDate: string;
  endDate: string;
  rows: TrialBalanceRow[];
  totalOpeningBalance: number;
  totalPeriodDebit: number;
  totalPeriodCredit: number;
  totalClosingDebit: number;
  totalClosingCredit: number;
  difference: number;
}

export interface GeneralLedgerEntry {
  jvId: number;
  voucherDate: string;
  voucherNumber: string;
  referenceNumber: string;
  description: string;
  debit: number;
  credit: number;
  runningBalance: number;
}

export interface GeneralLedgerResponse {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  startDate: string;
  endDate: string;
  openingBalance: number;
  closingBalance: number;
  totalDebit: number;
  totalCredit: number;
  entries: GeneralLedgerEntry[];
}

export interface ProfitLossRow {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  amount: number;
}

export interface ProfitLossResponse {
  startDate: string;
  endDate: string;
  incomeRows: ProfitLossRow[];
  expenseRows: ProfitLossRow[];
  totalIncome: number;
  totalExpense: number;
  netProfit: number;
  netLoss: number;
  isProfitable: boolean;
}

export interface BalanceSheetRow {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  balance: number;
}

export interface BalanceSheetResponse {
  asOfDate: string;
  assetRows: BalanceSheetRow[];
  liabilityRows: BalanceSheetRow[];
  equityRows: BalanceSheetRow[];
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  netProfitCurrentPeriod: number;
  totalLiabilitiesAndEquity: number;
  difference: number;
}

@Injectable({
  providedIn: 'root'
})
export class FinancialReportService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/financial-reports';

  getTrialBalance(companyId?: number, startDate?: string, endDate?: string): Observable<ApiResponse<TrialBalanceResponse>> {
    let params = new HttpParams();
    if (companyId) params = params.set('companyId', companyId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<TrialBalanceResponse>>(`${this.baseUrl}/trial-balance`, { params });
  }

  getGeneralLedger(accountId: number, companyId?: number, startDate?: string, endDate?: string): Observable<ApiResponse<GeneralLedgerResponse>> {
    let params = new HttpParams().set('accountId', accountId.toString());
    if (companyId) params = params.set('companyId', companyId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<GeneralLedgerResponse>>(`${this.baseUrl}/general-ledger`, { params });
  }

  getProfitAndLoss(companyId?: number, startDate?: string, endDate?: string): Observable<ApiResponse<ProfitLossResponse>> {
    let params = new HttpParams();
    if (companyId) params = params.set('companyId', companyId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<ProfitLossResponse>>(`${this.baseUrl}/profit-loss`, { params });
  }

  getBalanceSheet(companyId?: number, asOfDate?: string): Observable<ApiResponse<BalanceSheetResponse>> {
    let params = new HttpParams();
    if (companyId) params = params.set('companyId', companyId.toString());
    if (asOfDate) params = params.set('asOfDate', asOfDate);
    return this.http.get<ApiResponse<BalanceSheetResponse>>(`${this.baseUrl}/balance-sheet`, { params });
  }
}
