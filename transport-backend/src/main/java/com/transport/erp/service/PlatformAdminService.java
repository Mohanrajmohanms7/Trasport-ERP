package com.transport.erp.service;

import com.transport.erp.dto.ClientOnboardingRequest;
import com.transport.erp.dto.ClientOnboardingResult;
import com.transport.erp.dto.PlatformAdminStatsDTO;
import com.transport.erp.dto.SaaSClientDTO;
import com.transport.erp.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

public interface PlatformAdminService {
    
    // 1. Dashboard & Analytics
    PlatformAdminStatsDTO getSystemStats();
    Map<String, Object> getAnalytics();

    // 2. Client / Company Management
    Page<Company> getCompanies(String search, String status, Pageable pageable);
    Page<SaaSClientDTO> getClients(String search, String status, Pageable pageable);
    SaaSClientDTO getClientDetails(Long id);
    /** Phase 29 – complete client onboarding (company → HO → admin → subscription → masters → ready). */
    ClientOnboardingResult onboardClient(ClientOnboardingRequest request, String activeUser);
    Company createCompany(Company company, String activeUser);
    Company updateCompany(Long id, Company company, String activeUser);
    Company updateCompanyStatus(Long id, String status, String activeUser);
    void deleteCompany(Long id, String activeUser);

    // 4. Subscription Management
    Page<SaaSPlan> getPlans(Pageable pageable);
    SaaSPlan createPlan(SaaSPlan plan, String activeUser);
    SaaSPlan updatePlan(Long id, SaaSPlan plan, String activeUser);
    SaaSTenantSubscription createTenantSubscription(SaaSTenantSubscription sub, String activeUser);
    Page<SaaSTenantSubscription> getTenantSubscriptions(Long companyId, Pageable pageable);

    // 5. License Management
    Page<SaaSLicense> getLicenses(Long companyId, Pageable pageable);
    SaaSLicense createLicense(SaaSLicense license, String activeUser);
    SaaSLicense revokeLicense(Long id, String activeUser);

    // 6. User Management
    Page<AppUser> getAllUsers(String search, Pageable pageable);
    AppUser createUser(AppUser user, String roleCode, String activeUser);
    AppUser updateUser(Long id, AppUser userDetails, String roleCode, String activeUser);
    AppUser updateUserLockStatus(Long id, String status, String activeUser);
    void resetUserPassword(Long id, String newPassword, String activeUser);
    AppUser expireUserPassword(Long id, String activeUser);
    AppUser forceUserPasswordChange(Long id, String activeUser);

    // 7. Authentication Management (Sessions & Audits)
    Page<LoginHistory> getLoginHistory(String search, String status, Pageable pageable);
    Page<LoginHistory> getLogoutHistory(Pageable pageable);
    Page<LoginHistory> getActiveSessions(Pageable pageable);
    Page<LoginHistory> getFailedLoginAttempts(Pageable pageable);
    void forceLogoutSession(Long loginHistoryId, String activeUser);

    // 8. System Settings
    List<SaaSSystemSetting> getSystemSettings();
    SaaSSystemSetting updateSystemSetting(String key, String value, String activeUser);

    // 9. Audit Logs
    Page<AuditLog> getSystemAuditLogs(Pageable pageable);

    // 10. Backup & Restore
    Page<SaaSBackup> getBackups(Pageable pageable);
    SaaSBackup triggerBackup(String activeUser);

    // 11. Support Tickets
    Page<SaaSSupportTicket> getSupportTickets(String status, Pageable pageable);
    SaaSSupportTicket getSupportTicket(Long id);
    SaaSSupportReply createSupportReply(Long ticketId, SaaSSupportReply reply, String activeUser);
    SaaSSupportTicket updateTicketStatus(Long id, String status, String activeUser);

    // 12. Announcements
    Page<SaaSAnnouncement> getAnnouncements(Pageable pageable);
    SaaSAnnouncement createAnnouncement(SaaSAnnouncement announcement, String activeUser);
    void deleteAnnouncement(Long id, String activeUser);

    // 14. Billing Invoices
    Page<SaaSBillingInvoice> getBillingInvoices(Long companyId, Pageable pageable);

    Page<Vehicle> getVehicles(Pageable pageable);
    Page<Trip> getTrips(Pageable pageable);
}

