import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PlatformStats {
  totalCompanies: number;
  activeCompanies: number;
  totalUsers: number;
  totalVehicles: number;
  totalTrips: number;
  totalAuditLogs: number;
  activeLicenses: number;
  openSupportTickets: number;
  monthToDateRevenue: number;
}

export interface SaaSPlan {
  id?: number;
  code: string;
  name: string;
  description?: string;
  price: number;
  billingPeriod: string;
  maxUsers: number;
  maxVehicles: number;
  maxInvoices: number;
  status: string;
}

export interface TenantSubscription {
  id?: number;
  companyId: number;
  plan: SaaSPlan;
  status: string;
  startDate: string;
  endDate: string;
  amountPaid: number;
  paymentMethod: string;
  paymentStatus: string;
}

export interface SaaSLicense {
  id?: number;
  companyId: number;
  licenseKey: string;
  status: string;
  activationDate: string;
  expiryDate: string;
  maxUsers: number;
  maxVehicles: number;
}

export interface SupportTicket {
  id?: number;
  companyId: number;
  username: string;
  ticketNumber: string;
  subject: string;
  description: string;
  priority: string;
  status: string;
  createdDate: string;
  updatedDate: string;
  replies?: SupportReply[];
}

export interface SupportReply {
  id?: number;
  username: string;
  message: string;
  isAdminReply: boolean;
  createdDate?: string;
}

export interface Announcement {
  id?: number;
  title: string;
  message: string;
  startDate: string;
  endDate: string;
  status: string;
}

export interface BackupLog {
  id?: number;
  filename: string;
  fileSize: string;
  status: string;
  backupDate: string;
  triggerType: string;
  createdBy: string;
}

export interface BillingInvoice {
  id?: number;
  companyId: number;
  invoiceNumber: string;
  invoiceDate: string;
  amount: number;
  status: string;
  paymentMethod: string;
  transactionReference?: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlatformAdminService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/platform-admin';

  getStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/stats`);
  }

  getAnalytics(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/analytics`);
  }

  getCompanies(search?: string, status?: string, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);
    return this.http.get<any>(`${this.baseUrl}/companies`, { params });
  }

  getClients(search?: string, status?: string, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);
    return this.http.get<any>(`${this.baseUrl}/clients`, { params });
  }

  getClientDetails(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/clients/${id}`);
  }

  createCompany(company: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/companies`, company);
  }

  /** Phase 29 – complete client onboarding (returns credentials + step checklist). */
  onboardClient(payload: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/onboard`, payload);
  }

  updateCompany(id: number, company: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/companies/${id}`, company);
  }

  updateCompanyStatus(id: number, status: string): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/companies/${id}/status?status=${status}`, {});
  }

  deleteCompany(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/companies/${id}`);
  }

  seedDemoData(id: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/companies/${id}/seed-demo-data`, {});
  }

  getPlans(page = 0, size = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/plans`, { params });
  }

  createPlan(plan: SaaSPlan): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/plans`, plan);
  }

  updatePlan(id: number, plan: SaaSPlan): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/plans/${id}`, plan);
  }

  createTenantSubscription(sub: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/tenant-subscriptions`, sub);
  }

  getTenantSubscriptions(companyId?: number, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (companyId) params = params.set('companyId', companyId.toString());
    return this.http.get<any>(`${this.baseUrl}/tenant-subscriptions`, { params });
  }

  getLicenses(companyId?: number, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (companyId) params = params.set('companyId', companyId.toString());
    return this.http.get<any>(`${this.baseUrl}/licenses`, { params });
  }

  createLicense(license: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/licenses`, license);
  }

  revokeLicense(id: number): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/licenses/${id}/revoke`, {});
  }

  getUsers(search?: string, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) params = params.set('search', search);
    return this.http.get<any>(`${this.baseUrl}/users`, { params });
  }

  createUser(user: any, roleCode: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/users?roleCode=${roleCode}`, user);
  }

  updateUser(id: number, user: any, roleCode: string): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/users/${id}?roleCode=${roleCode}`, user);
  }

  updateUserLockStatus(id: number, status: string): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/users/${id}/status?status=${status}`, {});
  }

  resetUserPassword(id: number, payload: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/users/${id}/reset-password`, payload);
  }

  expireUserPassword(id: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/users/${id}/expire-password`, {});
  }

  forceUserPasswordChange(id: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/users/${id}/force-password-change`, {});
  }

  getLoginHistory(search?: string, status?: string, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);
    return this.http.get<any>(`${this.baseUrl}/auth/login-history`, { params });
  }

  getLogoutHistory(page = 0, size = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/auth/logout-history`, { params });
  }

  getActiveSessionsPage(page = 0, size = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/auth/active-sessions`, { params });
  }

  getFailedLoginAttempts(page = 0, size = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/auth/failed-logins`, { params });
  }

  forceLogoutSession(id: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/sessions/${id}/logout`, {});
  }

  getSystemSettings(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/settings`);
  }

  updateSystemSetting(key: string, value: string): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/settings`, { key, value });
  }

  getAuditLogs(page = 0, size = 15): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/audit-logs`, { params });
  }

  getBackups(page = 0, size = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/backups`, { params });
  }

  triggerBackup(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/backups`, {});
  }

  getSupportTickets(status?: string, page = 0, size = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) params = params.set('status', status);
    return this.http.get<any>(`${this.baseUrl}/tickets`, { params });
  }

  getSupportTicket(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/tickets/${id}`);
  }

  createSupportReply(id: number, reply: SupportReply): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/tickets/${id}/replies`, reply);
  }

  updateTicketStatus(id: number, status: string): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/tickets/${id}/status?status=${status}`, {});
  }

  getAnnouncements(page = 0, size = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/announcements`, { params });
  }

  createAnnouncement(announcement: Announcement): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/announcements`, announcement);
  }

  deleteAnnouncement(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/announcements/${id}`);
  }

  getBillingInvoices(companyId?: number, page = 0, size = 15): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (companyId) params = params.set('companyId', companyId.toString());
    return this.http.get<any>(`${this.baseUrl}/billing-invoices`, { params });
  }

  getVehicles(page = 0, size = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/vehicles`, { params });
  }

  getTrips(page = 0, size = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/trips`, { params });
  }
}
