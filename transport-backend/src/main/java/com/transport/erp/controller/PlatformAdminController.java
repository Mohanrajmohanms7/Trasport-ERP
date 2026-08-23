package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.ClientOnboardingRequest;
import com.transport.erp.dto.ClientOnboardingResult;
import com.transport.erp.dto.PlatformAdminStatsDTO;
import com.transport.erp.dto.SaaSClientDTO;
import com.transport.erp.model.*;
import com.transport.erp.service.PlatformAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform-admin")
@CrossOrigin(origins = "*")
public class PlatformAdminController {

    @Autowired
    private PlatformAdminService platformAdminService;

    private String getActiveUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // 1. Dashboard & Analytics
    @GetMapping("/stats")
    public ApiResponse<PlatformAdminStatsDTO> getStats() {
        PlatformAdminStatsDTO stats = platformAdminService.getSystemStats();
        return ApiResponse.success(stats, "Platform system metrics fetched successfully");
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> getAnalytics() {
        Map<String, Object> data = platformAdminService.getAnalytics();
        return ApiResponse.success(data, "System analytics chart details fetched successfully");
    }

    // 2. Client / Company Management
    @GetMapping("/companies")
    public ApiResponse<Page<Company>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Company> data = platformAdminService.getCompanies(search, status, pageable);
        return ApiResponse.success(data, "Tenant companies directory fetched successfully");
    }

