import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ReportMgmtService, ReportTemplate, ScheduledReport } from '../../services/report-mgmt.service';
import { DashboardService } from '../../services/dashboard.service';
import { FinancialReportService, TrialBalanceResponse, GeneralLedgerResponse, ProfitLossResponse, BalanceSheetResponse } from '../../services/financial-report.service';
import { AccountsMgmtService } from '../../services/accounts-mgmt.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import { FfDropdownComponent, FfSelectOption, FfTextboxComponent, FfButtonComponent, FfNotificationService, FfToastComponent } from '@ff/ui';

@Component({
  selector: 'app-report-details-console',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfButtonComponent,
    FfToastComponent
  ],
  templateUrl: './report-details-console.html',
  styles: []
})
export class ReportDetailsConsoleComponent implements OnInit {
  private reportMgmtService = inject(ReportMgmtService);
  private dashboardService = inject(DashboardService);
  private financialReportService = inject(FinancialReportService);
  private accountsMgmtService = inject(AccountsMgmtService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  activeTab = signal<string>('bi');
  activeFinancialSubTab = signal<string>('trial-balance');

  loading = signal<boolean>(false);
  biLoading = signal<boolean>(false);
  reportsLoading = signal<boolean>(false);
  exportLoading = signal<boolean>(false);
  showTemplateEditor = signal<boolean>(false);
  showScheduleEditor = signal<boolean>(false);

  templates = signal<ReportTemplate[]>([]);
  schedules = signal<ScheduledReport[]>([]);
  reportTypes = signal<string[]>(['FLEET', 'REVENUE', 'EXPENSE', 'TRIP', 'FUEL']);

  get reportTypeOptions(): FfSelectOption[] {
    return this.reportTypes().map(type => ({ label: type, value: type }));
  }
  get reportTemplateOptions(): FfSelectOption[] {
    return [
      { label: '-- Choose Template --', value: '' },
      ...this.templates().map(template => ({ label: template.templateName, value: template.id ?? '' }))
    ];
  }

  templateForm!: FormGroup;
  scheduleForm!: FormGroup;

  todayRevenue = signal<number>(0);
  todayExpenses = signal<number>(0);
  todayProfit = signal<number>(0);
  runningVehicles = signal<number>(0);
  idleVehicles = signal<number>(0);
  revenueTrend = signal<number[]>([]);
  trendLabels = signal<string[]>([]);

  // Financial Reports Signals
  trialBalanceData = signal<TrialBalanceResponse | null>(null);
  generalLedgerData = signal<GeneralLedgerResponse | null>(null);
  profitLossData = signal<ProfitLossResponse | null>(null);
  balanceSheetData = signal<BalanceSheetResponse | null>(null);

  coaAccounts = signal<any[]>([]);
  selectedGlAccountId = signal<number | null>(null);

  startDateFilter = signal<string>('');
  endDateFilter = signal<string>('');
  asOfDateFilter = signal<string>('');

  get coaAccountOptions(): FfSelectOption[] {
    return [
      { label: '-- Select COA Account --', value: '' },
      ...this.coaAccounts().map(acc => ({
        label: `${acc.accountCode} - ${acc.accountName} (${acc.accountType})`,
        value: acc.id ?? ''
      }))
    ];
  }

  chartPath = computed(() => this.buildPolyline(this.revenueTrend(), 600, 150));
  chartAreaPath = computed(() => {
    const line = this.buildPolyline(this.revenueTrend(), 600, 150);
    if (!line) return '';
    return `${line} L 600,150 L 0,150 Z`;
  });

  ngOnInit() {
    this.initForms();
    this.loadBiMetrics();
    this.loadTemplates();
    this.loadSchedules();
    this.loadCoaAccounts();
  }

  initForms() {
    this.templateForm = this.fb.group({
      templateName: ['', Validators.required],
      reportType: ['FLEET', Validators.required],
      columnsList: ['', Validators.required]
    });

    this.scheduleForm = this.fb.group({
      reportTemplate: this.fb.group({
        id: ['', Validators.required]
      }),
      cronExpression: ['0 0 12 * * ?', Validators.required],
      recipientEmail: ['', [Validators.required, Validators.email]]
    });
  }

  loadCoaAccounts() {
    this.accountsMgmtService.getAccounts().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          const list = res.data.content || res.data || [];
          this.coaAccounts.set(list);
          if (list.length > 0 && !this.selectedGlAccountId()) {
            this.selectedGlAccountId.set(list[0].id);
          }
        }
      }
    });
  }

  loadBiMetrics() {
    this.biLoading.set(true);
    this.dashboardService.getAdminMetrics().subscribe({
      next: (res: any) => {
        this.biLoading.set(false);
        if (res && res.success && res.data) {
          const d = res.data;
          this.todayRevenue.set(d.revenueToday || 0);
          this.todayExpenses.set(d.todayExpenses || 0);
          const profit = (d.revenueToday || 0) - (d.todayExpenses || 0);
          this.todayProfit.set(profit);
          this.runningVehicles.set(d.runningVehicles || 0);
          this.idleVehicles.set(d.availableVehicles || 0);
          if (Array.isArray(d.monthlyRevenueTrend)) {
            this.revenueTrend.set(d.monthlyRevenueTrend.map((v: any) => Number(v) || 0));
            this.trendLabels.set(this.lastMonthsLabels(d.monthlyRevenueTrend.length));
          }
        }
      },
      error: () => this.biLoading.set(false)
    });
  }

  // Financial Reports Actions
  loadTrialBalance() {
    this.reportsLoading.set(true);
    this.financialReportService.getTrialBalance(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (res) => {
        this.reportsLoading.set(false);
        if (res.success) {
          this.trialBalanceData.set(res.data);
        }
      },
      error: (err) => {
        this.reportsLoading.set(false);
        this.notify.error(err?.error?.message || 'Failed to load Trial Balance report.');
      }
    });
  }

  loadGeneralLedger() {
    const accId = this.selectedGlAccountId();
    if (!accId) {
      this.notify.warning('Please select an account for General Ledger report.');
      return;
    }
    this.reportsLoading.set(true);
    this.financialReportService.getGeneralLedger(accId, undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (res) => {
        this.reportsLoading.set(false);
        if (res.success) {
          this.generalLedgerData.set(res.data);
        }
      },
      error: (err) => {
        this.reportsLoading.set(false);
        this.notify.error(err?.error?.message || 'Failed to load General Ledger report.');
      }
    });
  }

  loadProfitAndLoss() {
    this.reportsLoading.set(true);
    this.financialReportService.getProfitAndLoss(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (res) => {
        this.reportsLoading.set(false);
        if (res.success) {
          this.profitLossData.set(res.data);
        }
      },
      error: (err) => {
        this.reportsLoading.set(false);
        this.notify.error(err?.error?.message || 'Failed to load Profit & Loss report.');
      }
    });
  }

  loadBalanceSheet() {
    this.reportsLoading.set(true);
    this.financialReportService.getBalanceSheet(undefined, this.asOfDateFilter() || undefined).subscribe({
      next: (res) => {
        this.reportsLoading.set(false);
        if (res.success) {
          this.balanceSheetData.set(res.data);
        }
      },
      error: (err) => {
        this.reportsLoading.set(false);
        this.notify.error(err?.error?.message || 'Failed to load Balance Sheet report.');
      }
    });
  }

  // ==========================================
  // EXPORT & PRINT HANDLERS
  // ==========================================
  // 1. Trial Balance Exports
  exportTrialBalancePdf() {
    this.exportLoading.set(true);
    this.financialReportService.downloadTrialBalancePdf(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'Trial_Balance_Report.pdf');
        this.notify.success('Trial Balance PDF downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download Trial Balance PDF.');
      }
    });
  }

  exportTrialBalanceCsv() {
    this.exportLoading.set(true);
    this.financialReportService.downloadTrialBalanceCsv(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'Trial_Balance_Report.csv');
        this.notify.success('Trial Balance CSV downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download Trial Balance CSV.');
      }
    });
  }

  printTrialBalance() {
    this.exportLoading.set(true);
    this.financialReportService.getTrialBalancePrint(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (res) => {
        this.exportLoading.set(false);
        if (res.success && res.data) {
          const wrapper = res.data;
          const data = wrapper.reportData;
          let rowsHtml = '';
          (data.rows || []).forEach(r => {
            rowsHtml += `
              <tr>
                <td>${r.accountCode || ''}</td>
                <td>${r.accountName || ''}</td>
                <td>${r.accountType || ''}</td>
                <td class="num">₹${(r.openingBalance || 0).toFixed(2)}</td>
                <td class="num">₹${(r.periodDebit || 0).toFixed(2)}</td>
                <td class="num">₹${(r.periodCredit || 0).toFixed(2)}</td>
                <td class="num">₹${(r.closingDebit || 0).toFixed(2)}</td>
                <td class="num">₹${(r.closingCredit || 0).toFixed(2)}</td>
              </tr>`;
          });
          const content = `
            <div class="header">
              <div>
                <div class="comp-title">${wrapper.companyName || 'TRANSAFLOW ERP'}</div>
                <div>${wrapper.companyAddress || ''} | GSTIN: ${wrapper.companyGSTIN || ''}</div>
              </div>
              <div class="doc-title">
                <div>TRIAL BALANCE STATEMENT</div>
                <div style="font-size:12px; font-weight:normal;">Period: ${data.startDate} to ${data.endDate}</div>
              </div>
            </div>
            <table>
              <thead>
                <tr><th>Code</th><th>Account Name</th><th>Type</th><th class="num">Opening Bal</th><th class="num">Debit</th><th class="num">Credit</th><th class="num">Closing Debit</th><th class="num">Closing Credit</th></tr>
              </thead>
              <tbody>${rowsHtml}</tbody>
              <tfoot>
                <tr style="font-weight:bold; background:#f1f5f9;">
                  <td colspan="3" style="text-align:right;">Totals:</td>
                  <td class="num">₹${(data.totalOpeningBalance || 0).toFixed(2)}</td>
                  <td class="num">₹${(data.totalPeriodDebit || 0).toFixed(2)}</td>
                  <td class="num">₹${(data.totalPeriodCredit || 0).toFixed(2)}</td>
                  <td class="num">₹${(data.totalClosingDebit || 0).toFixed(2)}</td>
                  <td class="num">₹${(data.totalClosingCredit || 0).toFixed(2)}</td>
                </tr>
              </tfoot>
            </table>`;
          this.printHtmlContent('Trial Balance Statement', content);
        }
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not prepare Trial Balance print view.');
      }
    });
  }

  // 2. General Ledger Exports
  exportGeneralLedgerPdf() {
    const accId = this.selectedGlAccountId();
    if (!accId) {
      this.notify.warning('Please select an account first.');
      return;
    }
    this.exportLoading.set(true);
    this.financialReportService.downloadGeneralLedgerPdf(accId, undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'General_Ledger_Report.pdf');
        this.notify.success('General Ledger PDF downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download General Ledger PDF.');
      }
    });
  }

  exportGeneralLedgerCsv() {
    const accId = this.selectedGlAccountId();
    if (!accId) {
      this.notify.warning('Please select an account first.');
      return;
    }
    this.exportLoading.set(true);
    this.financialReportService.downloadGeneralLedgerCsv(accId, undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'General_Ledger_Report.csv');
        this.notify.success('General Ledger CSV downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download General Ledger CSV.');
      }
    });
  }

  printGeneralLedger() {
    const accId = this.selectedGlAccountId();
    if (!accId) {
      this.notify.warning('Please select an account first.');
      return;
    }
    this.exportLoading.set(true);
    this.financialReportService.getGeneralLedgerPrint(accId, undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (res) => {
        this.exportLoading.set(false);
        if (res.success && res.data) {
          const wrapper = res.data;
          const data = wrapper.reportData;
          let entriesHtml = '';
          (data.entries || []).forEach(e => {
            entriesHtml += `
              <tr>
                <td>${e.voucherDate || ''}</td>
                <td>${e.voucherNumber || ''}</td>
                <td>${e.referenceNumber || ''}</td>
                <td>${e.description || ''}</td>
                <td class="num">₹${(e.debit || 0).toFixed(2)}</td>
                <td class="num">₹${(e.credit || 0).toFixed(2)}</td>
                <td class="num">₹${(e.runningBalance || 0).toFixed(2)}</td>
              </tr>`;
          });
          const content = `
            <div class="header">
              <div>
                <div class="comp-title">${wrapper.companyName || 'TRANSAFLOW ERP'}</div>
                <div>${wrapper.companyAddress || ''}</div>
              </div>
              <div class="doc-title">
                <div>GENERAL LEDGER STATEMENT</div>
                <div style="font-size:12px; font-weight:normal;">Account: ${data.accountCode} - ${data.accountName} (${data.accountType})</div>
              </div>
            </div>
            <div class="summary-card">
              Opening Balance: <strong>₹${(data.openingBalance || 0).toFixed(2)}</strong> | 
              Total Debit: <strong>₹${(data.totalDebit || 0).toFixed(2)}</strong> | 
              Total Credit: <strong>₹${(data.totalCredit || 0).toFixed(2)}</strong> | 
              Closing Balance: <strong>₹${(data.closingBalance || 0).toFixed(2)}</strong>
            </div>
            <table>
              <thead>
                <tr><th>Date</th><th>Voucher No</th><th>Reference</th><th>Description</th><th class="num">Debit</th><th class="num">Credit</th><th class="num">Running Bal</th></tr>
              </thead>
              <tbody>${entriesHtml}</tbody>
            </table>`;
          this.printHtmlContent('General Ledger Statement', content);
        }
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not prepare General Ledger print view.');
      }
    });
  }

  // 3. Profit & Loss Exports
  exportProfitLossPdf() {
    this.exportLoading.set(true);
    this.financialReportService.downloadProfitLossPdf(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'Profit_Loss_Report.pdf');
        this.notify.success('Profit & Loss PDF downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download Profit & Loss PDF.');
      }
    });
  }

  exportProfitLossCsv() {
    this.exportLoading.set(true);
    this.financialReportService.downloadProfitLossCsv(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'Profit_Loss_Report.csv');
        this.notify.success('Profit & Loss CSV downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download Profit & Loss CSV.');
      }
    });
  }

  printProfitLoss() {
    this.exportLoading.set(true);
    this.financialReportService.getProfitLossPrint(undefined, this.startDateFilter() || undefined, this.endDateFilter() || undefined).subscribe({
      next: (res) => {
        this.exportLoading.set(false);
        if (res.success && res.data) {
          const wrapper = res.data;
          const data = wrapper.reportData;
          let incHtml = '', expHtml = '';
          (data.incomeRows || []).forEach(r => {
            incHtml += `<tr><td>${r.accountCode} - ${r.accountName}</td><td class="num">₹${(r.amount || 0).toFixed(2)}</td></tr>`;
          });
          (data.expenseRows || []).forEach(r => {
            expHtml += `<tr><td>${r.accountCode} - ${r.accountName}</td><td class="num">₹${(r.amount || 0).toFixed(2)}</td></tr>`;
          });
          const netRes = data.isProfitable ? `NET PROFIT: ₹${(data.netProfit || 0).toFixed(2)}` : `NET LOSS: ₹${(data.netLoss || 0).toFixed(2)}`;
          const content = `
            <div class="header">
              <div>
                <div class="comp-title">${wrapper.companyName || 'TRANSAFLOW ERP'}</div>
                <div>${wrapper.companyAddress || ''}</div>
              </div>
              <div class="doc-title">
                <div>PROFIT & LOSS STATEMENT</div>
                <div style="font-size:12px; font-weight:normal;">Period: ${data.startDate} to ${data.endDate}</div>
              </div>
            </div>
            <h3>INCOME / REVENUE</h3>
            <table>
              <thead><tr><th>Account Name</th><th class="num">Amount</th></tr></thead>
              <tbody>${incHtml}</tbody>
              <tfoot><tr style="font-weight:bold;"><td>Total Income</td><td class="num">₹${(data.totalIncome || 0).toFixed(2)}</td></tr></tfoot>
            </table>
            <h3 style="margin-top:20px;">OPERATING EXPENSES</h3>
            <table>
              <thead><tr><th>Account Name</th><th class="num">Amount</th></tr></thead>
              <tbody>${expHtml}</tbody>
              <tfoot><tr style="font-weight:bold;"><td>Total Expenses</td><td class="num">₹${(data.totalExpense || 0).toFixed(2)}</td></tr></tfoot>
            </table>
            <div class="summary-card" style="margin-top:20px; font-size:16px; font-weight:bold; text-align:center;">
              ${netRes}
            </div>`;
          this.printHtmlContent('Profit & Loss Statement', content);
        }
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not prepare Profit & Loss print view.');
      }
    });
  }

  // 4. Balance Sheet Exports
  exportBalanceSheetPdf() {
    this.exportLoading.set(true);
    this.financialReportService.downloadBalanceSheetPdf(undefined, this.asOfDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'Balance_Sheet_Report.pdf');
        this.notify.success('Balance Sheet PDF downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download Balance Sheet PDF.');
      }
    });
  }

  exportBalanceSheetCsv() {
    this.exportLoading.set(true);
    this.financialReportService.downloadBalanceSheetCsv(undefined, this.asOfDateFilter() || undefined).subscribe({
      next: (blob) => {
        this.exportLoading.set(false);
        this.downloadFile(blob, 'Balance_Sheet_Report.csv');
        this.notify.success('Balance Sheet CSV downloaded.');
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not download Balance Sheet CSV.');
      }
    });
  }

  printBalanceSheet() {
    this.exportLoading.set(true);
    this.financialReportService.getBalanceSheetPrint(undefined, this.asOfDateFilter() || undefined).subscribe({
      next: (res) => {
        this.exportLoading.set(false);
        if (res.success && res.data) {
          const wrapper = res.data;
          const data = wrapper.reportData;
          let assetHtml = '', liabHtml = '', eqHtml = '';
          (data.assetRows || []).forEach(r => assetHtml += `<tr><td>${r.accountCode} - ${r.accountName}</td><td class="num">₹${(r.balance || 0).toFixed(2)}</td></tr>`);
          (data.liabilityRows || []).forEach(r => liabHtml += `<tr><td>${r.accountCode} - ${r.accountName}</td><td class="num">₹${(r.balance || 0).toFixed(2)}</td></tr>`);
          (data.equityRows || []).forEach(r => eqHtml += `<tr><td>${r.accountName}</td><td class="num">₹${(r.balance || 0).toFixed(2)}</td></tr>`);

          const content = `
            <div class="header">
              <div>
                <div class="comp-title">${wrapper.companyName || 'TRANSAFLOW ERP'}</div>
                <div>${wrapper.companyAddress || ''}</div>
              </div>
              <div class="doc-title">
                <div>BALANCE SHEET STATEMENT</div>
                <div style="font-size:12px; font-weight:normal;">As of Date: ${data.asOfDate}</div>
              </div>
            </div>
            <h3>ASSETS</h3>
            <table><thead><tr><th>Account Name</th><th class="num">Balance</th></tr></thead><tbody>${assetHtml}</tbody><tfoot><tr style="font-weight:bold;"><td>Total Assets</td><td class="num">₹${(data.totalAssets || 0).toFixed(2)}</td></tr></tfoot></table>
            <h3 style="margin-top:15px;">LIABILITIES</h3>
            <table><thead><tr><th>Account Name</th><th class="num">Balance</th></tr></thead><tbody>${liabHtml}</tbody><tfoot><tr style="font-weight:bold;"><td>Total Liabilities</td><td class="num">₹${(data.totalLiabilities || 0).toFixed(2)}</td></tr></tfoot></table>
            <h3 style="margin-top:15px;">EQUITY</h3>
            <table><thead><tr><th>Account Name</th><th class="num">Balance</th></tr></thead><tbody>${eqHtml}</tbody><tfoot><tr style="font-weight:bold;"><td>Total Equity</td><td class="num">₹${(data.totalEquity || 0).toFixed(2)}</td></tr></tfoot></table>
            <div class="summary-card" style="margin-top:20px; text-align:center;">
              Assets: <strong>₹${(data.totalAssets || 0).toFixed(2)}</strong> = Liabilities & Equity: <strong>₹${(data.totalLiabilitiesAndEquity || 0).toFixed(2)}</strong> (Difference: ₹${(data.difference || 0).toFixed(2)})
            </div>`;
          this.printHtmlContent('Balance Sheet Statement', content);
        }
      },
      error: () => {
        this.exportLoading.set(false);
        this.notify.error('Could not prepare Balance Sheet print view.');
      }
    });
  }

  private printHtmlContent(title: string, htmlContent: string) {
    const printWindow = window.open('', '_blank', 'width=1000,height=800');
    if (!printWindow) {
      this.notify.warning('Pop-up blocked. Please allow pop-ups to view print preview.');
      return;
    }
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>${title}</title>
          <style>
            body { font-family: system-ui, -apple-system, sans-serif; padding: 20px; color: #1e293b; line-height: 1.4; }
            .header { display: flex; justify-content: space-between; border-bottom: 2px solid #1e3a8a; padding-bottom: 10px; margin-bottom: 20px; }
            .comp-title { font-size: 20px; font-weight: bold; color: #1e3a8a; }
            .doc-title { font-size: 18px; font-weight: bold; text-align: right; color: #1e3a8a; }
            table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
            th { background: #1e3a8a; color: white; padding: 8px; text-align: left; }
            td { border-bottom: 1px solid #e2e8f0; padding: 6px 8px; }
            .num { text-align: right; font-family: monospace; }
            .summary-card { background: #f8fafc; border: 1px solid #cbd5e1; padding: 12px; border-radius: 8px; margin-bottom: 15px; }
            @media print { body { padding: 0; } }
          </style>
        </head>
        <body>
          ${htmlContent}
          <script>
            window.onload = function() {
              window.print();
              window.onafterprint = function() { window.close(); };
            };
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }

  private downloadFile(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  switchFinancialSubTab(tab: string) {
    this.activeFinancialSubTab.set(tab);
    if (tab === 'trial-balance' && !this.trialBalanceData()) {
      this.loadTrialBalance();
    } else if (tab === 'general-ledger' && !this.generalLedgerData()) {
      this.loadGeneralLedger();
    } else if (tab === 'profit-loss' && !this.profitLossData()) {
      this.loadProfitAndLoss();
    } else if (tab === 'balance-sheet' && !this.balanceSheetData()) {
      this.loadBalanceSheet();
    }
  }

  onTabChange(tab: string) {
    this.activeTab.set(tab);
    if (tab === 'financial-reports' && !this.trialBalanceData()) {
      this.loadTrialBalance();
    }
  }

  onGlAccountChange(value: any) {
    const accId = Number(value);
    if (accId) {
      this.selectedGlAccountId.set(accId);
      this.loadGeneralLedger();
    }
  }

  loadTemplates() {
    this.loading.set(true);
    this.reportMgmtService.getTemplates().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.templates.set(res.data.content || res.data || []);
        }
      },
      error: () => this.loading.set(false)
    });
  }

  loadSchedules() {
    this.reportMgmtService.getSchedules().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.schedules.set(res.data.content || res.data || []);
        }
      }
    });
  }

  saveTemplate() {
    if (this.templateForm.invalid) return;
    const val = this.templateForm.value;
    const columnsArray = typeof val.columnsList === 'string'
      ? val.columnsList.split(',').map((s: string) => s.trim()).filter(Boolean)
      : val.columnsList;

    const payload: ReportTemplate = {
      templateName: val.templateName || '',
      reportType: val.reportType || 'FLEET',
      columnsList: JSON.stringify(columnsArray),
      selectedColumnsJson: JSON.stringify(columnsArray)
    };

    this.reportMgmtService.createTemplate(payload).subscribe(() => {
      this.showTemplateEditor.set(false);
      this.templateForm.reset({ reportType: 'FLEET' });
      this.loadTemplates();
    });
  }

  deleteTemplate(tpl: ReportTemplate) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Report Template',
        message: `Are you sure you want to remove the template "${tpl.templateName}"?`,
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && tpl.id) {
        this.reportMgmtService.deleteTemplate(tpl.id).subscribe(() => this.loadTemplates());
      }
    });
  }

  saveSchedule() {
    if (this.scheduleForm.invalid) return;
    const val = this.scheduleForm.value;
    const payload: ScheduledReport = {
      reportTemplate: { id: Number(val.reportTemplate?.id || val.reportTemplate) },
      cronExpression: val.cronExpression || '',
      recipientEmail: val.recipientEmail || '',
      active: true
    };

    this.reportMgmtService.createSchedule(payload).subscribe(() => {
      this.showScheduleEditor.set(false);
      this.scheduleForm.reset({ cronExpression: '0 0 12 * * ?' });
      this.loadSchedules();
    });
  }

  deleteSchedule(sch: ScheduledReport) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Schedule trigger',
        message: 'Are you sure you want to remove this scheduled trigger?',
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && sch.id) {
        this.reportMgmtService.deleteSchedule(sch.id).subscribe(() => this.loadSchedules());
      }
    });
  }

  exportXlsx(tpl: ReportTemplate) {
    this.reportMgmtService.exportReport({ templateId: tpl.id, format: 'CSV' }).subscribe({
      next: (res) => {
        if (res.success && res.data?.contentBase64) {
          const binary = atob(res.data.contentBase64);
          const bytes = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
          const blob = new Blob([bytes], { type: res.data.mimeType || 'text/csv' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = res.data.fileName || `${tpl.templateName || 'report'}.csv`;
          a.click();
          URL.revokeObjectURL(url);
          this.notify.success('Report download started.');
        } else if (res.success && res.data?.downloadUrl) {
          window.open(res.data.downloadUrl, '_blank', 'noopener');
          this.notify.success('Report download started.');
        } else {
          this.notify.warning(res.message || 'Report was generated but no download data was returned.');
        }
      },
      error: (err) => {
        this.notify.error(err?.error?.message || 'Could not export report. Please try again.');
      }
    });
  }

  private lastMonthsLabels(count: number): string[] {
    const labels: string[] = [];
    const now = new Date();
    for (let i = count - 1; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      labels.push(d.toLocaleString('en', { month: 'short' }));
    }
    return labels;
  }

  private buildPolyline(values: number[], width: number, height: number): string {
    if (!values.length) return '';
    const max = Math.max(...values, 1);
    const pad = 10;
    const usableH = height - pad * 2;
    const step = values.length === 1 ? 0 : width / (values.length - 1);
    const points = values.map((v, i) => {
      const x = i * step;
      const y = pad + usableH - (v / max) * usableH;
      return `${x},${y}`;
    });
    return `M ${points.join(' L ')}`;
  }
}
