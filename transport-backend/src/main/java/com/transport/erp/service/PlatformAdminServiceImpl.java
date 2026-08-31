package com.transport.erp.service;

import com.transport.erp.dto.ClientOnboardingRequest;
import com.transport.erp.dto.ClientOnboardingResult;
import com.transport.erp.dto.PlatformAdminStatsDTO;
import com.transport.erp.dto.SaaSClientDTO;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional
public class PlatformAdminServiceImpl implements PlatformAdminService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Autowired
    private LookupValueRepository lookupValueRepository;

    @Autowired
    private TenantSupportingDataService supportingDataService;

    @Autowired
    private FinancialYearRepository financialYearRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppRoleRepository roleRepository;

    @Autowired
    private AppPermissionRepository permissionRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private SaaSPlanRepository planRepository;

    @Autowired
    private SaaSTenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private SaaSLicenseRepository licenseRepository;

    @Autowired
    private SaaSSupportTicketRepository ticketRepository;

    @Autowired
    private SaaSSupportReplyRepository replyRepository;

    @Autowired
    private SaaSAnnouncementRepository announcementRepository;

    @Autowired
    private SaaSSystemSettingRepository systemSettingRepository;

    @Autowired
    private SaaSBackupRepository backupRepository;

    @Autowired
    private SaaSBillingInvoiceRepository billingInvoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Autowired
    private DataSource dataSource;

    // 1. Dashboard & Analytics
    @Override
    @Transactional(readOnly = true)
    public PlatformAdminStatsDTO getSystemStats() {
        PlatformAdminStatsDTO stats = new PlatformAdminStatsDTO();
        stats.setTotalCompanies(companyRepository.count());
        
        long activeCompanies = companyRepository.findAll().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()) && !c.getIsDeleted()).count();
        stats.setActiveCompanies(activeCompanies);
        
        long totalUsrs = userRepository.findAll().stream()
                .filter(u -> u.getCompanyId() != null && !Boolean.TRUE.equals(u.getIsDeleted()))
                .count();
        stats.setTotalUsers(totalUsrs);
        stats.setTotalVehicles(vehicleRepository.count());
        stats.setTotalTrips(tripRepository.count());
        stats.setTotalAuditLogs(auditLogRepository.count());
        
        long activeLic = licenseRepository.findAll().stream()
                .filter(l -> "ACTIVE".equals(l.getStatus())).count();
        stats.setActiveLicenses(activeLic);
        
        long openTkt = ticketRepository.findAll().stream()
                .filter(t -> "OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus())).count();
        stats.setOpenSupportTickets(openTkt);
        
        double mtdRevenue = billingInvoiceRepository.findAll().stream()
                .filter(i -> "PAID".equals(i.getStatus()) && i.getInvoiceDate().getMonth() == LocalDate.now().getMonth())
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();
        stats.setMonthToDateRevenue(mtdRevenue);
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 1. CARDS DATA
        long totalClients = companyRepository.count();
        analytics.put("totalClients", totalClients);

        long activeClients = companyRepository.findAll().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()) && !c.getIsDeleted()).count();
        analytics.put("activeClients", activeClients);

        long trialClients = companyRepository.findAll().stream()
                .filter(c -> c.getSubscriptionPlan() != null && "TRIAL".equalsIgnoreCase(c.getSubscriptionPlan().getCode()) && !c.getIsDeleted())
                .count();
        analytics.put("trialClients", trialClients);

        long expiredClients = companyRepository.findAll().stream()
                .filter(c -> "EXPIRED".equalsIgnoreCase(c.getSubscriptionStatus()) || 
                             (c.getSubscriptionEndDate() != null && today.isAfter(c.getSubscriptionEndDate())))
                .count();
        analytics.put("expiredClients", expiredClients);

        double monthlyRevenue = billingInvoiceRepository.findAll().stream()
                .filter(i -> "PAID".equals(i.getStatus()) && i.getInvoiceDate().getMonth() == today.getMonth() && i.getInvoiceDate().getYear() == today.getYear())
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();
        analytics.put("monthlyRevenue", monthlyRevenue);

        double yearlyRevenue = billingInvoiceRepository.findAll().stream()
                .filter(i -> "PAID".equals(i.getStatus()) && i.getInvoiceDate().getYear() == today.getYear())
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();
        analytics.put("yearlyRevenue", yearlyRevenue);

        long totalUsers = userRepository.findAll().stream()
                .filter(u -> u.getCompanyId() != null && !Boolean.TRUE.equals(u.getIsDeleted()))
                .count();
        analytics.put("totalUsers", totalUsers);

        long totalVehicles = vehicleRepository.count();
        analytics.put("totalVehicles", totalVehicles);

        long totalTrips = tripRepository.count();
        analytics.put("totalTrips", totalTrips);

        double totalStorage = companyRepository.findAll().stream()
                .filter(c -> !c.getIsDeleted() && c.getStorage() != null)
                .mapToDouble(c -> {
                    try {
                        String clean = c.getStorage().replaceAll("[^0-9.]", "").trim();
                        return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
                    } catch (Exception e) {
                        return 0.0;
                    }
                }).sum();
        analytics.put("totalStorage", totalStorage);

        long totalApiCalls = auditLogRepository.count();
        analytics.put("totalApiCalls", totalApiCalls);

        long todaysLogin = loginHistoryRepository.findAll().stream()
                .filter(h -> h.getLoginTime() != null && h.getLoginTime().toLocalDate().isEqual(today))
                .filter(h -> {
                    AppUser user = h.getUser();
                    return user != null && user.getCompanyId() != null;
                })
                .count();
        analytics.put("todaysLogin", todaysLogin);

        long openTickets = ticketRepository.findAll().stream()
                .filter(t -> "OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus())).count();
        analytics.put("supportTickets", openTickets);

        // Recent Clients
        List<Map<String, Object>> recentClients = companyRepository.findAll().stream()
                .filter(c -> !c.getIsDeleted())
                .sorted(Comparator.comparing(Company::getId).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("code", c.getCode());
                    m.put("ownerName", c.getOwnerName());
                    m.put("status", c.getStatus());
                    m.put("planName", c.getSubscriptionPlan() != null ? c.getSubscriptionPlan().getName() : "None");
                    return m;
                }).toList();
        analytics.put("recentClients", recentClients);

        // Renewal Due (ending in 30 days)
        List<Map<String, Object>> renewalDue = companyRepository.findAll().stream()
                .filter(c -> !c.getIsDeleted() && c.getSubscriptionEndDate() != null && 
                             !today.isAfter(c.getSubscriptionEndDate()) && 
                             !today.plusDays(30).isBefore(c.getSubscriptionEndDate()))
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("code", c.getCode());
                    m.put("endDate", c.getSubscriptionEndDate());
                    return m;
                }).toList();
        analytics.put("renewalDue", renewalDue);

        // Latest Payments
        List<Map<String, Object>> latestPayments = billingInvoiceRepository.findAll().stream()
                .sorted(Comparator.comparing(SaaSBillingInvoice::getId).reversed())
                .limit(5)
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("invoiceNumber", i.getInvoiceNumber());
                    m.put("companyId", i.getCompanyId());
                    m.put("amount", i.getAmount());
                    m.put("status", i.getStatus());
                    m.put("invoiceDate", i.getInvoiceDate());
                    return m;
                }).toList();
        analytics.put("latestPayments", latestPayments);

        // System Health parameters (live JVM + DB probe)
        Map<String, Object> systemHealth = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemLoadAverage();
        if (cpuLoad < 0) {
            cpuLoad = 0;
        } else {
            int processors = Math.max(1, osBean.getAvailableProcessors());
            cpuLoad = Math.min(100.0, (cpuLoad / processors) * 100.0);
        }
        systemHealth.put("cpuUsage", Math.round(cpuLoad * 10.0) / 10.0);
        systemHealth.put("memoryUsedGB", Math.round((heapUsed / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0);
        systemHealth.put("memoryTotalGB", heapMax > 0
                ? Math.round((heapMax / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0
                : 0.0);
        systemHealth.put("dbStatus", probeDatabaseStatus());
        analytics.put("systemHealth", systemHealth);

        // Charts: real monthly / cumulative counts from DB timestamps
        List<Map<String, Object>> clientGrowth = new ArrayList<>();
        List<Map<String, Object>> revenueGrowth = new ArrayList<>();
        List<Map<String, Object>> tripGrowth = new ArrayList<>();
        List<Map<String, Object>> userGrowth = new ArrayList<>();
        List<Map<String, Object>> vehicleDistribution = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate targetDate = today.minusMonths(i);
            YearMonth ym = YearMonth.from(targetDate);
            LocalDate monthEnd = ym.atEndOfMonth();
            String monthLabel = targetDate.getMonth().name().substring(0, 3) + " " + targetDate.getYear();

            long clientsCount = companyRepository.findAll().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()) && c.getCreatedDate() != null)
                    .filter(c -> !c.getCreatedDate().toLocalDate().isAfter(monthEnd))
                    .count();
            Map<String, Object> cg = new HashMap<>();
            cg.put("month", monthLabel);
            cg.put("count", clientsCount);
            clientGrowth.add(cg);

            double revTotal = billingInvoiceRepository.findAll().stream()
                    .filter(inv -> "PAID".equals(inv.getStatus())
                            && inv.getInvoiceDate() != null
                            && inv.getInvoiceDate().getMonth() == targetDate.getMonth()
                            && inv.getInvoiceDate().getYear() == targetDate.getYear())
                    .mapToDouble(inv -> inv.getAmount() != null ? inv.getAmount().doubleValue() : 0.0)
                    .sum();
            Map<String, Object> rg = new HashMap<>();
            rg.put("month", monthLabel);
            rg.put("revenue", revTotal);
            revenueGrowth.add(rg);

            long tripsCount = tripRepository.findAll().stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()) && t.getTripDate() != null)
                    .filter(t -> t.getTripDate().getMonth() == targetDate.getMonth()
                            && t.getTripDate().getYear() == targetDate.getYear())
                    .count();
            Map<String, Object> tg = new HashMap<>();
            tg.put("month", monthLabel);
            tg.put("count", tripsCount);
            tripGrowth.add(tg);

            long usersCount = userRepository.findAll().stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()) && u.getCreatedDate() != null && u.getCompanyId() != null)
                    .filter(u -> !u.getCreatedDate().toLocalDate().isAfter(monthEnd))
                    .count();
            Map<String, Object> ug = new HashMap<>();
            ug.put("month", monthLabel);
            ug.put("count", usersCount);
            userGrowth.add(ug);
        }

        analytics.put("clientGrowth", clientGrowth);
        analytics.put("revenueGrowth", revenueGrowth);
        analytics.put("tripGrowth", tripGrowth);
        analytics.put("userGrowth", userGrowth);

        // Subscriptions plan distribution split
        List<Map<String, Object>> planDistribution = new ArrayList<>();
        List<SaaSPlan> plans = planRepository.findAll();
        for (SaaSPlan plan : plans) {
            long count = subscriptionRepository.findAll().stream()
                    .filter(sub -> "ACTIVE".equals(sub.getStatus()) && sub.getPlan().getId().equals(plan.getId()))
                    .count();
            Map<String, Object> item = new HashMap<>();
            item.put("planName", plan.getName());
            item.put("count", count);
            planDistribution.add(item);
        }
        analytics.put("planDistribution", planDistribution);

        // Vehicle distribution per company
        List<Company> activeComps = companyRepository.findAll().stream()
                .filter(c -> !c.getIsDeleted())
                .limit(5)
                .toList();
        for (Company comp : activeComps) {
            long count = vehicleRepository.findAll().stream()
                    .filter(v -> v.getCompanyId() != null && v.getCompanyId().equals(comp.getId()))
                    .count();
            Map<String, Object> item = new HashMap<>();
            item.put("companyName", comp.getName());
            item.put("count", count);
            vehicleDistribution.add(item);
        }
        analytics.put("vehicleDistribution", vehicleDistribution);

        return analytics;
    }

    // 2. Client / Company Management
    @Override
    @Transactional(readOnly = true)
    public Page<Company> getCompanies(String search, String status, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return companyRepository.findByIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(search, search, pageable);
        }
        return companyRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public Company createCompany(Company company, String activeUser) {
        ClientOnboardingRequest req = new ClientOnboardingRequest();
        req.setName(company.getName());
        req.setOwnerName(company.getOwnerName());
        req.setBusinessType(company.getBusinessType());
        req.setPhone(company.getPhone());
        req.setEmail(company.getEmail());
        req.setGstNumber(company.getGstNumber());
        req.setPanNumber(company.getPanNumber());
        req.setAddress(company.getAddress());
        req.setCity(company.getCity());
        req.setState(company.getState());
        req.setPincode(company.getPincode());
        req.setWebsite(company.getWebsite());
        req.setLogo(company.getLogo());
        req.setStorage(company.getStorage());
        req.setCode(company.getCode());
        req.setMaxUsers(company.getMaxUsers());
        req.setMaxVehicles(company.getMaxVehicles());
        if (company.getSubscriptionPlan() != null) {
            req.setSubscriptionPlanId(company.getSubscriptionPlan().getId());
        }
        if (company.getSubscriptionStartDate() != null) {
            req.setSubscriptionStartDate(company.getSubscriptionStartDate().toString());
        }
        if (company.getSubscriptionEndDate() != null) {
            req.setSubscriptionEndDate(company.getSubscriptionEndDate().toString());
        }
        ClientOnboardingResult result = onboardClient(req, activeUser);
        return companyRepository.findById(result.getCompanyId())
                .orElseThrow(() -> new IllegalStateException("Onboarded company not found"));
    }

    @Override
    public ClientOnboardingResult onboardClient(ClientOnboardingRequest request, String activeUser) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Company email is required");
        }

        List<ClientOnboardingResult.OnboardingStep> steps = new ArrayList<>();
        int stepNo = 1;

        // --- Create Company ---
        Company company = new Company();
        company.setName(request.getName().trim());
        company.setOwnerName(request.getOwnerName());
        company.setBusinessType(request.getBusinessType());
        company.setPhone(request.getPhone());
        company.setEmail(request.getEmail().trim());
        company.setGstNumber(request.getGstNumber());
        company.setPanNumber(request.getPanNumber());
        company.setAddress(request.getAddress());
        company.setCity(request.getCity());
        company.setState(request.getState());
        company.setPincode(request.getPincode());
        company.setWebsite(request.getWebsite());
        company.setLogo(request.getLogo());
        company.setStorage(request.getStorage() == null || request.getStorage().trim().isEmpty()
                ? "10 GB" : request.getStorage().trim());

        String companyCode = (request.getCode() == null || request.getCode().trim().isEmpty())
                ? generateCompanyCode(company.getName(), company.getOwnerName())
                : request.getCode().trim().toUpperCase();
        if (companyRepository.findByCodeAndIsDeletedFalse(companyCode).isPresent()) {
            throw new IllegalArgumentException("Company with code " + companyCode + " already exists!");
        }
        company.setCode(companyCode);
        company.setStatus("ACTIVE");
        company.setIsDeleted(false);
        Company savedCompany = companyRepository.save(company);
        steps.add(step("CREATE_COMPANY", "Create Company", "DONE",
                "Company #" + savedCompany.getId() + " · " + savedCompany.getName(), stepNo++));
        steps.add(step("GENERATE_COMPANY_CODE", "Generate Company Code", "DONE",
                companyCode, stepNo++));

        // --- Create Head Office ---
        Branch branch = new Branch();
        String branchCode = "HO_" + companyCode;
        branch.setCode(branchCode);
        branch.setName("Head Office");
        branch.setDescription("Primary Operations Hub for " + savedCompany.getName());
        branch.setStatus("ACTIVE");
        branch.setCompanyId(savedCompany.getId());
        branch.setGstNumber(savedCompany.getGstNumber());
        branch.setManager(savedCompany.getOwnerName() != null ? savedCompany.getOwnerName() : "Administrator");
        branch.setPhone(savedCompany.getPhone());
        branch.setEmail(savedCompany.getEmail());
        branch.setAddress(savedCompany.getAddress());
        branch.setLatitude(BigDecimal.valueOf(28.6139));
        branch.setLongitude(BigDecimal.valueOf(77.2090));
        Branch savedBranch = branchRepository.save(branch);

        savedCompany.setBranchId(savedBranch.getId());
        companyRepository.save(savedCompany);
        steps.add(step("CREATE_HEAD_OFFICE", "Create Head Office", "DONE",
                branchCode + " · " + savedBranch.getName(), stepNo++));

        // --- Default Permissions (system catalog already seeded) ---
        List<AppPermission> allPermissions = permissionRepository.findAll().stream()
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .toList();
        steps.add(step("INSERT_DEFAULT_PERMISSIONS", "Insert Default Permissions", "DONE",
                allPermissions.size() + " system permissions linked", stepNo++));

        // --- Default Roles ---
        AppRole adminRole = createTenantRole(savedCompany.getId(), savedBranch.getId(),
                "COMPANY_ADMIN", "Company Administrator", "Full ERP access for this tenant", allPermissions);
        createTenantRole(savedCompany.getId(), savedBranch.getId(),
                "BRANCH_MANAGER", "Branch Manager", "Manage branch operations",
                filterPermissions(allPermissions, "VIEW", "CREATE", "EDIT", "APPROVE", "EXPORT", "PRINT"));
        createTenantRole(savedCompany.getId(), savedBranch.getId(),
                "OPERATOR", "Operator", "Day-to-day operations",
                filterPermissions(allPermissions, "VIEW", "CREATE", "EDIT"));
        createTenantRole(savedCompany.getId(), savedBranch.getId(),
                "ACCOUNTANT", "Accountant", "Billing and payments",
                filterPermissions(allPermissions, "VIEW", "CREATE", "EDIT", "EXPORT", "PRINT"));
        createTenantRole(savedCompany.getId(), savedBranch.getId(),
                "VIEWER", "Viewer", "Read-only access",
                filterPermissions(allPermissions, "VIEW"));
        steps.add(step("INSERT_DEFAULT_ROLES", "Insert Default Roles", "DONE",
                "COMPANY_ADMIN, BRANCH_MANAGER, OPERATOR, ACCOUNTANT, VIEWER", stepNo++));

        // --- Create Company Admin + credentials ---
        String adminUsername = (request.getAdminUsername() != null && !request.getAdminUsername().trim().isEmpty())
                ? request.getAdminUsername().trim()
                : generateUniqueUsername(companyCode);
        String temporaryPassword = (request.getAdminPassword() != null && !request.getAdminPassword().trim().isEmpty())
                ? request.getAdminPassword().trim()
                : generateTemporaryPassword();
        AppUser adminUser = new AppUser();
        adminUser.setCode(companyCode + "_ADMIN");
        adminUser.setName(savedCompany.getOwnerName() != null
                ? savedCompany.getOwnerName()
                : (savedCompany.getName() + " Admin"));
        adminUser.setUsername(adminUsername);
        adminUser.setPassword(passwordEncoder.encode(temporaryPassword));
        adminUser.setEmail(savedCompany.getEmail());
        adminUser.setPhone(savedCompany.getPhone());
        adminUser.setStatus("ACTIVE");
        adminUser.setCompanyId(savedCompany.getId());
        adminUser.setBranchId(savedBranch.getId());
        adminUser.setForcePasswordChange(true);
        adminUser.getRoles().add(adminRole);
        AppUser savedAdmin = userRepository.save(adminUser);
        steps.add(step("CREATE_COMPANY_ADMIN", "Create Company Admin", "DONE",
                "User #" + savedAdmin.getId(), stepNo++));
        steps.add(step("GENERATE_USERNAME", "Generate Username", "DONE", adminUsername, stepNo++));
        steps.add(step("GENERATE_TEMPORARY_PASSWORD", "Generate Temporary Password", "DONE",
                (request.getAdminPassword() != null && !request.getAdminPassword().trim().isEmpty())
                ? "Custom credentials configured successfully"
                : "Temporary password issued (force change on first login)", stepNo++));

        // --- Financial Year ---
        LocalDate today = LocalDate.now();
        int fyStartYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        FinancialYear fy = new FinancialYear();
        fy.setCode("FY" + fyStartYear + "-" + String.valueOf(fyStartYear + 1).substring(2));
        fy.setName("Financial Year " + fyStartYear + "-" + (fyStartYear + 1));
        fy.setStartDate(LocalDate.of(fyStartYear, 4, 1));
        fy.setEndDate(LocalDate.of(fyStartYear + 1, 3, 31));
        fy.setIsDefault(true);
        fy.setStatus("ACTIVE");
        fy.setCompanyId(savedCompany.getId());
        fy.setBranchId(savedBranch.getId());
        financialYearRepository.save(fy);

        // --- Default Masters (settings + lookups from template company 1) ---
        int settingsCopied = 0;
        List<AppSetting> templateSettings = appSettingRepository.findAll().stream()
                .filter(s -> Long.valueOf(1L).equals(s.getCompanyId()))
                .toList();
        for (AppSetting ts : templateSettings) {
            AppSetting s = new AppSetting();
            s.setKeyName(ts.getKeyName());
            s.setValueData(ts.getValueData());
            s.setDescription(ts.getDescription());
            s.setCode(ts.getCode());
            s.setName(ts.getName());
            s.setStatus(ts.getStatus());
            s.setCompanyId(savedCompany.getId());
            s.setBranchId(savedBranch.getId());
            s.setIsDeleted(false);
            appSettingRepository.save(s);
            settingsCopied++;
        }

        int lookupsCopied = 0;
        List<LookupValue> templateLookups = lookupValueRepository.findAll().stream()
                .filter(lv -> Long.valueOf(1L).equals(lv.getCompanyId()))
                .toList();
        for (LookupValue tlv : templateLookups) {
            LookupValue lv = new LookupValue();
            lv.setType(tlv.getType());
            lv.setCode(tlv.getCode());
            lv.setName(tlv.getName());
            lv.setStatus(tlv.getStatus());
            lv.setDescription(tlv.getDescription());
            lv.setCompanyId(savedCompany.getId());
            lv.setBranchId(savedBranch.getId());
            lv.setIsDeleted(false);
            lookupValueRepository.save(lv);
            lookupsCopied++;
        }
        steps.add(step("INSERT_DEFAULT_MASTERS", "Insert Default Masters", "DONE",
                settingsCopied + " settings, " + lookupsCopied + " lookups, FY " + fy.getCode(), stepNo++));

        // Ensure full supporting masters (extra lookups + sample materials/quarry) — no C/V/D
        Map<String, Object> supporting = supportingDataService.seedForCompany(savedCompany.getId(), savedBranch.getId());
        steps.add(step("SEED_SUPPORTING_MASTERS", "Seed Supporting Masters", "DONE",
                "lookups+" + supporting.get("lookupsAdded")
                        + ", materials+" + supporting.get("materialsAdded")
                        + ", quarries+" + supporting.get("quarriesAdded"), stepNo++));

        // --- Assign Subscription ---
        SaaSPlan plan = resolveOnboardingPlan(request.getSubscriptionPlanId());
        LocalDate subStart = parseDateOr(request.getSubscriptionStartDate(), today);
        LocalDate subEnd = parseDateOr(request.getSubscriptionEndDate(), null);
        if (subEnd == null) {
            int months = request.getBillingMonths() != null && request.getBillingMonths() > 0
                    ? request.getBillingMonths()
                    : ("YEARLY".equalsIgnoreCase(plan.getBillingPeriod()) ? 12
                    : ("TRIAL".equalsIgnoreCase(plan.getCode()) ? 1 : 1));
            subEnd = subStart.plusMonths(months);
        }

        SaaSTenantSubscription sub = new SaaSTenantSubscription();
        sub.setCompanyId(savedCompany.getId());
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        sub.setStartDate(subStart);
        sub.setEndDate(subEnd);
        sub.setAmountPaid(BigDecimal.ZERO);
        sub.setPaymentMethod("SYSTEM");
        sub.setPaymentStatus("PAID");
        subscriptionRepository.save(sub);

        int maxUsers = request.getMaxUsers() != null ? request.getMaxUsers() : plan.getMaxUsers();
        int maxVehicles = request.getMaxVehicles() != null ? request.getMaxVehicles() : plan.getMaxVehicles();
        savedCompany.setSubscriptionPlan(plan);
        savedCompany.setSubscriptionStartDate(subStart);
        savedCompany.setSubscriptionEndDate(subEnd);
        savedCompany.setSubscriptionRenewalDate(subEnd);
        savedCompany.setSubscriptionStatus("ACTIVE");
        savedCompany.setMaxUsers(maxUsers);
        savedCompany.setMaxVehicles(maxVehicles);
        companyRepository.save(savedCompany);

        String licenseKey = "LIC-" + companyCode + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SaaSLicense lic = new SaaSLicense();
        lic.setCompanyId(savedCompany.getId());
        lic.setLicenseKey(licenseKey);
        lic.setStatus("ACTIVE");
        lic.setActivationDate(subStart);
        lic.setExpiryDate(subEnd);
        lic.setMaxUsers(maxUsers);
        lic.setMaxVehicles(maxVehicles);
        licenseRepository.save(lic);
        steps.add(step("ASSIGN_SUBSCRIPTION", "Assign Subscription", "DONE",
                plan.getName() + " · " + subStart + " → " + subEnd, stepNo++));

        // --- Welcome Email (future ready) ---
        boolean sendEmail = request.getSendWelcomeEmail() == null || request.getSendWelcomeEmail();
        String welcomeStatus;
        String welcomeNote;
        if (sendEmail) {
            welcomeStatus = queueWelcomeEmail(savedCompany, adminUsername, temporaryPassword);
            welcomeNote = "Welcome email queued for delivery (SMTP integration pending)";
            steps.add(step("SEND_WELCOME_EMAIL", "Send Welcome Email", "DONE", welcomeNote, stepNo++));
        } else {
            welcomeStatus = "SKIPPED";
            welcomeNote = "Welcome email skipped by request";
            steps.add(step("SEND_WELCOME_EMAIL", "Send Welcome Email", "SKIPPED", welcomeNote, stepNo++));
        }

        steps.add(step("CLIENT_READY", "Client Ready", "DONE",
                "Company Admin can login with temporary credentials", stepNo));

        auditService.log(activeUser, "ONBOARD_CLIENT", "companies", savedCompany.getId(), null,
                "Phase 29 client onboarding complete: " + savedCompany.getName()
                        + " (admin=" + adminUsername + ", plan=" + plan.getCode() + ")");

        return ClientOnboardingResult.builder()
                .companyId(savedCompany.getId())
                .companyName(savedCompany.getName())
                .companyCode(companyCode)
                .branchId(savedBranch.getId())
                .branchCode(branchCode)
                .branchName(savedBranch.getName())
                .adminUsername(adminUsername)
                .temporaryPassword(temporaryPassword)
                .adminUserId(savedAdmin.getId())
                .planName(plan.getName())
                .planCode(plan.getCode())
                .subscriptionStatus("ACTIVE")
                .subscriptionStartDate(subStart.toString())
                .subscriptionEndDate(subEnd.toString())
                .licenseKey(licenseKey)
                .welcomeEmailStatus(welcomeStatus)
                .welcomeEmailNote(welcomeNote)
                .clientReady(true)
                .steps(steps)
                .build();
    }

    private ClientOnboardingResult.OnboardingStep step(String code, String label, String status, String detail, int order) {
        return ClientOnboardingResult.OnboardingStep.builder()
                .order(order)
                .code(code)
                .label(label)
                .status(status)
                .detail(detail)
                .build();
    }

    private AppRole createTenantRole(Long companyId, Long branchId, String code, String name,
                                     String description, Collection<AppPermission> permissions) {
        // Reuse if this tenant already has the role (retry-safe after partial failures)
        Optional<AppRole> existing = roleRepository.findByCodeAndCompanyIdAndIsDeletedFalse(code, companyId);
        if (existing.isPresent()) {
            AppRole role = existing.get();
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
            role.setBranchId(branchId);
            role.setName(name);
            role.setDescription(description);
            role.setStatus("ACTIVE");
            return roleRepository.save(role);
        }

        AppRole role = new AppRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setStatus("ACTIVE");
        role.setCompanyId(companyId);
        role.setBranchId(branchId);
        role.getPermissions().addAll(permissions);
        return roleRepository.save(role);
    }

    private List<AppPermission> filterPermissions(List<AppPermission> all, String... codes) {
        Set<String> wanted = new HashSet<>(Arrays.asList(codes));
        return all.stream().filter(p -> wanted.contains(p.getCode())).toList();
    }

    private SaaSPlan resolveOnboardingPlan(Long planId) {
        if (planId != null) {
            return planRepository.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found: " + planId));
        }
        return planRepository.findByCodeAndIsDeletedFalse("TRIAL")
                .or(() -> planRepository.findByCodeAndIsDeletedFalse("BASIC"))
                .orElseGet(() -> planRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No SaaS plans configured. Create a plan first.")));
    }

    private String generateUniqueUsername(String companyCode) {
        String base = "admin_" + companyCode.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (base.length() < 6) {
            base = "admin_" + companyCode.toLowerCase();
        }
        String candidate = base;
        int i = 1;
        while (userRepository.findByUsernameAndIsDeletedFalse(candidate).isPresent()) {
            candidate = base + i;
            i++;
        }
        return candidate;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Tmp@");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private LocalDate parseDateOr(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(value);
    }

    /**
     * Future-ready welcome email hook. Logs intent; SMTP/provider can be wired later.
     */
    private String queueWelcomeEmail(Company company, String username, String temporaryPassword) {
        // Intentionally no SMTP send yet — keeps Phase 29 future-ready without failing onboarding.
        System.out.println("[WELCOME_EMAIL_QUEUED] to=" + company.getEmail()
                + " company=" + company.getCode()
                + " username=" + username
                + " tempPasswordLength=" + (temporaryPassword != null ? temporaryPassword.length() : 0));
        return "QUEUED";
    }

    private String generateCompanyCode(String companyName, String ownerName) {
        String base = companyName != null && !companyName.trim().isEmpty() ? companyName : ownerName;
        if (base == null || base.trim().isEmpty()) {
            base = "AKS";
        }
        String prefix = base.replaceAll("[^a-zA-Z]", "").toUpperCase();
        if (prefix.length() < 3) {
            prefix = (prefix + "XYZ").substring(0, 3);
        } else {
            prefix = prefix.substring(0, 3);
        }
        
        int suffix = 1;
        while (suffix < 1000) {
            String code = String.format("%s%03d", prefix, suffix);
            Optional<Company> existing = companyRepository.findByCodeAndIsDeletedFalse(code);
            if (!existing.isPresent()) {
                return code;
            }
            suffix++;
        }
        return prefix + (System.currentTimeMillis() % 1000);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaaSClientDTO> getClients(String search, String status, Pageable pageable) {
        Page<Company> companiesPage;
        if (search != null && !search.isEmpty()) {
            companiesPage = companyRepository.findByIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(search, search, pageable);
        } else {
            companiesPage = companyRepository.findByIsDeletedFalse(pageable);
        }

        return companiesPage.map(company -> mapToClientDTO(company));
    }

    @Override
    @Transactional(readOnly = true)
    public SaaSClientDTO getClientDetails(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id " + id));
        if (company.getIsDeleted()) {
            throw new IllegalArgumentException("Client has been deleted.");
        }
        return mapToClientDTO(company);
    }

    private SaaSClientDTO mapToClientDTO(Company company) {
        SaaSClientDTO dto = new SaaSClientDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setCode(company.getCode());
        dto.setOwnerName(company.getOwnerName());
        dto.setBusinessType(company.getBusinessType());
        dto.setPhone(company.getPhone());
        dto.setEmail(company.getEmail());
        dto.setGstNumber(company.getGstNumber());
        dto.setAddress(company.getAddress());
        dto.setCity(company.getCity());
        dto.setState(company.getState());
        dto.setCountry(company.getCountry());
        dto.setPincode(company.getPincode());
        dto.setLogo(company.getLogo());
        dto.setWebsite(company.getWebsite());
        dto.setStatus(company.getStatus());
        dto.setStorage(company.getStorage());
        dto.setCreatedDate(company.getCreatedDate());

        // Map subscription fields directly from Company record
        dto.setSubscriptionStartDate(company.getSubscriptionStartDate());
        dto.setSubscriptionEndDate(company.getSubscriptionEndDate());
        dto.setSubscriptionRenewalDate(company.getSubscriptionRenewalDate());
        dto.setSubscriptionStatus(company.getSubscriptionStatus());
        dto.setMaxUsers(company.getMaxUsers());
        dto.setMaxVehicles(company.getMaxVehicles());
        if (company.getSubscriptionPlan() != null) {
            dto.setSubscriptionPlanId(company.getSubscriptionPlan().getId());
            dto.setPlanName(company.getSubscriptionPlan().getName());
            dto.setExpiryDate(company.getSubscriptionEndDate());
        } else {
            dto.setPlanName("No Active Plan");
        }

        // Count active licenses
        long activeLics = licenseRepository.findAll().stream()
                .filter(l -> l.getCompanyId().equals(company.getId()) && "ACTIVE".equals(l.getStatus()))
                .count();
        dto.setLicenseCount((int) activeLics);

        return dto;
    }

    @Override
    public Company updateCompanyStatus(Long id, String status, String activeUser) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id " + id));
        company.setStatus(status);
        Company updated = companyRepository.save(company);
        
        auditService.log(activeUser, "UPDATE_COMPANY_STATUS", "companies", id, null,
                "Updated company status to: " + status);
        return updated;
    }

    @Override
    public Company updateCompany(Long id, Company companyDetails, String activeUser) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id " + id));
        
        company.setName(companyDetails.getName());
        company.setOwnerName(companyDetails.getOwnerName());
        company.setBusinessType(companyDetails.getBusinessType());
        company.setPhone(companyDetails.getPhone());
        company.setEmail(companyDetails.getEmail());
        company.setGstNumber(companyDetails.getGstNumber());
        company.setPanNumber(companyDetails.getPanNumber());
        company.setAddress(companyDetails.getAddress());
        company.setCity(companyDetails.getCity());
        company.setState(companyDetails.getState());
        company.setPincode(companyDetails.getPincode());
        company.setLogo(companyDetails.getLogo());
        company.setWebsite(companyDetails.getWebsite());
        company.setStorage(companyDetails.getStorage());

        // Update subscription limits & details
        company.setSubscriptionStartDate(companyDetails.getSubscriptionStartDate());
        company.setSubscriptionEndDate(companyDetails.getSubscriptionEndDate());
        company.setSubscriptionRenewalDate(companyDetails.getSubscriptionRenewalDate());
        company.setSubscriptionStatus(companyDetails.getSubscriptionStatus());
        company.setMaxUsers(companyDetails.getMaxUsers());
        company.setMaxVehicles(companyDetails.getMaxVehicles());
        
        if (companyDetails.getSubscriptionPlan() != null && companyDetails.getSubscriptionPlan().getId() != null) {
            SaaSPlan plan = planRepository.findById(companyDetails.getSubscriptionPlan().getId()).orElse(null);
            company.setSubscriptionPlan(plan);
        } else {
            company.setSubscriptionPlan(null);
        }
        
        Company updated = companyRepository.save(company);
        auditService.log(activeUser, "UPDATE_COMPANY", "companies", id, null,
                "Updated SaaS client details: " + updated.getName());
        return updated;
    }

    @Override
    public void deleteCompany(Long id, String activeUser) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id " + id));
        company.setIsDeleted(true);
        company.setStatus("INACTIVE");
        companyRepository.save(company);
        
        auditService.log(activeUser, "DELETE_COMPANY", "companies", id, null,
                "Soft-deleted tenant company: " + company.getName());
    }

    // 4. Subscription Management
    @Override
    @Transactional(readOnly = true)
    public Page<SaaSPlan> getPlans(Pageable pageable) {
        return planRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public SaaSPlan createPlan(SaaSPlan plan, String activeUser) {
        plan.setStatus("ACTIVE");
        plan.setIsDeleted(false);
        SaaSPlan saved = planRepository.save(plan);
        auditService.log(activeUser, "CREATE_SAAS_PLAN", "saas_plans", saved.getId(), null,
                "Created SaaS plan: " + saved.getName());
        return saved;
    }

    @Override
    public SaaSPlan updatePlan(Long id, SaaSPlan planDetails, String activeUser) {
        SaaSPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with id " + id));
        
        plan.setName(planDetails.getName());
        plan.setDescription(planDetails.getDescription());
        plan.setPrice(planDetails.getPrice());
        plan.setBillingPeriod(planDetails.getBillingPeriod());
        plan.setMaxUsers(planDetails.getMaxUsers());
        plan.setMaxVehicles(planDetails.getMaxVehicles());
        plan.setMaxInvoices(planDetails.getMaxInvoices());
        plan.setStatus(planDetails.getStatus());
        
        SaaSPlan updated = planRepository.save(plan);
        auditService.log(activeUser, "UPDATE_SAAS_PLAN", "saas_plans", id, null,
                "Updated SaaS plan: " + updated.getName());
        return updated;
    }

    @Override
    public SaaSTenantSubscription createTenantSubscription(SaaSTenantSubscription sub, String activeUser) {
        sub.setStatus("ACTIVE");
        sub.setPaymentStatus("PAID");
        SaaSTenantSubscription saved = subscriptionRepository.save(sub);
        
        // Also automatically create/update SaaSLicense mapping
        SaaSLicense lic = new SaaSLicense();
        lic.setCompanyId(sub.getCompanyId());
        lic.setLicenseKey("LIC-" + sub.getCompanyId() + "-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        lic.setStatus("ACTIVE");
        lic.setActivationDate(sub.getStartDate());
        lic.setExpiryDate(sub.getEndDate());
        lic.setMaxUsers(sub.getPlan().getMaxUsers());
        lic.setMaxVehicles(sub.getPlan().getMaxVehicles());
        licenseRepository.save(lic);
        
        // Save a SaaS billing invoice log
        SaaSBillingInvoice inv = new SaaSBillingInvoice();
        inv.setCompanyId(sub.getCompanyId());
        inv.setInvoiceNumber("INV-SaaS-" + System.currentTimeMillis());
        inv.setInvoiceDate(LocalDate.now());
        inv.setAmount(sub.getAmountPaid());
        inv.setStatus("PAID");
        inv.setPaymentMethod(sub.getPaymentMethod());
        inv.setBillingPeriodStart(sub.getStartDate());
        inv.setBillingPeriodEnd(sub.getEndDate());
        billingInvoiceRepository.save(inv);

        auditService.log(activeUser, "CREATE_TENANT_SUBSCRIPTION", "saas_tenant_subscriptions", saved.getId(), null,
                "Provisioned subscription on plan: " + sub.getPlan().getName() + " for company: " + sub.getCompanyId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaaSTenantSubscription> getTenantSubscriptions(Long companyId, Pageable pageable) {
        if (companyId != null) {
            return subscriptionRepository.findByCompanyId(companyId, pageable);
        }
        return subscriptionRepository.findAll(pageable);
    }

    // 5. License Management
    @Override
    @Transactional(readOnly = true)
    public Page<SaaSLicense> getLicenses(Long companyId, Pageable pageable) {
        if (companyId != null) {
            return licenseRepository.findByCompanyId(companyId, pageable);
        }
        return licenseRepository.findAll(pageable);
    }

    @Override
    public SaaSLicense createLicense(SaaSLicense license, String activeUser) {
        license.setStatus("ACTIVE");
        SaaSLicense saved = licenseRepository.save(license);
        auditService.log(activeUser, "CREATE_SAAS_LICENSE", "saas_licenses", saved.getId(), null,
                "Created SaaS license key: " + saved.getLicenseKey());
        return saved;
    }

    @Override
    public SaaSLicense revokeLicense(Long id, String activeUser) {
        SaaSLicense license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found with id " + id));
        license.setStatus("REVOKED");
        SaaSLicense updated = licenseRepository.save(license);
        auditService.log(activeUser, "REVOKE_SAAS_LICENSE", "saas_licenses", id, null,
                "Revoked SaaS license key: " + updated.getLicenseKey());
        return updated;
    }

    // 6. User Management
    @Override
    @Transactional(readOnly = true)
    public Page<AppUser> getAllUsers(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return userRepository.findByIsDeletedFalseAndUsernameContainingIgnoreCaseOrIsDeletedFalseAndEmailContainingIgnoreCase(search, search, pageable);
        }
        return userRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public AppUser createUser(AppUser user, String roleCode, String activeUser) {
        Optional<AppUser> existing = userRepository.findByUsernameAndIsDeletedFalse(user.getUsername());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("ACTIVE");
        user.setIsDeleted(false);
        user.setPasswordExpiry(LocalDateTime.now().plusDays(90));
        user.setFailedLoginAttempts(0);
        user.setForcePasswordChange(false);

        // Prefer tenant role for the user's company; fall back to system role (company_id NULL).
        Optional<AppRole> roleOpt = resolveRoleForCompany(roleCode, user.getCompanyId());
        if (roleOpt.isPresent()) {
            user.getRoles().add(roleOpt.get());
        } else {
            throw new IllegalArgumentException("Role not found with code: " + roleCode);
        }

        AppUser saved = userRepository.save(user);
        auditService.log(activeUser, "CREATE_USER", "app_users", saved.getId(), null,
                "Created login user: " + saved.getUsername() + " with role: " + roleCode);
        return saved;
    }

    @Override
    public AppUser updateUser(Long id, AppUser userDetails, String roleCode, String activeUser) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        user.setStatus(userDetails.getStatus());
        user.setCompanyId(userDetails.getCompanyId());
        user.setBranchId(userDetails.getBranchId());

        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        if (roleCode != null && !roleCode.trim().isEmpty()) {
            Optional<AppRole> roleOpt = resolveRoleForCompany(roleCode, user.getCompanyId());
            if (roleOpt.isPresent()) {
                user.getRoles().clear();
                user.getRoles().add(roleOpt.get());
            }
        }

        AppUser updated = userRepository.save(user);
        auditService.log(activeUser, "UPDATE_USER", "app_users", id, null,
                "Updated details for user: " + updated.getUsername());
        return updated;
    }

    @Override
    public AppUser updateUserLockStatus(Long id, String status, String activeUser) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id " + id));
        user.setStatus(status);
        if ("ACTIVE".equals(status)) {
            user.setFailedLoginAttempts(0);
        }
        AppUser updated = userRepository.save(user);
        auditService.log(activeUser, "UPDATE_USER_STATUS", "app_users", id, null,
                "Updated user lock status to: " + status + " for username: " + user.getUsername());
        return updated;
    }

    @Override
    public void resetUserPassword(Long id, String newPassword, String activeUser) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id " + id));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(true); // Password reset forces change on next login
        userRepository.save(user);
        auditService.log(activeUser, "RESET_USER_PASSWORD", "app_users", id, null,
                "Reset password & forced change on next login for username: " + user.getUsername());
    }

    @Override
    public AppUser expireUserPassword(Long id, String activeUser) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id " + id));
        user.setPasswordExpiry(LocalDateTime.now().minusMinutes(1)); // Expired immediately
        AppUser updated = userRepository.save(user);
        auditService.log(activeUser, "EXPIRE_USER_PASSWORD", "app_users", id, null,
                "Forced password expiry for username: " + user.getUsername());
        return updated;
    }

    @Override
    public AppUser forceUserPasswordChange(Long id, String activeUser) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id " + id));
        user.setForcePasswordChange(true);
        AppUser updated = userRepository.save(user);
        auditService.log(activeUser, "FORCE_PASSWORD_CHANGE", "app_users", id, null,
                "Enabled force password change flag for username: " + user.getUsername());
        return updated;
    }

    // 7. Authentication Management (Sessions & Audits)
    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistory> getLoginHistory(String search, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return loginHistoryRepository.findByStatus(status, pageable);
        }
        return loginHistoryRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistory> getLogoutHistory(Pageable pageable) {
        return loginHistoryRepository.findByLogoutTimeIsNotNull(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistory> getActiveSessions(Pageable pageable) {
        return loginHistoryRepository.findByStatusAndLogoutTimeIsNull("SUCCESS", pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistory> getFailedLoginAttempts(Pageable pageable) {
        return loginHistoryRepository.findByStatus("FAILED", pageable);
    }

    @Override
    public void forceLogoutSession(Long loginHistoryId, String activeUser) {
        LoginHistory history = loginHistoryRepository.findById(loginHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + loginHistoryId));
        history.setLogoutTime(LocalDateTime.now());
        loginHistoryRepository.save(history);

        auditService.log(activeUser, "FORCE_LOGOUT_SESSION", "login_history", loginHistoryId, null,
                "Forced logout active session for user: " + history.getUsername());
    }

    // 8. System Settings
    @Override
    @Transactional(readOnly = true)
    public List<SaaSSystemSetting> getSystemSettings() {
        return systemSettingRepository.findAll();
    }

    @Override
    public SaaSSystemSetting updateSystemSetting(String key, String value, String activeUser) {
        SaaSSystemSetting setting = systemSettingRepository.findByKeyName(key)
                .orElseGet(() -> {
                    SaaSSystemSetting s = new SaaSSystemSetting();
                    s.setKeyName(key);
                    s.setDescription("Custom dynamic system settings parameter");
                    return s;
                });
        setting.setValueData(value);
        setting.setUpdatedBy(activeUser);
        SaaSSystemSetting updated = systemSettingRepository.save(setting);
        
        auditService.log(activeUser, "UPDATE_SAAS_SETTING", "saas_system_settings", updated.getId(), null,
                "Modified SaaS global setting parameter: " + key + " = " + value);
        return updated;
    }

    // 9. Audit Logs
    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getSystemAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    // 10. Backup & Restore
    @Override
    @Transactional(readOnly = true)
    public Page<SaaSBackup> getBackups(Pageable pageable) {
        return backupRepository.findAllByOrderByBackupDateDesc(pageable);
    }

    @Override
    public SaaSBackup triggerBackup(String activeUser) {
        long estimatedRows = companyRepository.count()
                + userRepository.count()
                + vehicleRepository.count()
                + tripRepository.count()
                + auditLogRepository.count();
        long estimatedBytes = Math.max(estimatedRows, 1) * 1024L;

        SaaSBackup backup = new SaaSBackup();
        backup.setFilename("manual_backup_transport_erp_" + System.currentTimeMillis() + ".meta");
        backup.setFileSize(formatByteSize(estimatedBytes));
        backup.setStatus("RECORDED");
        backup.setTriggerType("MANUAL");
        backup.setCreatedBy(activeUser);

        SaaSBackup saved = backupRepository.save(backup);
        auditService.log(activeUser, "TRIGGER_DATABASE_BACKUP", "saas_backups", saved.getId(), null,
                "Backup metadata recorded from live row counts (~" + estimatedRows + " rows). Physical DB dump is not configured.");
        return saved;
    }

    private String probeDatabaseStatus() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private static String formatByteSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        return String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }

    // 11. Support Tickets
    @Override
    @Transactional(readOnly = true)
    public Page<SaaSSupportTicket> getSupportTickets(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return ticketRepository.findByStatus(status, pageable);
        }
        return ticketRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public SaaSSupportTicket getSupportTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with id " + id));
    }

    @Override
    public SaaSSupportReply createSupportReply(Long ticketId, SaaSSupportReply reply, String activeUser) {
        SaaSSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with id " + ticketId));
        
        reply.setTicket(ticket);
        reply.setUsername(activeUser);
        reply.setIsAdminReply(true); // Since it comes from Platform Admin console
        SaaSSupportReply saved = replyRepository.save(reply);
        
        ticket.setStatus("IN_PROGRESS");
        ticketRepository.save(ticket);
        
        auditService.log(activeUser, "ADD_TICKET_REPLY", "saas_support_replies", saved.getId(), null,
                "Added staff reply message to ticket: " + ticket.getTicketNumber());
        return saved;
    }

    @Override
    public SaaSSupportTicket updateTicketStatus(Long id, String status, String activeUser) {
        SaaSSupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with id " + id));
        ticket.setStatus(status);
        SaaSSupportTicket updated = ticketRepository.save(ticket);
        
        auditService.log(activeUser, "UPDATE_TICKET_STATUS", "saas_support_tickets", id, null,
                "Updated ticket status to: " + status + " for ticket: " + ticket.getTicketNumber());
        return updated;
    }

    // 12. Announcements
    @Override
    @Transactional(readOnly = true)
    public Page<SaaSAnnouncement> getAnnouncements(Pageable pageable) {
        return announcementRepository.findAll(pageable);
    }

    @Override
    public SaaSAnnouncement createAnnouncement(SaaSAnnouncement announcement, String activeUser) {
        announcement.setStatus("ACTIVE");
        SaaSAnnouncement saved = announcementRepository.save(announcement);
        auditService.log(activeUser, "CREATE_ANNOUNCEMENT", "saas_announcements", saved.getId(), null,
                "Broadcasted system announcement: " + saved.getTitle());
        return saved;
    }

    @Override
    public void deleteAnnouncement(Long id, String activeUser) {
        SaaSAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found with id " + id));
        announcementRepository.delete(announcement);
        auditService.log(activeUser, "DELETE_ANNOUNCEMENT", "saas_announcements", id, null,
                "Removed system announcement: " + announcement.getTitle());
    }

    private Optional<AppRole> resolveRoleForCompany(String roleCode, Long companyId) {
        if (roleCode == null || roleCode.isBlank()) {
            return Optional.empty();
        }
        if (companyId != null) {
            Optional<AppRole> tenantRole = roleRepository.findByCodeAndCompanyIdAndIsDeletedFalse(roleCode, companyId);
            if (tenantRole.isPresent()) {
                return tenantRole;
            }
        }
        return roleRepository.findAllByCodeAndIsDeletedFalse(roleCode).stream()
                .filter(r -> r.getCompanyId() == null)
                .findFirst();
    }

    // 14. Billing Invoices
    @Override
    @Transactional(readOnly = true)
    public Page<SaaSBillingInvoice> getBillingInvoices(Long companyId, Pageable pageable) {
        if (companyId != null) {
            return billingInvoiceRepository.findByCompanyId(companyId, pageable);
        }
        return billingInvoiceRepository.findAllByOrderByInvoiceDateDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)

    public Page<Vehicle> getVehicles(Pageable pageable) {
        return vehicleRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTrips(Pageable pageable) {
        return tripRepository.findAll(pageable);
    }
}
