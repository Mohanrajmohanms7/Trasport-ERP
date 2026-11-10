package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class DriverService {

    @org.springframework.beans.factory.annotation.Autowired
    private BranchDefaults branchDefaults;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private BusinessDependencyValidationService validationService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AuditService auditService;

    private static final Set<String> LINK_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");

    /** List with an optional status filter (dropdowns ask for ACTIVE only); no status = driver list as before. */
    public Page<Driver> getAll(Long companyId, String search, String status, Pageable pageable) {
        if (status == null || status.isBlank()) return getAll(companyId, search, pageable);
        return driverRepository.searchByStatus(companyId, search == null ? "" : search.trim(), status.trim().toUpperCase(), pageable);
    }

    public Page<Driver> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return driverRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return driverRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Driver> getById(Long id) {
        return driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .map(d -> {
                    tenantAccess.assertOwned(d.getCompanyId());
                    attachUserName(d);
                    return d;
                });
    }

    @Transactional
    public Driver create(Driver driver) {
        Long companyId = tenantAccess.resolveCompanyId(driver.getCompanyId());
        driver.setCompanyId(companyId);
        // Vehicles and drivers are based at a branch: the one chosen (same company), else the user's branch,
        // else the head office — so branch lists, filters and the Branch Master counts include them.
        driver.setBranchId(branchDefaults.resolve(companyId, driver.getBranchId()));
        if (driverRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, driver.getCode()).isPresent()) {
            throw new IllegalArgumentException("Driver code already exists in this company: " + driver.getCode());
        }
        if (driverRepository.findByCompanyIdAndLicenseNumberAndIsDeletedFalse(companyId, driver.getLicenseNumber()).isPresent()) {
            throw new IllegalArgumentException("Driver license already exists in this company: " + driver.getLicenseNumber());
        }
        driver.setIsDeleted(false);
        return driverRepository.save(driver);
    }

    @Transactional
    public Driver update(Long id, Driver driverDetails) {
        Driver driver = driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        tenantAccess.assertOwned(driver.getCompanyId());

        Optional<Driver> existingCode = driverRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                driver.getCompanyId(), driverDetails.getCode());
        if (existingCode.isPresent() && !existingCode.get().getId().equals(id)) {
            throw new IllegalArgumentException("Driver code already exists: " + driverDetails.getCode());
        }

        Optional<Driver> existingLicense = driverRepository.findByCompanyIdAndLicenseNumberAndIsDeletedFalse(
                driver.getCompanyId(), driverDetails.getLicenseNumber());
        if (existingLicense.isPresent() && !existingLicense.get().getId().equals(id)) {
            throw new IllegalArgumentException("Driver license already exists in this company: " + driverDetails.getLicenseNumber());
        }

        driver.setCode(driverDetails.getCode());
        driver.setName(driverDetails.getName());
        driver.setDescription(driverDetails.getDescription());
        driver.setStatus(driverDetails.getStatus());
        driver.setLicenseNumber(driverDetails.getLicenseNumber());
        driver.setLicenseExpiryDate(driverDetails.getLicenseExpiryDate());
        driver.setPhoneNumber(driverDetails.getPhoneNumber());
        if (driverDetails.getBranchId() != null) {
            driver.setBranchId(driverDetails.getBranchId());
        }

        return driverRepository.save(driver);
    }

    @Transactional
    public void delete(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        tenantAccess.assertOwned(driver.getCompanyId());

        validationService.validateDriverDelete(id);

        driver.setIsDeleted(true);
        driverRepository.save(driver);
    }

    @Transactional
    public Driver toggleStatus(Long id) {
        Driver driver = driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        tenantAccess.assertOwned(driver.getCompanyId());
        driver.setStatus("ACTIVE".equals(driver.getStatus()) ? "INACTIVE" : "ACTIVE");
        return driverRepository.save(driver);
    }

    @Transactional
    public Driver linkAppUser(Long driverId, Long appUserId, String username) {
        AppUser actor = tenantAccess.requireCurrentUser();
        assertLinkAccess(actor);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> invalidLink("Driver was not found."));
        if (Boolean.TRUE.equals(driver.getIsDeleted())) {
            throw invalidLink("Driver is deleted.");
        }
        tenantAccess.assertCompanyAccess(driver.getCompanyId());
        assertDriverBranch(driver, actor);
        if (appUserId == null) {
            if (driver.getAppUserId() != null) {
                driver.setAppUserId(null);
                driver.setAppUserName(null);
                driver.setUpdatedBy(username);
                Driver saved = driverRepository.save(driver);
                auditService.log(username, "DRIVER_APP_USER_UNLINKED", "drivers", saved.getId(), null,
                        "driverId=" + saved.getId());
                return saved;
            }
            return driver;
        }
        if (appUserId.equals(driver.getAppUserId())) {
            attachUserName(driver);
            return driver;
        }
        AppUser account = userRepository.findWithRolesById(appUserId)
                .orElseThrow(() -> invalidLink("App user was not found."));
        if (Boolean.TRUE.equals(account.getIsDeleted())) {
            throw invalidLink("App user is deleted.");
        }
        if (account.getCompanyId() == null || !account.getCompanyId().equals(driver.getCompanyId())) {
            throw invalidLink("App user must belong to the same company as the driver.");
        }
        boolean driverRole = account.getRoles() != null && account.getRoles().stream()
                .map(AppRole::getCode)
                .anyMatch("DRIVER"::equals);
        if (!driverRole) {
            throw invalidLink("App user must have the DRIVER role.");
        }
        if (driverRepository.findByAppUserIdAndIdNotAndIsDeletedFalse(appUserId, driver.getId()).isPresent()) {
            throw invalidLink("App user is already linked to another driver.");
        }
        driver.setAppUserId(account.getId());
        driver.setAppUserName(account.getUsername());
        driver.setUpdatedBy(username);
        Driver saved = driverRepository.save(driver);
        auditService.log(username, "DRIVER_APP_USER_LINKED", "drivers", saved.getId(), null,
                "driverId=" + saved.getId() + ", appUserId=" + account.getId());
        return saved;
    }

    private void assertLinkAccess(AppUser actor) {
        if (tenantAccess.isSuperAdmin(actor)) {
            return;
        }
        if (actor.getRoles() == null || actor.getRoles().stream().map(AppRole::getCode).noneMatch(LINK_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: linking a driver requires COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private void assertDriverBranch(Driver driver, AppUser actor) {
        if (tenantAccess.isSuperAdmin(actor)) {
            return;
        }
        if (actor.getBranchId() != null && !tenantAccess.isCompanyWideAdmin(actor) && driver.getBranchId() != null
                && !actor.getBranchId().equals(driver.getBranchId())) {
            throw new AccessDeniedException("Access denied: Driver belongs to another branch.");
        }
    }

    private void attachUserName(Driver driver) {
        if (driver.getAppUserId() == null) {
            driver.setAppUserName(null);
            return;
        }
        userRepository.findWithRolesById(driver.getAppUserId())
                .ifPresent(user -> driver.setAppUserName(user.getUsername()));
    }

    private BusinessValidationException invalidLink(String message) {
        return new BusinessValidationException(
                "Invalid Driver Login",
                "DRIVER_APP_USER_INVALID",
                "DRIVER_APP_USER_INVALID: " + message,
                "Choose an active DRIVER user in the same company that is not already linked.");
    }

    // ------------------------------------------------------------------ driver login (one step from Driver Master)
    @Autowired private UserService userService;
    @Autowired private com.transport.erp.repository.AppRoleRepository roleRepoForLogin;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /** Creates a DRIVER login for this driver (same company / branch) and links it, in one step. */
    @Transactional
    public Driver createLogin(Long driverId, String username, String password, String actor) {
        Driver driver = loginDriver(driverId);
        if (driver.getAppUserId() != null) throw invalidLink("This driver already has a login. Reset its password instead.");
        com.transport.erp.model.AppUser u = new com.transport.erp.model.AppUser();
        u.setUsername(username == null ? null : username.trim());
        u.setPassword(password);
        u.setName(driver.getName());
        u.setCode("DRV-" + driver.getId());
        u.setCompanyId(driver.getCompanyId());
        u.setBranchId(driver.getBranchId());
        u.setStatus("ACTIVE");
        // The DRIVER role record (the company's own, else the global one) — roles are resolved by id.
        com.transport.erp.model.AppRole driverRole = roleRepoForLogin.findByCodeAndCompanyIdAndIsDeletedFalse("DRIVER", driver.getCompanyId())
                .or(() -> roleRepoForLogin.findFirstByCodeAndCompanyIdIsNullAndIsDeletedFalse("DRIVER"))
                .orElseThrow(() -> invalidLink("The DRIVER role is missing. Ask the platform admin to set up roles."));
        com.transport.erp.model.AppRole ref = new com.transport.erp.model.AppRole();
        ref.setId(driverRole.getId());
        u.setRoles(new java.util.HashSet<>(java.util.Set.of(ref)));
        com.transport.erp.model.AppUser saved = userService.createUser(u, actor);
        return linkAppUser(driverId, saved.getId(), actor);
    }

    /** New password for this driver's login; signs the driver out everywhere. */
    @Transactional
    public void resetLoginPassword(Long driverId, String password, String actor) {
        com.transport.erp.model.AppUser u = loginUser(loginDriver(driverId));
        if (password == null || password.length() < 8) throw invalidLink("Password must be at least 8 characters.");
        u.setPassword(passwordEncoder.encode(password));
        u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
        userRepository.save(u);
        auditService.log(actor, "DRIVER_LOGIN_PASSWORD_RESET", "app_users", u.getId(), null, "Password reset for driver login " + u.getUsername());
    }

    /** Enable / disable this driver's login (disable signs the driver out at once). */
    @Transactional
    public void setLoginActive(Long driverId, boolean active, String actor) {
        com.transport.erp.model.AppUser u = loginUser(loginDriver(driverId));
        u.setStatus(active ? "ACTIVE" : "INACTIVE");
        if (!active) u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
        userRepository.save(u);
        auditService.log(actor, active ? "DRIVER_LOGIN_ENABLED" : "DRIVER_LOGIN_DISABLED", "app_users", u.getId(), null, "Driver login " + u.getUsername());
    }

    private Driver loginDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId).filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> invalidLink("Driver not found."));
        tenantAccess.assertCompanyAccess(driver.getCompanyId());
        return driver;
    }

    private com.transport.erp.model.AppUser loginUser(Driver driver) {
        if (driver.getAppUserId() == null) throw invalidLink("This driver has no login yet. Create one first.");
        return userRepository.findById(driver.getAppUserId()).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> invalidLink("The linked login was not found."));
    }
}