    @GetMapping("/clients")
    public ApiResponse<Page<SaaSClientDTO>> getClients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSClientDTO> data = platformAdminService.getClients(search, status, pageable);
        return ApiResponse.success(data, "SaaS clients directory fetched successfully");
    }

    @GetMapping("/clients/{id}")
    public ApiResponse<SaaSClientDTO> getClientDetails(@PathVariable Long id) {
        SaaSClientDTO client = platformAdminService.getClientDetails(id);
        return ApiResponse.success(client, "SaaS client profile details fetched successfully");
    }

    @PostMapping("/onboard")
    public ApiResponse<ClientOnboardingResult> onboardClient(@RequestBody ClientOnboardingRequest request) {
        ClientOnboardingResult result = platformAdminService.onboardClient(request, getActiveUser());
        return ApiResponse.success(result, "Client onboarding completed — company admin can login immediately");
    }

    @PostMapping("/companies")
    public ApiResponse<Company> createCompany(@RequestBody Company company) {
        Company created = platformAdminService.createCompany(company, getActiveUser());
        return ApiResponse.success(created, "Tenant company registered and provisioned successfully");
    }

    @PutMapping("/companies/{id}")
    public ApiResponse<Company> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        Company updated = platformAdminService.updateCompany(id, company, getActiveUser());
        return ApiResponse.success(updated, "Client details updated successfully");
    }

    @PutMapping("/companies/{id}/status")
    public ApiResponse<Company> updateCompanyStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Company updated = platformAdminService.updateCompanyStatus(id, status, getActiveUser());
        return ApiResponse.success(updated, "Tenant company status toggled successfully");
    }

    @DeleteMapping("/companies/{id}")
    public ApiResponse<Void> deleteCompany(@PathVariable Long id) {
        platformAdminService.deleteCompany(id, getActiveUser());
        return ApiResponse.success(null, "Tenant company soft deleted successfully");
    }

    @PostMapping("/companies/{id}/seed-demo-data")
    public ApiResponse<Map<String, Object>> seedDemoData(@PathVariable Long id) {
        Map<String, Object> result = platformAdminService.seedFullDemoData(id);
        return ApiResponse.success(result, "Full flow demo data generated successfully for tenant client");
    }

    // 4. Subscription Plans Management
    @GetMapping("/plans")
    public ApiResponse<Page<SaaSPlan>> getPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<SaaSPlan> data = platformAdminService.getPlans(pageable);
        return ApiResponse.success(data, "SaaS subscription plans fetched successfully");
    }

    @PostMapping("/plans")
    public ApiResponse<SaaSPlan> createPlan(@RequestBody SaaSPlan plan) {
        SaaSPlan created = platformAdminService.createPlan(plan, getActiveUser());
        return ApiResponse.success(created, "SaaS plan created successfully");
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<SaaSPlan> updatePlan(
            @PathVariable Long id,
            @RequestBody SaaSPlan plan) {
        SaaSPlan updated = platformAdminService.updatePlan(id, plan, getActiveUser());
        return ApiResponse.success(updated, "SaaS plan details updated successfully");
    }

    @PostMapping("/tenant-subscriptions")
    public ApiResponse<SaaSTenantSubscription> createTenantSubscription(@RequestBody SaaSTenantSubscription sub) {
        SaaSTenantSubscription created = platformAdminService.createTenantSubscription(sub, getActiveUser());
        return ApiResponse.success(created, "Tenant plan subscription provisioned successfully");
    }

    @GetMapping("/tenant-subscriptions")
    public ApiResponse<Page<SaaSTenantSubscription>> getTenantSubscriptions(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSTenantSubscription> data = platformAdminService.getTenantSubscriptions(companyId, pageable);
        return ApiResponse.success(data, "Tenant plan subscriptions history fetched successfully");
    }

    // 5. License Management
    @GetMapping("/licenses")
    public ApiResponse<Page<SaaSLicense>> getLicenses(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSLicense> data = platformAdminService.getLicenses(companyId, pageable);
        return ApiResponse.success(data, "System license registries fetched successfully");
    }

    @PostMapping("/licenses")
    public ApiResponse<SaaSLicense> createLicense(@RequestBody SaaSLicense license) {
        SaaSLicense created = platformAdminService.createLicense(license, getActiveUser());
        return ApiResponse.success(created, "License key registry completed successfully");
    }

    @PutMapping("/licenses/{id}/revoke")
    public ApiResponse<SaaSLicense> revokeLicense(@PathVariable Long id) {
        SaaSLicense revoked = platformAdminService.revokeLicense(id, getActiveUser());
        return ApiResponse.success(revoked, "Tenant license revoked successfully");
    }

    // 6. User Management
    @GetMapping("/users")
    public ApiResponse<Page<AppUser>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<AppUser> data = platformAdminService.getAllUsers(search, pageable);
        return ApiResponse.success(data, "Global system users directory fetched successfully");
    }

    @PostMapping("/users")
    public ApiResponse<AppUser> createUser(
            @RequestBody AppUser user,
            @RequestParam String roleCode) {
        AppUser created = platformAdminService.createUser(user, roleCode, getActiveUser());
        return ApiResponse.success(created, "User registered successfully");
    }

    @PutMapping("/users/{id}")
    public ApiResponse<AppUser> updateUser(
            @PathVariable Long id,
            @RequestBody AppUser user,
            @RequestParam(required = false) String roleCode) {
        AppUser updated = platformAdminService.updateUser(id, user, roleCode, getActiveUser());
        return ApiResponse.success(updated, "User details updated successfully");
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<AppUser> updateUserLockStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        AppUser updated = platformAdminService.updateUserLockStatus(id, status, getActiveUser());
        return ApiResponse.success(updated, "User lock status updated successfully");
    }

    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetUserPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        platformAdminService.resetUserPassword(id, newPassword, getActiveUser());
        return ApiResponse.success(null, "User password reset completed successfully");
    }

    @PostMapping("/users/{id}/expire-password")
    public ApiResponse<AppUser> expireUserPassword(@PathVariable Long id) {
        AppUser updated = platformAdminService.expireUserPassword(id, getActiveUser());
        return ApiResponse.success(updated, "User password marked as expired successfully");
    }

    @PostMapping("/users/{id}/force-password-change")
    public ApiResponse<AppUser> forceUserPasswordChange(@PathVariable Long id) {
        AppUser updated = platformAdminService.forceUserPasswordChange(id, getActiveUser());
        return ApiResponse.success(updated, "Force password change enabled successfully");
    }

    // 7. Authentication Management (Sessions & Audits)
    @GetMapping("/auth/login-history")
    public ApiResponse<Page<LoginHistory>> getLoginHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<LoginHistory> data = platformAdminService.getLoginHistory(search, status, pageable);
        return ApiResponse.success(data, "Login history records list fetched successfully");
    }

    @GetMapping("/auth/logout-history")
    public ApiResponse<Page<LoginHistory>> getLogoutHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<LoginHistory> data = platformAdminService.getLogoutHistory(pageable);
        return ApiResponse.success(data, "Logout history records list fetched successfully");
    }

    @GetMapping("/auth/active-sessions")
    public ApiResponse<Page<LoginHistory>> getActiveSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<LoginHistory> data = platformAdminService.getActiveSessions(pageable);
        return ApiResponse.success(data, "Active system user login sessions fetched successfully");
    }

    @GetMapping("/auth/failed-logins")
    public ApiResponse<Page<LoginHistory>> getFailedLoginAttempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<LoginHistory> data = platformAdminService.getFailedLoginAttempts(pageable);
        return ApiResponse.success(data, "Failed login attempts list fetched successfully");
    }

    @PostMapping("/auth/sessions/{id}/logout")
    public ApiResponse<Void> forceLogoutSession(@PathVariable Long id) {
        platformAdminService.forceLogoutSession(id, getActiveUser());
        return ApiResponse.success(null, "Forced logout session command completed successfully");
    }

    // 8. System Settings
    @GetMapping("/settings")
    public ApiResponse<List<SaaSSystemSetting>> getSettings() {
        List<SaaSSystemSetting> settings = platformAdminService.getSystemSettings();
        return ApiResponse.success(settings, "SaaS global configurations list fetched successfully");
    }

    @PutMapping("/settings")
    public ApiResponse<SaaSSystemSetting> updateSetting(@RequestBody Map<String, String> payload) {
        String key = payload.get("key");
        String value = payload.get("value");
        SaaSSystemSetting updated = platformAdminService.updateSystemSetting(key, value, getActiveUser());
        return ApiResponse.success(updated, "SaaS configuration updated successfully");
    }

    // 9. Audit Logs
    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<AuditLog> data = platformAdminService.getSystemAuditLogs(pageable);
        return ApiResponse.success(data, "Global audit history timeline fetched successfully");
    }

    // 10. Backup & Restore
    @GetMapping("/backups")
    public ApiResponse<Page<SaaSBackup>> getBackups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSBackup> data = platformAdminService.getBackups(pageable);
        return ApiResponse.success(data, "System database snapshot logs fetched successfully");
    }

    @PostMapping("/backups")
    public ApiResponse<SaaSBackup> triggerBackup() {
        SaaSBackup backup = platformAdminService.triggerBackup(getActiveUser());
        return ApiResponse.success(backup, "Manual database snapshot backup triggered successfully");
    }

    // 11. Support Tickets
    @GetMapping("/tickets")
    public ApiResponse<Page<SaaSSupportTicket>> getSupportTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSSupportTicket> data = platformAdminService.getSupportTickets(status, pageable);
        return ApiResponse.success(data, "SaaS support tickets query completed successfully");
    }

    @GetMapping("/tickets/{id}")
    public ApiResponse<SaaSSupportTicket> getSupportTicket(@PathVariable Long id) {
        SaaSSupportTicket ticket = platformAdminService.getSupportTicket(id);
        return ApiResponse.success(ticket, "Support ticket thread details fetched successfully");
    }

    @PostMapping("/tickets/{id}/replies")
    public ApiResponse<SaaSSupportReply> createSupportReply(
            @PathVariable Long id,
            @RequestBody SaaSSupportReply reply) {
        SaaSSupportReply created = platformAdminService.createSupportReply(id, reply, getActiveUser());
        return ApiResponse.success(created, "Support reply registered and status updated successfully");
    }

    @PutMapping("/tickets/{id}/status")
    public ApiResponse<SaaSSupportTicket> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        SaaSSupportTicket updated = platformAdminService.updateTicketStatus(id, status, getActiveUser());
        return ApiResponse.success(updated, "Support ticket status updated successfully");
    }

    // 12. Announcements
    @GetMapping("/announcements")
    public ApiResponse<Page<SaaSAnnouncement>> getAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSAnnouncement> data = platformAdminService.getAnnouncements(pageable);
        return ApiResponse.success(data, "Broadcasting announcements list fetched successfully");
    }

    @PostMapping("/announcements")
    public ApiResponse<SaaSAnnouncement> createAnnouncement(@RequestBody SaaSAnnouncement announcement) {
        SaaSAnnouncement created = platformAdminService.createAnnouncement(announcement, getActiveUser());
        return ApiResponse.success(created, "System broadcast announcement registered successfully");
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        platformAdminService.deleteAnnouncement(id, getActiveUser());
        return ApiResponse.success(null, "Broadcast announcement removed successfully");
    }

    // 14. Billing
    @GetMapping("/billing-invoices")
    public ApiResponse<Page<SaaSBillingInvoice>> getBillingInvoices(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SaaSBillingInvoice> data = platformAdminService.getBillingInvoices(companyId, pageable);
        return ApiResponse.success(data, "Subscription billing statements fetched successfully");
    }

    @GetMapping("/vehicles")
    public ApiResponse<Page<Vehicle>> getVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Vehicle> data = platformAdminService.getVehicles(pageable);
        return ApiResponse.success(data, "All system vehicles list fetched successfully");
    }

    @GetMapping("/trips")
    public ApiResponse<Page<Trip>> getTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Trip> data = platformAdminService.getTrips(pageable);
        return ApiResponse.success(data, "All system trips list fetched successfully");
    }
}
