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
