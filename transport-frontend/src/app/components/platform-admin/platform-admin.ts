import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { PlatformAdminService, PlatformStats, SaaSPlan, TenantSubscription, SaaSLicense, SupportTicket, SupportReply, Announcement, BackupLog, BillingInvoice } from '../../services/platform-admin.service';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';
import {
  FfDropdownComponent,
  FfSelectOption,
  FfTextboxComponent,
  FfNumberComponent,
  FfTextareaComponent,
  FfDatepickerComponent,
  FfCheckboxComponent,
  FfButtonComponent,
  FfPasswordComponent
} from '@ff/ui';

@Component({
  selector: 'app-platform-admin',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    MatMenuModule,
    FfDropdownComponent,
    FfTextboxComponent,
    FfNumberComponent,
    FfTextareaComponent,
    FfDatepickerComponent,
    FfPasswordComponent
  ],
  templateUrl: './platform-admin.html',
  styles: [`
    :host {
      display: block;
      height: 100%;
      min-height: 0;
      overflow: hidden;
    }

    .pa-shell {
      background: #f1f5f9;
      color: #0f172a;
      box-sizing: border-box;
    }
    :host-context(html.dark) .pa-shell,
    :host-context(.dark) .pa-shell {
      background: #0b1220;
      color: #f1f5f9;
    }

    /* Light hero — readable; never shrink/clip in flex layout */
    .pa-hero {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 8px 24px rgba(15, 23, 42, 0.06);
      position: relative;
      overflow: visible;
      flex-shrink: 0;
      min-height: auto;
    }
    .pa-hero::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
      background: #2563eb;
    }
    .pa-hero .pa-hero-kicker {
      color: #2563eb !important;
    }
    .pa-hero .pa-hero-title,
    .pa-hero .pa-hero-title .material-icons {
      color: #0f172a !important;
    }
    .pa-hero .pa-hero-sub {
      color: #475569 !important;
    }
    .pa-hero .pa-sync-btn {
      background: #2563eb !important;
      color: #ffffff !important;
      border: none !important;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }
    .pa-hero .pa-sync-btn:hover {
      background: #1d4ed8 !important;
    }

    :host-context(html.dark) .pa-hero,
    :host-context(.dark) .pa-hero {
      background: #111827;
      border-color: #1f2937;
    }
    :host-context(html.dark) .pa-hero .pa-hero-kicker,
    :host-context(.dark) .pa-hero .pa-hero-kicker {
      color: #60a5fa !important;
    }
    :host-context(html.dark) .pa-hero .pa-hero-title,
    :host-context(html.dark) .pa-hero .pa-hero-title .material-icons,
    :host-context(.dark) .pa-hero .pa-hero-title,
    :host-context(.dark) .pa-hero .pa-hero-title .material-icons {
      color: #f8fafc !important;
    }
    :host-context(html.dark) .pa-hero .pa-hero-sub,
    :host-context(.dark) .pa-hero .pa-hero-sub {
      color: #94a3b8 !important;
    }

    .pa-nav-item {
      color: #334155 !important;
      transition: background 0.15s ease, color 0.15s ease;
    }
    .pa-nav-item .material-icons {
      color: #64748b !important;
    }
    .pa-nav-item:hover {
      background: #f1f5f9 !important;
      color: #0f172a !important;
    }
    .pa-nav-item.is-active {
      background: #eff6ff !important;
      color: #1d4ed8 !important;
      box-shadow: inset 3px 0 0 #2563eb;
      font-weight: 700;
    }
    .pa-nav-item.is-active .material-icons {
      color: #2563eb !important;
    }

    :host-context(html.dark) .pa-nav-item,
    :host-context(.dark) .pa-nav-item {
      color: #e2e8f0 !important;
    }
    :host-context(html.dark) .pa-nav-item .material-icons,
    :host-context(.dark) .pa-nav-item .material-icons {
      color: #94a3b8 !important;
    }
    :host-context(html.dark) .pa-nav-item:hover,
    :host-context(.dark) .pa-nav-item:hover {
      background: #1e293b !important;
      color: #f8fafc !important;
    }
    :host-context(html.dark) .pa-nav-item.is-active,
    :host-context(.dark) .pa-nav-item.is-active {
      background: rgba(37, 99, 235, 0.2) !important;
      color: #93c5fd !important;
      box-shadow: inset 3px 0 0 #3b82f6;
    }
    :host-context(html.dark) .pa-nav-item.is-active .material-icons,
    :host-context(.dark) .pa-nav-item.is-active .material-icons {
      color: #60a5fa !important;
    }

    .pa-panel {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    }
    :host-context(html.dark) .pa-panel,
    :host-context(.dark) .pa-panel {
      background: #111827;
      border-color: #1f2937;
    }

    .pa-stat {
      background: #ffffff !important;
      border: 1px solid #e2e8f0 !important;
    }
    .pa-stat .pa-stat-label {
      color: #64748b !important;
    }
    .pa-stat .pa-stat-value {
      color: #0f172a !important;
    }
    .pa-stat .pa-stat-meta {
      color: #475569 !important;
    }
    :host-context(html.dark) .pa-stat,
    :host-context(.dark) .pa-stat {
      background: #0f172a !important;
      border-color: #1e293b !important;
    }
    :host-context(html.dark) .pa-stat .pa-stat-label,
    :host-context(.dark) .pa-stat .pa-stat-label {
      color: #94a3b8 !important;
    }
    :host-context(html.dark) .pa-stat .pa-stat-value,
    :host-context(.dark) .pa-stat .pa-stat-value {
      color: #f8fafc !important;
    }
    :host-context(html.dark) .pa-stat .pa-stat-meta,
    :host-context(.dark) .pa-stat .pa-stat-meta {
      color: #cbd5e1 !important;
    }

    .pa-toolbar {
      background: #f8fafc !important;
      border: 1px solid #e2e8f0 !important;
    }
    :host-context(html.dark) .pa-toolbar,
    :host-context(.dark) .pa-toolbar {
      background: #0f172a !important;
      border-color: #1e293b !important;
    }

    .pa-table-wrap {
      border-radius: 12px;
      overflow: hidden;
      border: 1px solid #e2e8f0;
      background: #ffffff;
    }
    .pa-table-wrap table thead th {
      background: #f1f5f9 !important;
      color: #334155 !important;
      font-weight: 700;
    }
    .pa-table-wrap table tbody td {
      color: #1e293b !important;
    }
    .pa-table-wrap table tbody td .pa-muted {
      color: #64748b !important;
    }
    .pa-table-wrap tbody tr:hover {
      background: #f8fafc !important;
    }
    :host-context(html.dark) .pa-table-wrap,
    :host-context(.dark) .pa-table-wrap {
      background: #111827;
      border-color: #1e293b;
    }
    :host-context(html.dark) .pa-table-wrap table thead th,
    :host-context(.dark) .pa-table-wrap table thead th {
      background: #0f172a !important;
      color: #cbd5e1 !important;
    }
    :host-context(html.dark) .pa-table-wrap table tbody td,
    :host-context(.dark) .pa-table-wrap table tbody td {
      color: #e2e8f0 !important;
    }
    :host-context(html.dark) .pa-table-wrap table tbody td .pa-muted,
    :host-context(.dark) .pa-table-wrap table tbody td .pa-muted {
      color: #94a3b8 !important;
    }
    :host-context(html.dark) .pa-table-wrap tbody tr:hover,
    :host-context(.dark) .pa-table-wrap tbody tr:hover {
      background: #1e293b !important;
    }

    .kpi-card {
      background: #ffffff !important;
      border: 1px solid #e2e8f0 !important;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
    }
    :host-context(html.dark) .kpi-card,
    :host-context(.dark) .kpi-card {
      background: #0f172a !important;
      border-color: #1e293b !important;
    }

    .pa-btn-primary {
      background: #2563eb !important;
      color: #fff !important;
    }
    .pa-btn-primary:hover {
      background: #1d4ed8 !important;
    }
    .pa-btn-success {
      background: #059669 !important;
      color: #fff !important;
    }
    .pa-btn-success:hover {
      background: #047857 !important;
    }
  `]
})
export class PlatformAdminComponent implements OnInit {
  private platformService = inject(PlatformAdminService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);

