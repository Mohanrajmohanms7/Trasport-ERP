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
    @org.springframework.beans.factory.annotation.Autowired
    private com.transport.erp.service.XlsxExportService xlsxExportService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.transport.erp.service.AuditService auditService;

    /** One ZIP with an Excel file per business list of a tenant: a portable data copy / hand-over. */
    @GetMapping("/companies/{id}/data-export")
    public org.springframework.http.ResponseEntity<byte[]> exportCompanyData(@PathVariable Long id) throws java.io.IOException {
        java.util.LinkedHashMap<String, java.util.function.Function<Long, byte[]>> parts = new java.util.LinkedHashMap<>();
        parts.put("vehicles", c -> xlsxExportService.exportVehicles(c, "xlsx"));
        parts.put("drivers", c -> xlsxExportService.exportDrivers(c, "xlsx"));
        parts.put("customers", c -> xlsxExportService.exportCustomers(c, "xlsx"));
        parts.put("materials", c -> xlsxExportService.exportMaterials(c, "xlsx"));
        parts.put("suppliers", c -> xlsxExportService.exportSuppliers(c, "xlsx"));
        parts.put("bookings", c -> xlsxExportService.exportBookings(c, "xlsx"));
        parts.put("trips", c -> xlsxExportService.exportTrips(c, "xlsx"));
        parts.put("fuel", c -> xlsxExportService.exportFuelEntries(c, "xlsx"));
        parts.put("expenses", c -> xlsxExportService.exportExpenses(c, "xlsx"));
        parts.put("invoices", c -> xlsxExportService.exportSalesInvoices(c, "xlsx"));
        parts.put("receipts", c -> xlsxExportService.exportCustomerReceipts(c, "xlsx"));
        parts.put("driver_payroll", c -> xlsxExportService.exportDriverPayroll(c, "xlsx"));
        parts.put("driver_advances", c -> xlsxExportService.exportDriverAdvances(c, "xlsx"));
        parts.put("work_orders", c -> xlsxExportService.exportWorkOrders(c, "xlsx"));
        parts.put("spare_parts", c -> xlsxExportService.exportSpareParts(c, "xlsx"));
        parts.put("stock", c -> xlsxExportService.exportStock(c, "xlsx"));
        parts.put("journal", c -> xlsxExportService.exportJournal(c, "xlsx"));
        parts.put("chart_of_accounts", c -> xlsxExportService.exportChartOfAccounts(c, "xlsx"));
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bos)) {
            for (var e : parts.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(e.getKey() + ".xlsx"));
                zip.write(e.getValue().apply(id));
                zip.closeEntry();
            }
        }
        auditService.log(getActiveUser(), "COMPANY_DATA_EXPORT", "companies", id, null, "Downloaded company data export ZIP");
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"company_" + id + "_data_" + java.time.LocalDate.now() + ".zip\"")
                .body(bos.toByteArray());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.transport.erp.features.FeatureAccessService featureAccessService;

    @GetMapping("/features/catalog")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> featureCatalog() {
        return ApiResponse.success(featureAccessService.catalog(), "Feature catalog");
    }

    @GetMapping("/companies/{id}/features")
    public ApiResponse<java.util.Map<String, Object>> companyFeatures(@PathVariable Long id) {
        return ApiResponse.success(featureAccessService.companyView(id), "Client features");
    }

    /** Body: {"features":{"code":true|false,...}} — values equal to the plan follow the plan. */
    @PutMapping("/companies/{id}/features")
    public ApiResponse<java.util.Map<String, Object>> saveCompanyFeatures(@PathVariable Long id, @RequestBody java.util.Map<String, java.util.Map<String, Boolean>> body) {
        java.util.Map<String, Object> r = featureAccessService.saveCompany(id, body.get("features"), getActiveUser());
        auditService.log(getActiveUser(), "CLIENT_FEATURES_UPDATED", "companies", id, null, "Client feature access updated");
        return ApiResponse.success(r, "Client feature access saved");
    }

    @PostMapping("/companies/{id}/features/reset")
    public ApiResponse<java.util.Map<String, Object>> resetCompanyFeatures(@PathVariable Long id) {
        auditService.log(getActiveUser(), "CLIENT_FEATURES_RESET", "companies", id, null, "Client feature access reset to plan");
        return ApiResponse.success(featureAccessService.resetCompany(id), "Client now follows its plan");
    }

    @GetMapping("/plans/{id}/features")
    public ApiResponse<java.util.Map<String, Object>> planFeatures(@PathVariable Long id) {
        return ApiResponse.success(featureAccessService.planView(id), "Plan features");
    }

    @PutMapping("/plans/{id}/features")
    public ApiResponse<java.util.Map<String, Object>> savePlanFeatures(@PathVariable Long id, @RequestBody java.util.Map<String, java.util.Map<String, Boolean>> body) {
        java.util.Map<String, Object> r = featureAccessService.savePlan(id, body.get("features"), getActiveUser());
        auditService.log(getActiveUser(), "PLAN_FEATURES_UPDATED", "saas_plans", id, null, "Plan feature access updated");
        return ApiResponse.success(r, "Plan features saved");
    }

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

    // 11. Support Tickets (Help & Support; see docs/SUPPORT_TICKETS.md)
    @Autowired
    private com.transport.erp.service.SupportTicketService supportTickets;

    @GetMapping("/tickets")
    public ApiResponse<Page<Map<String, Object>>> getSupportTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(supportTickets.listForAdmin(status, priority, companyId, module, assignedTo, search, overdue, page, size),
                "Support tickets fetched");
    }

    @GetMapping("/tickets/dashboard")
    public ApiResponse<Map<String, Object>> getSupportDashboard() {
        return ApiResponse.success(supportTickets.dashboard(), "Support dashboard fetched");
    }

    @GetMapping("/tickets/assignees")
    public ApiResponse<java.util.List<Map<String, Object>>> getSupportAssignees() {
        return ApiResponse.success(supportTickets.assignees(), "Assignees fetched");
    }

    @GetMapping("/tickets/{id}")
    public ApiResponse<Map<String, Object>> getSupportTicket(@PathVariable Long id) {
        return ApiResponse.success(supportTickets.getForAdmin(id), "Support ticket fetched");
    }

    /** Reply to the client, or an internal note with {"internal": true}; optional {"status": "..."}. */
    @PostMapping("/tickets/{id}/replies")
    public ApiResponse<Map<String, Object>> createSupportReply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(supportTickets.adminReply(id, body), "Reply saved");
    }

    @PutMapping("/tickets/{id}/status")
    public ApiResponse<Map<String, Object>> updateTicketStatus(@PathVariable Long id, @RequestParam String status,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        Object resolution = body == null ? null : body.get("resolution");
        return ApiResponse.success(supportTickets.setStatus(id, status, resolution == null ? null : String.valueOf(resolution)),
                "Support ticket status updated");
    }

    @PostMapping(value = "/tickets/{id}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> attachToTicket(@PathVariable Long id,
                                                           @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                           @RequestParam(defaultValue = "false") boolean internal) {
        return ApiResponse.success(supportTickets.adminAttach(id, file, internal), "File attached");
    }

    @GetMapping("/tickets/{id}/attachments/{attachmentId}")
    public org.springframework.http.ResponseEntity<byte[]> downloadTicketFile(@PathVariable Long id, @PathVariable Long attachmentId) {
        return com.transport.erp.controller.SupportController.fileResponse(supportTickets.download(id, attachmentId, true));
    }

    /** Close resolved tickets the client has not answered for 7 days now (also runs hourly). */
    @PostMapping("/tickets/auto-close")
    public ApiResponse<Map<String, Object>> autoCloseTickets() {
        return ApiResponse.success(Map.of("closed", supportTickets.runAutoCloseNow()), "Auto-close done");
    }

    @PutMapping("/tickets/{id}/priority")
    public ApiResponse<Map<String, Object>> updateTicketPriority(@PathVariable Long id, @RequestParam String priority) {
        return ApiResponse.success(supportTickets.setPriority(id, priority), "Support ticket priority updated");
    }

    @PutMapping("/tickets/{id}/assign")
    public ApiResponse<Map<String, Object>> assignTicket(@PathVariable Long id, @RequestParam(required = false) String username) {
        return ApiResponse.success(supportTickets.assign(id, username), "Support ticket assigned");
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
