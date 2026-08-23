package com.transport.erp.service;

import com.transport.erp.dto.SetupStatusResponse;
import com.transport.erp.model.AppSetting;
import com.transport.erp.model.AppUser;
import com.transport.erp.repository.AppSettingRepository;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.CustomerRepository;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.MaterialRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SetupService {

    private static final String SETUP_COMPLETED_KEY = "SETUP_COMPLETED";

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final MaterialRepository materialRepository;
    private final AppSettingRepository appSettingRepository;
    private final TenantAccessService tenantAccess;
    private final TenantSupportingDataService supportingDataService;
    private final javax.sql.DataSource dataSource;

    @Transactional(readOnly = true)
    public SetupStatusResponse getStatus() {
        Long companyId = tenantAccess.resolveCompanyId(null);

        long vehicles = vehicleRepository.findByCompanyIdAndIsDeletedFalse(companyId, Pageable.unpaged()).getTotalElements();
        long drivers = driverRepository.findByCompanyIdAndIsDeletedFalse(companyId, Pageable.unpaged()).getTotalElements();
        long customers = customerRepository.findByCompanyIdAndIsDeletedFalse(companyId, Pageable.unpaged()).getTotalElements();
        long materials = materialRepository.findByCompanyIdAndIsDeletedFalse(companyId, Pageable.unpaged()).getTotalElements();

        boolean completed = appSettingRepository
                .findByKeyNameAndCompanyIdAndIsDeletedFalse(SETUP_COMPLETED_KEY, companyId)
                .map(setting -> Boolean.parseBoolean(setting.getValueData()))
                .orElse(false);

        return SetupStatusResponse.builder()
                .setupCompleted(completed)
                .hasBusinessData(vehicles > 0 || drivers > 0 || customers > 0 || materials > 0)
                .companyCount(companyRepository.count())
                .branchCount(branchRepository.count())
                .vehicleCount(vehicles)
                .driverCount(drivers)
                .customerCount(customers)
                .materialCount(materials)
                .build();
    }

    @Transactional
    public SetupStatusResponse completeSetup() {
        Long companyId = tenantAccess.resolveCompanyId(null);

        AppSetting setting = appSettingRepository
                .findByKeyNameAndCompanyIdAndIsDeletedFalse(SETUP_COMPLETED_KEY, companyId)
                .orElseGet(() -> {
                    AppSetting created = new AppSetting();
                    created.setKeyName(SETUP_COMPLETED_KEY);
                    created.setCode(SETUP_COMPLETED_KEY);
                    created.setName("Setup Completed");
                    created.setDescription("Set to true once the Setup Wizard is finished");
                    created.setStatus("ACTIVE");
                    created.setCompanyId(companyId);
                    created.setCreatedBy("SYSTEM");
                    created.setIsDeleted(false);
                    return created;
                });

        setting.setValueData("true");
        setting.setUpdatedBy("SYSTEM");
        appSettingRepository.save(setting);

        return getStatus();
    }

    /**
     * Loads lookups + sample materials/quarry for the logged-in company.
     * Does NOT create customers / vehicles / drivers — enter those manually (1 each is enough).
     */
    @Transactional
    public Map<String, Object> seedSupportingExampleData() {
        AppUser user = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = user.getBranchId() != null ? user.getBranchId() : 1L;
        return supportingDataService.seedForCompany(companyId, branchId);
    }

    @Transactional
    public void seedDemoData() {
        org.springframework.jdbc.datasource.init.ResourceDatabasePopulator populator =
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
        populator.addScript(new org.springframework.core.io.ClassPathResource("db/demo/reset_db.sql"));
        populator.addScript(new org.springframework.core.io.ClassPathResource("db/demo/seed_demo_data.sql"));
        populator.addScript(new org.springframework.core.io.ClassPathResource("db/demo/reset_sequences.sql"));
        try (java.sql.Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed demo database: " + e.getMessage(), e);
        }
    }
}
