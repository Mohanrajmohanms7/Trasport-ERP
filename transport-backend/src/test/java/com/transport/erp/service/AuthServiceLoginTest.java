package com.transport.erp.service;

import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.RefreshToken;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.LoginHistoryRepository;
import com.transport.erp.repository.SaaSLicenseRepository;
import com.transport.erp.repository.SaaSPlanRepository;
import com.transport.erp.repository.SaaSTenantSubscriptionRepository;
import com.transport.erp.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AppUserRepository userRepository;
    @Mock private LoginHistoryRepository loginHistoryRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private SaaSPlanRepository planRepository;
    @Mock private SaaSTenantSubscriptionRepository subscriptionRepository;
    @Mock private SaaSLicenseRepository licenseRepository;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private AppUser activeUser;

    @BeforeEach
    void setUp() {
        AppRole role = new AppRole();
        role.setCode("SUPER_ADMIN");

        activeUser = new AppUser();
        activeUser.setId(1L);
        activeUser.setUsername("superadmin");
        activeUser.setName("Platform Super Admin");
        activeUser.setEmail("superadmin@test.local");
        activeUser.setStatus("ACTIVE");
        activeUser.setCompanyId(1L);
        activeUser.setBranchId(1L);
        activeUser.setRoles(Set.of(role));
    }

    @Test
    void loginReturnsTokenAndRolesForValidCredentials() {
        Authentication authentication = mock(Authentication.class);
        User principal = new User("superadmin", "encoded", List.of());
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsernameAndIsDeletedFalse("superadmin"))
                .thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateToken(principal)).thenReturn("jwt-token");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        Map<String, Object> result = authService.login("superadmin", "Super@123", "127.0.0.1", "test-agent");

        assertEquals("jwt-token", result.get("token"));
        assertEquals("refresh-token", result.get("refreshToken"));
        assertEquals("superadmin", result.get("username"));
        assertTrue(((List<?>) result.get("roles")).contains("SUPER_ADMIN"));
        verify(loginHistoryRepository).save(any());
        verify(auditService).log(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void loginRethrowsBadCredentialsWithoutWrappingAsValidationError() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad creds"));
        when(userRepository.findByUsernameAndIsDeletedFalse("superadmin"))
                .thenReturn(Optional.of(activeUser));

        assertThrows(BadCredentialsException.class,
                () -> authService.login("superadmin", "wrong", "127.0.0.1", "test-agent"));
        verify(loginHistoryRepository).save(any());
    }

    @Test
    void loginRejectsInactiveUser() {
        activeUser.setStatus("INACTIVE");
        Authentication authentication = mock(Authentication.class);
        User principal = new User("superadmin", "encoded", List.of());
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsernameAndIsDeletedFalse("superadmin"))
                .thenReturn(Optional.of(activeUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("superadmin", "Super@123", "127.0.0.1", "test-agent"));
        assertEquals("User account is inactive.", ex.getMessage());
    }
}
