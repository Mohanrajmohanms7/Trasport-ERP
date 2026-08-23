package com.transport.erp.config;

import com.transport.erp.model.AppPermission;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppSetting;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Branch;
import com.transport.erp.model.Company;
import com.transport.erp.model.FinancialYear;
import com.transport.erp.repository.AppPermissionRepository;
import com.transport.erp.repository.AppRoleRepository;
import com.transport.erp.repository.AppSettingRepository;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.FinancialYearRepository;
import com.transport.erp.service.TenantSupportingDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Seeds the minimum data required to start the application: one company, one branch,
 * the SUPER_ADMIN role with full permissions, the admin login, and supporting
 * lookups/materials (no customers / vehicles / drivers).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String COMPANY_CODE = "DEMO";
    private static final String BRANCH_CODE = "HO";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String ADMIN_USERNAME = "admin";

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final AppRoleRepository roleRepository;
    private final AppPermissionRepository permissionRepository;
    private final AppUserRepository userRepository;
    private final FinancialYearRepository financialYearRepository;
    private final AppSettingRepository appSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantSupportingDataService supportingDataService;

    @Value("${app.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.admin-password:Admin@123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("Bootstrap seeding disabled (app.bootstrap.enabled=false). Skipping.");
            return;
        }

        Company company = seedCompany();
        Branch branch = seedBranch(company);
        AppRole superAdmin = seedRoleWithPermissions();
        seedAdminUser(company, branch, superAdmin);
        seedFinancialYear(company);
        seedApplicationSettings(company);
        supportingDataService.seedForCompany(company.getId(), branch.getId());

        log.info("Bootstrap seeding complete. Login with username '{}'.", ADMIN_USERNAME);
    }

    private Company seedCompany() {
        return companyRepository.findByCodeAndIsDeletedFalse(COMPANY_CODE).orElseGet(() -> {
            Company company = new Company();
            company.setCode(COMPANY_CODE);
            company.setName("Demo Transport ERP");
            company.setDescription("Default company created at first startup");
            company.setStatus("ACTIVE");
            company.setCountry("India");
            company.setCreatedBy("SYSTEM");
            Company saved = companyRepository.save(company);
            log.info("Seeded company '{}' (id={})", saved.getName(), saved.getId());
            return saved;
        });
    }

    private Branch seedBranch(Company company) {
        return branchRepository.findByCompanyIdAndCodeAndIsDeletedFalse(company.getId(), BRANCH_CODE)
                .orElseGet(() -> {
                    Branch branch = new Branch();
                    branch.setCode(BRANCH_CODE);
                    branch.setName("Head Office");
                    branch.setDescription("Default branch created at first startup");
                    branch.setStatus("ACTIVE");
                    branch.setCompanyId(company.getId());
                    branch.setCreatedBy("SYSTEM");
                    Branch saved = branchRepository.save(branch);
                    log.info("Seeded branch '{}' (id={})", saved.getName(), saved.getId());
                    return saved;
                });
    }

    private AppRole seedRoleWithPermissions() {
        Set<AppPermission> permissions = seedPermissions();

        AppRole role = roleRepository.findAllByCodeAndIsDeletedFalse(SUPER_ADMIN_ROLE).stream()
                .findFirst()
                .orElseGet(() -> {
                    AppRole created = new AppRole();
                    created.setCode(SUPER_ADMIN_ROLE);
                    created.setName("Super Administrator");
                    created.setDescription("Full access to every module and action");
                    created.setStatus("ACTIVE");
                    created.setCreatedBy("SYSTEM");
                    return created;
                });

        // Re-attach permissions on every start so newly added permission codes are granted.
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        AppRole saved = roleRepository.save(role);
        log.info("Seeded role '{}' with {} permissions", saved.getCode(), permissions.size());
        return saved;
    }

    private Set<AppPermission> seedPermissions() {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("FULL_ACCESS", "Unrestricted access to all modules");
        definitions.put("VIEW", "View records");
        definitions.put("CREATE", "Create records");
        definitions.put("EDIT", "Edit records");
        definitions.put("DELETE", "Delete records");
        definitions.put("APPROVE", "Approve records");
        definitions.put("REJECT", "Reject records");
        definitions.put("EXPORT", "Export data to Excel or PDF");
        definitions.put("IMPORT", "Import data from Excel");
        definitions.put("PRINT", "Print documents");

        Set<AppPermission> permissions = new LinkedHashSet<>();
        definitions.forEach((code, description) -> {
            AppPermission permission = permissionRepository.findByCodeAndIsDeletedFalse(code)
                    .orElseGet(() -> {
                        AppPermission created = new AppPermission();
                        created.setCode(code);
                        created.setName(toTitle(code));
                        created.setDescription(description);
                        created.setStatus("ACTIVE");
                        created.setCreatedBy("SYSTEM");
                        return permissionRepository.save(created);
                    });
            permissions.add(permission);
        });
        return permissions;
    }

    private void seedAdminUser(Company company, Branch branch, AppRole superAdmin) {
        if (userRepository.findByUsernameAndIsDeletedFalse(ADMIN_USERNAME).isPresent()) {
            log.info("Admin user '{}' already exists. Skipping.", ADMIN_USERNAME);
            return;
        }

        AppUser admin = new AppUser();
        admin.setCode("EMP001");
        admin.setName("System Administrator");
        admin.setDescription("Default administrator created at first startup");
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEmail("admin@transport-erp.local");
        admin.setStatus("ACTIVE");
        admin.setCompanyId(company.getId());
        admin.setBranchId(branch.getId());
        admin.setCreatedBy("SYSTEM");
        admin.getRoles().add(superAdmin);

        userRepository.save(admin);
        log.info("Seeded admin user '{}' with role {}", ADMIN_USERNAME, SUPER_ADMIN_ROLE);
    }

    private void seedFinancialYear(Company company) {
        // Indian financial year runs April 1 to March 31.
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= Month.APRIL.getValue() ? today.getYear() : today.getYear() - 1;
        LocalDate start = LocalDate.of(startYear, Month.APRIL, 1);
        LocalDate end = LocalDate.of(startYear + 1, Month.MARCH, 31);
        String code = "FY" + startYear + "-" + (startYear + 1);

        if (financialYearRepository.findByCodeAndCompanyIdAndIsDeletedFalse(code, company.getId()).isPresent()) {
            return;
        }

        FinancialYear financialYear = new FinancialYear();
        financialYear.setCode(code);
        financialYear.setName(startYear + "-" + (startYear + 1));
        financialYear.setDescription("Default financial year created at first startup");
        financialYear.setStartDate(start);
        financialYear.setEndDate(end);
        financialYear.setIsDefault(true);
        financialYear.setStatus("ACTIVE");
        financialYear.setCompanyId(company.getId());
        financialYear.setCreatedBy("SYSTEM");

        financialYearRepository.save(financialYear);
        log.info("Seeded financial year '{}'", code);
    }

    private void seedApplicationSettings(Company company) {
        Map<String, String[]> settings = new LinkedHashMap<>();
        // key -> { value, description }
        settings.put("DEFAULT_CURRENCY", new String[]{"INR", "Default transaction currency"});
        settings.put("CURRENCY_SYMBOL", new String[]{"\u20B9", "Symbol rendered on documents"});
        settings.put("DEFAULT_COUNTRY", new String[]{"India", "Default country for addresses"});
        settings.put("DEFAULT_TIMEZONE", new String[]{"Asia/Kolkata", "Application timezone"});
        settings.put("DATE_FORMAT", new String[]{"dd/MM/yyyy", "Display date format"});
        settings.put("DEFAULT_GST_PERCENT", new String[]{"18", "Default GST percentage"});
        settings.put("SETUP_COMPLETED", new String[]{"false", "Set to true once the Setup Wizard is finished"});

        settings.forEach((key, meta) -> {
            if (appSettingRepository.findByKeyNameAndCompanyIdAndIsDeletedFalse(key, company.getId()).isPresent()) {
                return;
            }
            AppSetting setting = new AppSetting();
            setting.setKeyName(key);
            setting.setValueData(meta[0]);
            setting.setCode(key);
            setting.setName(toTitle(key));
            setting.setDescription(meta[1]);
            setting.setStatus("ACTIVE");
            setting.setCompanyId(company.getId());
            setting.setCreatedBy("SYSTEM");
            appSettingRepository.save(setting);
        });
    }

    private String toTitle(String code) {
        String[] parts = code.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