  // General States
  activeTab = signal<string>('dashboard');
  loading = signal<boolean>(false);
  successMsg = signal<string>('');
  errorMsg = signal<string>('');

  // Dashboard Stats
  stats = signal<PlatformStats | null>(null);
  analytics = signal<any | null>(null);
  revenueGrowth = signal<any[]>([]);
  planDistribution = signal<any[]>([]);
  clientGrowth = signal<any[]>([]);
  tripGrowth = signal<any[]>([]);
  userGrowth = signal<any[]>([]);
  vehicleDistribution = signal<any[]>([]);
  recentClients = signal<any[]>([]);
  renewalDue = signal<any[]>([]);
  latestPayments = signal<any[]>([]);
  systemHealth = signal<any | null>(null);
  // Companies / Clients List
  companies = signal<any[]>([]);
  companyTotal = signal<number>(0);
  companyPage = signal<number>(0);
  companySearch = signal<string>('');
  clientFilterStatus = signal<string>('');
  showCompanyForm = signal<boolean>(false);
  isEditMode = signal<boolean>(false);
  selectedClient = signal<any | null>(null);
  showClientViewModal = signal<boolean>(false);
  companyForm!: FormGroup;

  // Phase 29 – Client Onboarding Wizard
  showOnboardingWizard = signal<boolean>(false);
  onboardingStep = signal<number>(1);
  onboardingBusy = signal<boolean>(false);
  onboardingResult = signal<any | null>(null);
  copiedField = signal<string>('');
  readonly onboardingStepsMeta = [
    { n: 1, label: 'Company Details' },
    { n: 2, label: 'Head Office' },
    { n: 3, label: 'Subscription' },
    { n: 4, label: 'Provision' },
    { n: 5, label: 'Client Ready' }
  ];
  readonly provisionChecklist = [
    'Create Company',
    'Create Head Office',
    'Generate Company Code',
    'Create Company Admin',
    'Generate Username',
    'Generate Temporary Password',
    'Assign Subscription',
    'Insert Default Masters',
    'Insert Default Roles',
    'Insert Default Permissions',
    'Send Welcome Email (queued)',
    'Client Ready'
  ];

  // Plans & Subscriptions
  plans = signal<SaaSPlan[]>([]);
  tenantSubscriptions = signal<TenantSubscription[]>([]);
  showPlanForm = signal<boolean>(false);
  editingPlan = signal<SaaSPlan | null>(null);
  planForm!: FormGroup;
  
