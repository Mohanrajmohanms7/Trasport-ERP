import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ReportMgmtService, ReportTemplate, ScheduledReport } from '../../services/report-mgmt.service';
import { DashboardService } from '../../services/dashboard.service';
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
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private notify = inject(FfNotificationService);

  activeTab = signal<string>('bi');
  loading = signal<boolean>(false);
  biLoading = signal<boolean>(false);
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

  loadBiMetrics() {
    this.biLoading.set(true);
    this.dashboardService.getAdminMetrics().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const d = res.data;
          const revenue = Number(d.revenueToday ?? 0);
          const expenses = Number(d.todayExpenses ?? 0);
          this.todayRevenue.set(revenue);
          this.todayExpenses.set(expenses);
          this.todayProfit.set(revenue - expenses);
          this.runningVehicles.set(Number(d.runningVehicles ?? 0));
          this.idleVehicles.set(Number(d.availableVehicles ?? 0));
          const trend = (d.monthlyRevenueTrend || []).map((v: any) => Number(v) || 0);
          this.revenueTrend.set(trend);
          this.trendLabels.set(this.lastMonthsLabels(trend.length || 5));
        }
        this.biLoading.set(false);
      },
      error: () => {
        this.biLoading.set(false);
        this.notify.error('Could not load BI metrics for this company.');
      }
    });
  }

  loadTemplates() {
    this.loading.set(true);
    this.reportMgmtService.getTemplates().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.templates.set(res.data.content || res.data);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  loadSchedules() {
    this.reportMgmtService.getSchedules().subscribe(res => {
      if (res.success && res.data) {
        this.schedules.set(res.data.content || res.data);
      }
    });
  }

  openAddTemplate() {
    this.templateForm.reset({ reportType: 'FLEET' });
    this.showTemplateEditor.set(true);
  }

  openAddSchedule() {
    this.scheduleForm.reset({ cronExpression: '0 0 12 * * ?' });
    this.showScheduleEditor.set(true);
  }

  saveTemplate() {
    if (this.templateForm.invalid) return;
    this.loading.set(true);
    this.reportMgmtService.createTemplate(this.templateForm.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadTemplates();
        this.showTemplateEditor.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  saveSchedule() {
    if (this.scheduleForm.invalid) return;
    this.loading.set(true);
    this.reportMgmtService.createSchedule(this.scheduleForm.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSchedules();
        this.showScheduleEditor.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  deleteTemplate(tpl: ReportTemplate) {
    if (!tpl.id) return;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete Template',
        message: `Are you sure you want to remove report template: ${tpl.templateName}?`,
        type: 'danger'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && tpl.id) {
        this.reportMgmtService.deleteTemplate(tpl.id).subscribe(() => this.loadTemplates());
      }
    });
  }

  deleteSchedule(sch: ScheduledReport) {
    if (!sch.id) return;
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
