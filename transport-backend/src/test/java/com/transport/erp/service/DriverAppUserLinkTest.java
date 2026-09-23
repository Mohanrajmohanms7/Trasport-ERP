package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DriverAppUserLinkTest {

    @Mock private DriverRepository driverRepository;
    @Mock private TenantAccessService tenantAccess;
    @Mock private BusinessDependencyValidationService validationService;
    @Mock private AppUserRepository userRepository;
    @Mock private AuditService auditService;
    @InjectMocks private DriverService service;

    private AppUser admin;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin", 1L, 1L, "COMPANY_ADMIN");
        when(tenantAccess.requireCurrentUser()).thenReturn(admin);
        when(tenantAccess.isSuperAdmin(admin)).thenReturn(false);
        doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (!Long.valueOf(1L).equals(companyId)) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return null;
        }).when(tenantAccess).assertCompanyAccess(any());
    }

    @Test
    void sameCompanyDriverUserLinks() {
        Driver driver = driver(8L, 1L);
        AppUser account = user(4L, "ram", 1L, 1L, "DRIVER");
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        when(userRepository.findWithRolesById(4L)).thenReturn(Optional.of(account));
        when(driverRepository.findByAppUserIdAndIdNotAndIsDeletedFalse(4L, 8L)).thenReturn(Optional.empty());
        when(driverRepository.save(driver)).thenReturn(driver);

        Driver saved = service.linkAppUser(8L, 4L, "admin");

        assertEquals(4L, saved.getAppUserId());
        verify(auditService).log(eq("admin"), eq("DRIVER_APP_USER_LINKED"), eq("drivers"), eq(8L), isNull(), any());
    }

    @Test
    void nonDriverUserIsRejected() {
        Driver driver = driver(8L, 1L);
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        when(userRepository.findWithRolesById(4L)).thenReturn(Optional.of(user(4L, "clerk", 1L, 1L, "OPERATOR")));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.linkAppUser(8L, 4L, "admin"));
        assertEquals("DRIVER_APP_USER_INVALID", ex.getErrorCode());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void deletedUserIsRejected() {
        Driver driver = driver(8L, 1L);
        AppUser account = user(4L, "ram", 1L, 1L, "DRIVER");
        account.setIsDeleted(true);
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        when(userRepository.findWithRolesById(4L)).thenReturn(Optional.of(account));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.linkAppUser(8L, 4L, "admin"));
        assertEquals("DRIVER_APP_USER_INVALID", ex.getErrorCode());
    }

    @Test
    void deletedDriverIsRejected() {
        Driver driver = driver(8L, 1L);
        driver.setIsDeleted(true);
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.linkAppUser(8L, 4L, "admin"));
        assertEquals("DRIVER_APP_USER_INVALID", ex.getErrorCode());
    }

    @Test
    void crossCompanyUserIsRejected() {
        Driver driver = driver(8L, 1L);
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        when(userRepository.findWithRolesById(4L)).thenReturn(Optional.of(user(4L, "ram", 2L, null, "DRIVER")));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.linkAppUser(8L, 4L, "admin"));
        assertEquals("DRIVER_APP_USER_INVALID", ex.getErrorCode());
    }

    @Test
    void crossCompanyDriverIsRejected() {
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver(8L, 2L)));
        assertThrows(AccessDeniedException.class, () -> service.linkAppUser(8L, 4L, "admin"));
    }

    @Test
    void userAlreadyLinkedIsRejected() {
        Driver driver = driver(8L, 1L);
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        when(userRepository.findWithRolesById(4L)).thenReturn(Optional.of(user(4L, "ram", 1L, 1L, "DRIVER")));
        when(driverRepository.findByAppUserIdAndIdNotAndIsDeletedFalse(4L, 8L)).thenReturn(Optional.of(driver(9L, 1L)));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.linkAppUser(8L, 4L, "admin"));
        assertEquals("DRIVER_APP_USER_INVALID", ex.getErrorCode());
    }

    @Test
    void unlinkClearsTheLogin() {
        Driver driver = driver(8L, 1L);
        driver.setAppUserId(4L);
        when(driverRepository.findById(8L)).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);
        Driver saved = service.linkAppUser(8L, null, "admin");
        assertNull(saved.getAppUserId());
        verify(auditService).log(eq("admin"), eq("DRIVER_APP_USER_UNLINKED"), eq("drivers"), eq(8L), isNull(), any());
    }

    private Driver driver(Long id, Long companyId) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setCompanyId(companyId);
        driver.setBranchId(1L);
        driver.setIsDeleted(false);
        return driver;
    }

    private AppUser user(Long id, String username, Long companyId, Long branchId, String roleCode) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setCompanyId(companyId);
        user.setBranchId(branchId);
        user.setIsDeleted(false);
        AppRole role = new AppRole();
        role.setCode(roleCode);
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