  // Licenses
  licenses = signal<SaaSLicense[]>([]);

  // Users & Sessions
  users = signal<any[]>([]);
  userSearch = signal<string>('');
  activeSessions = signal<any[]>([]);
  loginHistory = signal<any[]>([]);
  logoutHistory = signal<any[]>([]);
  failedLogins = signal<any[]>([]);
  authTab = signal<string>('sessions');
  showUserForm = signal<boolean>(false);
  isUserEditMode = signal<boolean>(false);
  editingUser = signal<any | null>(null);
  showPasswordResetModal = signal<boolean>(false);
  selectedUserId = signal<number | null>(null);
  passwordForm!: FormGroup;
  userForm!: FormGroup;

  // System Settings
  systemSettings = signal<any[]>([]);

  // Support Tickets
  tickets = signal<SupportTicket[]>([]);
  selectedTicket = signal<SupportTicket | null>(null);
  showTicketThread = signal<boolean>(false);
  replyMessage = signal<string>('');

  // Announcements
  announcements = signal<Announcement[]>([]);
  showAnnouncementForm = signal<boolean>(false);
  announcementForm!: FormGroup;

  // Backup logs
  backups = signal<BackupLog[]>([]);

  // Billing
  invoices = signal<BillingInvoice[]>([]);

  // Audit Logs List
  auditLogsList = signal<any[]>([]);

  // Dropdowns Select Options
  statusOptions: FfSelectOption[] = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Suspended', value: 'SUSPENDED' },
    { label: 'Inactive', value: 'INACTIVE' }
  ];

  billingPeriodOptions: FfSelectOption[] = [
    { label: 'Monthly', value: 'MONTHLY' },
    { label: 'Yearly', value: 'YEARLY' }
  ];

  ticketStatusOptions: FfSelectOption[] = [
    { label: 'Open', value: 'OPEN' },
    { label: 'In Progress', value: 'IN_PROGRESS' },
    { label: 'Resolved', value: 'RESOLVED' },
    { label: 'Closed', value: 'CLOSED' }
  ];

  ticketPriorityOptions: FfSelectOption[] = [
    { label: 'Low', value: 'LOW' },
    { label: 'Medium', value: 'MEDIUM' },
    { label: 'High', value: 'HIGH' }
  ];

  ngOnInit(): void {
    this.initForms();
    this.loadDashboardData();
  }

  initForms(): void {
    this.companyForm = this.fb.group({
      id: [null],
      code: [''],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      ownerName: ['', [Validators.required, Validators.maxLength(150)]],
      businessType: ['', [Validators.required]],
      phone: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      gstNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/)]],
      panNumber: [''],
      address: ['', Validators.required],
      city: ['', Validators.required],
      state: ['', Validators.required],
      pincode: ['', Validators.required],
      logo: [''],
      website: [''],
      storage: ['10 GB'],
      subscriptionPlanId: [null],
      subscriptionStartDate: [null],
      subscriptionEndDate: [null],
      subscriptionRenewalDate: [null],
      subscriptionStatus: ['ACTIVE'],
      maxUsers: [5],
      maxVehicles: [5],
      billingMonths: [1],
      sendWelcomeEmail: [true],
      adminUsername: [''],
      adminPassword: ['']
    });

    this.planForm = this.fb.group({
      code: ['', Validators.required],
      name: ['', Validators.required],
      description: [''],
      price: [0, Validators.required],
      billingPeriod: ['MONTHLY', Validators.required],
      maxUsers: [5, Validators.required],
      maxVehicles: [5, Validators.required],
      maxInvoices: [50, Validators.required]
    });

    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.userForm = this.fb.group({
      id: [null],
      username: ['', [Validators.required, Validators.maxLength(50)]],
      password: [''],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      roleCode: ['OPERATOR', Validators.required],
      companyId: [null, Validators.required],
      branchId: [null, Validators.required],
      status: ['ACTIVE', Validators.required]
    });

    this.announcementForm = this.fb.group({
      title: ['', Validators.required],
      message: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required]
    });
  }

  Math = Math;
  showDetailModal = signal<boolean>(false);
  detailModalTitle = signal<string>('');
  detailModalType = signal<string>('');
  detailModalData = signal<any[]>([]);
  detailModalPage = signal<number>(0);
  detailModalTotal = signal<number>(0);

  openCardDetails(type: string): void {
    this.detailModalType.set(type);
    this.detailModalPage.set(0);
    this.detailModalData.set([]);
    this.showDetailModal.set(true);
    this.loadCardDetailsData();
  }

  loadCardDetailsData(): void {
    const type = this.detailModalType();
    const page = this.detailModalPage();
    this.loading.set(true);

    if (type === 'total_clients' || type === 'active_clients' || type === 'trial_clients' || type === 'expired_clients') {
      this.detailModalTitle.set(type.replace('_', ' ').replace(/\b\w/g, c => c.toUpperCase()));
      this.platformService.getClients('', '', page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            let list = res.data.content || [];
            if (type === 'active_clients') {
              list = list.filter((c: any) => c.status === 'ACTIVE');
            } else if (type === 'trial_clients') {
              list = list.filter((c: any) => c.plan?.code === 'TRIAL');
            } else if (type === 'expired_clients') {
              list = list.filter((c: any) => c.subscriptionStatus === 'EXPIRED' || (c.subscriptionEndDate && new Date(c.subscriptionEndDate) < new Date()));
            }
            this.detailModalData.set(list);
            this.detailModalTotal.set(list.length);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'total_users') {
      this.detailModalTitle.set('Tenant Users Directory');
      this.platformService.getUsers('', page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            const list = (res.data.content || []).filter((u: any) => u.companyId !== null);
            this.detailModalData.set(list);
            this.detailModalTotal.set(res.data.totalElements);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'total_vehicles') {
      this.detailModalTitle.set('Registered Fleet Vehicles');
      this.platformService.getVehicles(page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            this.detailModalData.set(res.data.content || []);
            this.detailModalTotal.set(res.data.totalElements);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'total_trips') {
      this.detailModalTitle.set('SaaS Fleet Trips Journey Logs');
      this.platformService.getTrips(page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            this.detailModalData.set(res.data.content || []);
            this.detailModalTotal.set(res.data.totalElements);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'monthly_revenue' || type === 'yearly_revenue') {
      this.detailModalTitle.set('Subscription Invoices (Paid)');
      this.platformService.getBillingInvoices(undefined, page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            const list = (res.data.content || []).filter((i: any) => i.status === 'PAID');
            this.detailModalData.set(list);
            this.detailModalTotal.set(list.length);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'today_login') {
      this.detailModalTitle.set("Today's Tenant Session Logins");
      this.platformService.getLoginHistory('', 'SUCCESS', page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            const todayStr = new Date().toISOString().split('T')[0];
            const list = (res.data.content || []).filter((h: any) => {
              return h.loginTime && h.loginTime.startsWith(todayStr) && h.user?.companyId !== null;
            });
            this.detailModalData.set(list);
            this.detailModalTotal.set(list.length);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'total_storage') {
      this.detailModalTitle.set('SaaS Data Storage Breakdown');
      this.platformService.getClients('', '', page, 200).subscribe({
        next: (res) => {
          if (res.success) {
            const list = res.data.content || [];
            this.detailModalData.set(list);
            this.detailModalTotal.set(list.length);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else if (type === 'total_apicalls') {
      this.detailModalTitle.set('Platform API Audit Log Stream');
      this.platformService.getAuditLogs(page, 20).subscribe({
        next: (res) => {
          if (res.success) {
            this.detailModalData.set(res.data.content || []);
            this.detailModalTotal.set(res.data.totalElements);
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else {
      this.loading.set(false);
    }
  }

  closeCardDetails(): void {
    this.showDetailModal.set(false);
    this.detailModalData.set([]);
  }

  detailModalNextPage(): void {
    const total = this.detailModalTotal();
    const current = this.detailModalPage();
    if ((current + 1) * 20 < total) {
      this.detailModalPage.set(current + 1);
      this.loadCardDetailsData();
    }
  }

  detailModalPrevPage(): void {
    const current = this.detailModalPage();
    if (current > 0) {
      this.detailModalPage.set(current - 1);
      this.loadCardDetailsData();
    }
  }

  loadDashboardData(): void {
    this.loading.set(true);
    this.platformService.getStats().subscribe({
      next: (res) => {
        if (res.success) this.stats.set(res.data);
      },
      error: (e) => this.handleError(e)
    });

    this.platformService.getAnalytics().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const d = res.data;
          this.analytics.set(d);
          this.revenueGrowth.set(d.revenueGrowth || []);
          this.planDistribution.set(d.planDistribution || []);
          this.clientGrowth.set(d.clientGrowth || []);
          this.tripGrowth.set(d.tripGrowth || []);
          this.userGrowth.set(d.userGrowth || []);
          this.vehicleDistribution.set(d.vehicleDistribution || []);
          this.recentClients.set(d.recentClients || []);
          this.renewalDue.set(d.renewalDue || []);
          this.latestPayments.set(d.latestPayments || []);
          this.systemHealth.set(d.systemHealth || null);
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  /** Safe bar height for chart series (px). */
  chartBarHeight(value: number, maxValue: number, minPx = 8, maxPx = 120): number {
    const max = maxValue > 0 ? maxValue : 1;
    return Math.max(minPx, Math.round((Number(value) || 0) / max * maxPx));
  }

  maxOf(items: any[], key: string): number {
    if (!items?.length) return 1;
    return Math.max(1, ...items.map(i => Number(i?.[key]) || 0));
  }

  switchTab(tab: string): void {
    this.activeTab.set(tab);
    this.successMsg.set('');
    this.errorMsg.set('');
    
    if (tab === 'dashboard') this.loadDashboardData();
    else if (tab === 'companies') this.loadClients();
    else if (tab === 'subscriptions') this.loadSubscriptions();
    else if (tab === 'licenses') this.loadLicenses();
    else if (tab === 'users') this.loadUsersAndSessions();
    else if (tab === 'settings') this.loadSettings();
    else if (tab === 'tickets') this.loadTickets();
    else if (tab === 'announcements') this.loadAnnouncements();
    else if (tab === 'backups') this.loadBackups();
    else if (tab === 'billing') this.loadBilling();
    else if (tab === 'audit') this.loadAuditLogs();
  }

  // 2. SaaS Client Operations
  loadClients(): void {
    this.loading.set(true);
    this.platformService.getPlans(0, 1000).subscribe({
      next: (res) => {
        if (res.success) this.plans.set(res.data.content);
      }
    });
    
    this.platformService.getClients(this.companySearch(), this.clientFilterStatus(), this.companyPage(), 10).subscribe({
      next: (res) => {
        if (res.success) {
          this.companies.set(res.data.content);
          this.companyTotal.set(res.data.totalElements);
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  onClientSearch(): void {
    this.companyPage.set(0);
    this.loadClients();
  }

  viewClientDetails(client: any): void {
    this.platformService.getClientDetails(client.id).subscribe({
      next: (res) => {
        if (res.success) {
          this.selectedClient.set(res.data);
          this.showClientViewModal.set(true);
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  editClientDetails(client: any): void {
    this.isEditMode.set(true);
    this.showOnboardingWizard.set(false);
    this.companyForm.reset();
    this.companyForm.patchValue(client);
    this.showCompanyForm.set(true);
  }

  openOnboardingWizard(): void {
    this.isEditMode.set(false);
    this.showCompanyForm.set(false);
    this.onboardingResult.set(null);
    this.onboardingStep.set(1);
    this.copiedField.set('');
    this.companyForm.reset({
      storage: '10 GB',
      subscriptionStatus: 'ACTIVE',
      maxUsers: 5,
      maxVehicles: 5,
      sendWelcomeEmail: true,
      billingMonths: 1
    });
    this.showOnboardingWizard.set(true);
    if (!this.plans().length) {
      this.platformService.getPlans(0, 1000).subscribe({
        next: (res) => {
          if (res.success) this.plans.set(res.data.content);
        }
      });
    }
  }

  closeOnboardingWizard(): void {
    this.showOnboardingWizard.set(false);
    this.onboardingStep.set(1);
    this.onboardingResult.set(null);
    this.errorMsg.set('');
    this.companyForm.reset({ storage: '10 GB', sendWelcomeEmail: true, maxUsers: 5, maxVehicles: 5, billingMonths: 1, subscriptionStatus: 'ACTIVE' });
  }

  onboardingNext(): void {
    const step = this.onboardingStep();
    if (step === 1 && this.companyDetailsInvalid()) return;
    if (step === 3 && !this.companyForm.value.subscriptionPlanId) {
      this.companyForm.get('subscriptionPlanId')?.markAsTouched();
      this.errorMsg.set('Please select a Subscription Plan before continuing.');
      return;
    }
    this.errorMsg.set('');
    if (step < 5) this.onboardingStep.set(step + 1);
  }

  onboardingBack(): void {
    const step = this.onboardingStep();
    if (step > 1 && step < 5) this.onboardingStep.set(step - 1);
  }

  /** Marks required company fields and returns true when the step cannot continue. */
  companyDetailsInvalid(): boolean {
    const fieldLabels: Record<string, string> = {
      name: 'Company Name',
      ownerName: 'Owner / CEO Name',
      businessType: 'Business Type',
      phone: 'Phone',
      email: 'Email',
      gstNumber: 'GSTIN',
      address: 'Address',
      city: 'City',
      state: 'State',
      pincode: 'Pincode'
    };
    const missing: string[] = [];
    const formatIssues: string[] = [];

    Object.keys(fieldLabels).forEach(k => {
      const c = this.companyForm.get(k);
      if (!c) return;
      c.markAsTouched();
      c.updateValueAndValidity({ emitEvent: true });
      if (c.hasError('required')) {
        missing.push(fieldLabels[k]);
      } else if (c.invalid) {
        if (c.hasError('email')) formatIssues.push(`${fieldLabels[k]} (enter a valid email)`);
        else if (c.hasError('pattern')) formatIssues.push(`${fieldLabels[k]} (invalid format)`);
        else formatIssues.push(fieldLabels[k]);
      }
    });

    if (missing.length || formatIssues.length) {
      const parts: string[] = [];
      if (missing.length) {
        parts.push(`Please fill required fields: ${missing.join(', ')}`);
      }
      if (formatIssues.length) {
        parts.push(`Please correct: ${formatIssues.join(', ')}`);
      }
      this.errorMsg.set(parts.join('. ') + '.');
      return true;
    }

    this.errorMsg.set('');
    return false;
  }

  previewCompanyCode(): string {
    const name = (this.companyForm.value.name || this.companyForm.value.ownerName || 'AKS').toString();
    let prefix = name.replace(/[^a-zA-Z]/g, '').toUpperCase();
    if (prefix.length < 3) prefix = (prefix + 'XYZ').substring(0, 3);
    else prefix = prefix.substring(0, 3);
    return prefix + '###';
  }

  selectedPlanLabel(): string {
    const id = this.companyForm.value.subscriptionPlanId;
    const plan = this.plans().find((p: any) => String(p.id) === String(id));
    return plan ? `${plan.name} (₹${plan.price})` : '—';
  }

  runOnboarding(): void {
    if (this.companyDetailsInvalid() || !this.companyForm.value.subscriptionPlanId) {
      this.errorMsg.set('Company details and subscription plan are required');
      return;
    }
    this.onboardingBusy.set(true);
    this.loading.set(true);
    this.errorMsg.set('');

    const v = this.companyForm.value;
    const payload = {
      name: v.name,
      ownerName: v.ownerName,
      businessType: v.businessType,
      phone: v.phone,
      email: v.email,
      gstNumber: v.gstNumber,
      panNumber: v.panNumber,
      address: v.address,
      city: v.city,
      state: v.state,
      pincode: v.pincode,
      website: v.website,
      logo: v.logo,
      storage: v.storage || '10 GB',
      subscriptionPlanId: Number(v.subscriptionPlanId),
      subscriptionStartDate: v.subscriptionStartDate || null,
      subscriptionEndDate: v.subscriptionEndDate || null,
      billingMonths: v.billingMonths || 1,
      maxUsers: v.maxUsers || null,
      maxVehicles: v.maxVehicles || null,
      sendWelcomeEmail: v.sendWelcomeEmail !== false,
      adminUsername: v.adminUsername || null,
      adminPassword: v.adminPassword || null
    };

    this.platformService.onboardClient(payload).subscribe({
      next: (res) => {
        if (res.success) {
          this.onboardingResult.set(res.data);
          this.onboardingStep.set(5);
          this.showSuccess('Client onboarding complete — Company Admin can login immediately');
          this.loadClients();
        }
        this.onboardingBusy.set(false);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.onboardingBusy.set(false);
        this.loading.set(false);
      }
    });
  }

  copyCredential(value: string, field: string): void {
    if (!value) return;
    navigator.clipboard?.writeText(value).then(() => {
      this.copiedField.set(field);
      setTimeout(() => this.copiedField.set(''), 2000);
    }).catch(() => {
      this.errorMsg.set('Could not copy to clipboard');
    });
  }

  finishOnboarding(): void {
    this.closeOnboardingWizard();
  }

  deleteClient(client: any): void {
    if (confirm(`Are you sure you want to permanently delete SaaS Client "${client.name}"? This soft-deletes the tenant data.`)) {
      this.loading.set(true);
      this.platformService.deleteCompany(client.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess('SaaS Client soft-deleted successfully');
            this.loadClients();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  toggleCompanyStatus(company: any): void {
    const newStatus = company.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.platformService.updateCompanyStatus(company.id, newStatus).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess(`Company status updated to ${newStatus}`);
          this.loadClients();
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  triggerSeedDemoData(company: any): void {
    if (confirm(`Are you sure you want to seed full flow ERP demonstration transactions for SaaS Client "${company.name}"? This populates Drivers, Vehicles, Bookings, Trips, Fuel, Expenses, and Invoices automatically.`)) {
      this.loading.set(true);
      this.platformService.seedDemoData(company.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess(res.message || 'Demo transactions generated successfully!');
            this.loadClients();
            this.loadDashboardData();
          } else {
            this.errorMsg.set(res.message || 'Data generation failed.');
            setTimeout(() => this.errorMsg.set(''), 5500);
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  saveCompany(): void {
    if (this.companyForm.invalid) return;
    this.loading.set(true);

    const formVal = { ...this.companyForm.value };
    if (formVal.subscriptionPlanId) {
      formVal.subscriptionPlan = { id: formVal.subscriptionPlanId };
    }

    if (this.isEditMode() && formVal.id) {
      this.platformService.updateCompany(formVal.id, formVal).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess('Client profile details updated successfully');
            this.companyForm.reset();
            this.showCompanyForm.set(false);
            this.isEditMode.set(false);
            this.loadClients();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    } else {
      this.platformService.createCompany(formVal).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess('Tenant registered and automated B2B provisioning complete (branch, admin user, settings, financial year, lookups, subscription & license keys seeded)!');
            this.companyForm.reset();
            this.showCompanyForm.set(false);
            this.loadClients();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  exportClientsCsv(): void {
    this.loading.set(true);
    this.platformService.getClients('', '', 0, 1000).subscribe({
      next: (res) => {
        if (res.success && res.data.content) {
          const list = res.data.content;
          const headers = [
            'Company Name', 'Company Code', 'Owner Name', 'Business Type',
            'Phone', 'Email', 'GST', 'Address', 'City', 'State', 'Pincode',
            'Status', 'Plan', 'License Count', 'Storage', 'Created Date'
          ];
          
          let csvContent = headers.join(',') + '\n';
          
          list.forEach((item: any) => {
            const row = [
              `"${item.name || ''}"`,
              `"${item.code || ''}"`,
              `"${item.ownerName || ''}"`,
              `"${item.businessType || ''}"`,
              `"${item.phone || ''}"`,
              `"${item.email || ''}"`,
              `"${item.gstNumber || ''}"`,
              `"${(item.address || '').replace(/"/g, '""')}"`,
              `"${item.city || ''}"`,
              `"${item.state || ''}"`,
              `"${item.pincode || ''}"`,
              `"${item.status || ''}"`,
              `"${item.planName || ''}"`,
              item.licenseCount || 0,
              `"${item.storage || ''}"`,
              item.createdDate || ''
            ];
            csvContent += row.join(',') + '\n';
          });
          
          const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.setAttribute('href', url);
          link.setAttribute('download', `saas_clients_report_${Date.now()}.csv`);
          link.style.visibility = 'hidden';
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  // 4. Plan & Subscriptions
  loadSubscriptions(): void {
    this.loading.set(true);
    this.platformService.getPlans().subscribe({
      next: (res) => {
        if (res.success) this.plans.set(res.data.content);
      },
      error: (e) => this.handleError(e)
    });

    this.platformService.getTenantSubscriptions().subscribe({
      next: (res) => {
        if (res.success) this.tenantSubscriptions.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  savePlan(): void {
    if (this.planForm.invalid) return;
    this.loading.set(true);
    const apiCall = this.editingPlan()
      ? this.platformService.updatePlan(this.editingPlan()!.id!, this.planForm.value)
      : this.platformService.createPlan(this.planForm.value);

    apiCall.subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess(this.editingPlan() ? 'Plan details updated' : 'Subscription plan created');
          this.planForm.reset();
          this.showPlanForm.set(false);
          this.editingPlan.set(null);
          this.loadSubscriptions();
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  editPlan(plan: SaaSPlan): void {
    this.editingPlan.set(plan);
    this.planForm.patchValue(plan);
    this.showPlanForm.set(true);
  }

  openCreatePlanForm(): void {
    this.editingPlan.set(null);
    this.planForm.reset({
      billingPeriod: 'MONTHLY',
      price: 0,
      maxUsers: 5,
      maxVehicles: 5,
      maxInvoices: 50
    });
    this.showPlanForm.set(true);
  }

  // 5. Licenses
  loadLicenses(): void {
    this.loading.set(true);
    this.platformService.getLicenses().subscribe({
      next: (res) => {
        if (res.success) this.licenses.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  revokeLicense(id: number): void {
    this.platformService.revokeLicense(id).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('Tenant license key revoked successfully.');
          this.loadLicenses();
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  // 6. Users & Sessions Management
  roleOptions = ['SUPER_ADMIN', 'COMPANY_ADMIN', 'MANAGER', 'OPERATOR', 'ACCOUNTANT', 'DRIVER', 'VIEWER'];
  companiesList = signal<any[]>([]);

  loadUsersAndSessions(): void {
    this.loading.set(true);
    
    // Load Users
    this.platformService.getUsers(this.userSearch()).subscribe({
      next: (res) => {
        if (res.success) this.users.set(res.data.content);
      },
      error: (e) => this.handleError(e)
    });

    // Load Active Sessions
    this.platformService.getActiveSessionsPage(0, 100).subscribe({
      next: (res) => {
        if (res.success) this.activeSessions.set(res.data.content || []);
      },
      error: (e) => this.handleError(e)
    });

    // Load Login History
    this.platformService.getLoginHistory('', '', 0, 100).subscribe({
      next: (res) => {
        if (res.success) this.loginHistory.set(res.data.content || []);
      },
      error: (e) => this.handleError(e)
    });

    // Load Logout History
    this.platformService.getLogoutHistory(0, 100).subscribe({
      next: (res) => {
        if (res.success) this.logoutHistory.set(res.data.content || []);
      },
      error: (e) => this.handleError(e)
    });

    // Load Failed Logins
    this.platformService.getFailedLoginAttempts(0, 100).subscribe({
      next: (res) => {
        if (res.success) this.failedLogins.set(res.data.content || []);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });

    // Load Companies for user selection form
    this.platformService.getCompanies('', 'ACTIVE', 0, 1000).subscribe({
      next: (res) => {
        if (res.success) this.companiesList.set(res.data.content || []);
      }
    });
  }

  onUserCompanyChange(companyId: any): void {
    const selected = this.companiesList().find(c => c.id == companyId);
    if (selected) {
      this.userForm.patchValue({
        branchId: selected.branchId
      });
    }
  }

  openCreateUserForm(): void {
    this.isUserEditMode.set(false);
    this.editingUser.set(null);
    this.userForm.reset({
      status: 'ACTIVE',
      roleCode: 'OPERATOR'
    });
    this.showUserForm.set(true);
  }

  openEditUserForm(user: any): void {
    this.isUserEditMode.set(true);
    this.editingUser.set(user);
    this.userForm.reset();
    
    // Map role
    const roleCode = user.roles && user.roles.length > 0 ? user.roles[0].code : 'OPERATOR';
    
    this.userForm.patchValue({
      id: user.id,
      username: user.username,
      name: user.name,
      email: user.email,
      phone: user.phone,
      roleCode: roleCode,
      companyId: user.companyId,
      branchId: user.branchId,
      status: user.status
    });
    this.showUserForm.set(true);
  }

  saveUser(): void {
    if (this.userForm.invalid) return;
    this.loading.set(true);

    const payload = this.userForm.value;
    const roleCode = payload.roleCode;

    if (this.isUserEditMode() && payload.id) {
      this.platformService.updateUser(payload.id, payload, roleCode).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess('User details updated successfully!');
            this.showUserForm.set(false);
            this.loadUsersAndSessions();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    } else {
      this.platformService.createUser(payload, roleCode).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess('User registered successfully!');
            this.showUserForm.set(false);
            this.loadUsersAndSessions();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  toggleUserLock(user: any): void {
    const newStatus = user.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED';
    this.platformService.updateUserLockStatus(user.id, newStatus).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess(`User lock status changed to ${newStatus}`);
          this.loadUsersAndSessions();
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  toggleUserDisable(user: any): void {
    const newStatus = user.status === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE';
    this.platformService.updateUserLockStatus(user.id, newStatus).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess(`User marked as ${newStatus === 'ACTIVE' ? 'ENABLED' : 'DISABLED'}`);
          this.loadUsersAndSessions();
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  expirePassword(user: any): void {
    if (confirm(`Are you sure you want to expire password for user "${user.username}"? They will need to reset it.`)) {
      this.loading.set(true);
      this.platformService.expireUserPassword(user.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess(`Forced password expiry for user "${user.username}" successfully`);
            this.loadUsersAndSessions();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  forcePasswordChange(user: any): void {
    if (confirm(`Are you sure you want to force password change for user "${user.username}" on their next login?`)) {
      this.loading.set(true);
      this.platformService.forceUserPasswordChange(user.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess(`Force password change flag enabled for user "${user.username}"`);
            this.loadUsersAndSessions();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  terminateSession(session: any): void {
    if (confirm(`Force terminate active session for user "${session.username}"?`)) {
      this.loading.set(true);
      this.platformService.forceLogoutSession(session.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.showSuccess('Active session terminated successfully');
            this.loadUsersAndSessions();
          }
          this.loading.set(false);
        },
        error: (e) => {
          this.handleError(e);
          this.loading.set(false);
        }
      });
    }
  }

  triggerPasswordReset(userId: number): void {
    this.selectedUserId.set(userId);
    this.passwordForm.reset();
    this.showPasswordResetModal.set(true);
  }

  submitPasswordReset(): void {
    if (this.passwordForm.invalid || !this.selectedUserId()) return;
    this.loading.set(true);
    this.platformService.resetUserPassword(this.selectedUserId()!, this.passwordForm.value).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('User password reset successfully');
          this.showPasswordResetModal.set(false);
          this.selectedUserId.set(null);
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  // 8. Settings
  loadSettings(): void {
    this.loading.set(true);
    this.platformService.getSystemSettings().subscribe({
      next: (res) => {
        if (res.success) this.systemSettings.set(res.data);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  saveSetting(setting: any, newValue: string): void {
    if (!newValue.trim()) return;
    this.platformService.updateSystemSetting(setting.keyName, newValue).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess(`Updated setting ${setting.keyName}`);
          this.loadSettings();
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  // 9. Audit Logs
  loadAuditLogs(): void {
    this.loading.set(true);
    this.platformService.getAuditLogs().subscribe({
      next: (res) => {
        if (res.success) {
          this.stats.set({ ...this.stats()!, totalAuditLogs: res.data.totalElements });
          this.auditLogsList.set(res.data.content || []);
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  // 10. Backups
  loadBackups(): void {
    this.loading.set(true);
    this.platformService.getBackups().subscribe({
      next: (res) => {
        if (res.success) this.backups.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  runManualBackup(): void {
    this.loading.set(true);
    this.platformService.triggerBackup().subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('Database snapshot manual backup successfully created!');
          this.loadBackups();
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  // 11. Tickets
  loadTickets(): void {
    this.loading.set(true);
    this.platformService.getSupportTickets().subscribe({
      next: (res) => {
        if (res.success) this.tickets.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  viewTicket(ticketId: number): void {
    this.loading.set(true);
    this.platformService.getSupportTicket(ticketId).subscribe({
      next: (res) => {
        if (res.success) {
          this.selectedTicket.set(res.data);
          this.replyMessage.set('');
          this.showTicketThread.set(true);
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  submitTicketReply(): void {
    if (!this.replyMessage().trim() || !this.selectedTicket()) return;
    this.loading.set(true);
    const reply: SupportReply = {
      message: this.replyMessage(),
      isAdminReply: true,
      username: 'superadmin'
    };

    this.platformService.createSupportReply(this.selectedTicket()!.id!, reply).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('Staff reply posted successfully');
          this.viewTicket(this.selectedTicket()!.id!);
          this.loadTickets();
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  closeTicket(ticket: SupportTicket): void {
    this.platformService.updateTicketStatus(ticket.id!, 'CLOSED').subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('Ticket status updated to CLOSED');
          this.loadTickets();
          if (this.selectedTicket()?.id === ticket.id) {
            this.showTicketThread.set(false);
          }
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  // 12. Announcements
  loadAnnouncements(): void {
    this.loading.set(true);
    this.platformService.getAnnouncements().subscribe({
      next: (res) => {
        if (res.success) this.announcements.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  saveAnnouncement(): void {
    if (this.announcementForm.invalid) return;
    this.loading.set(true);
    this.platformService.createAnnouncement(this.announcementForm.value).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('Global system announcement broadcasted successfully!');
          this.announcementForm.reset();
          this.showAnnouncementForm.set(false);
          this.loadAnnouncements();
        }
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  deleteAnnouncement(id: number): void {
    this.platformService.deleteAnnouncement(id).subscribe({
      next: (res) => {
        if (res.success) {
          this.showSuccess('Broadcast announcement deleted successfully.');
          this.loadAnnouncements();
        }
      },
      error: (e) => this.handleError(e)
    });
  }

  // 14. Billing
  loadBilling(): void {
    this.loading.set(true);
    this.platformService.getBillingInvoices().subscribe({
      next: (res) => {
        if (res.success) this.invoices.set(res.data.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.handleError(e);
        this.loading.set(false);
      }
    });
  }

  // 9. Global System Audit Log
  loadAuditLogsList(): void {
    this.loadAuditLogs();
  }

  // Utils
  private showSuccess(msg: string): void {
    this.successMsg.set(msg);
    this.errorMsg.set('');
    setTimeout(() => this.successMsg.set(''), 4500);
  }

  private handleError(err: any): void {
    const defaultMsg = 'An error occurred during platform administration operations.';
    this.errorMsg.set(err?.error?.message || err?.message || defaultMsg);
    this.successMsg.set('');
    setTimeout(() => this.errorMsg.set(''), 5500);
  }
}
