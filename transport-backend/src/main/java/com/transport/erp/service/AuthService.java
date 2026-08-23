package com.transport.erp.service;

import com.transport.erp.model.AppUser;
import com.transport.erp.model.LoginHistory;
import com.transport.erp.model.RefreshToken;
import com.transport.erp.model.Company;
import com.transport.erp.model.SaaSPlan;
import com.transport.erp.model.SaaSTenantSubscription;
import com.transport.erp.model.SaaSLicense;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.LoginHistoryRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.SaaSPlanRepository;
import com.transport.erp.repository.SaaSTenantSubscriptionRepository;
import com.transport.erp.repository.SaaSLicenseRepository;
import com.transport.erp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SaaSPlanRepository planRepository;

    @Autowired
    private SaaSTenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private SaaSLicenseRepository licenseRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> login(String username, String password, String ipAddress, String userAgent) {
        Map<String, Object> response = new HashMap<>();
        final String cleanUsername = (username != null) ? username.trim() : null;
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanUsername, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            AppUser user = userRepository.findByUsernameAndIsDeletedFalse(cleanUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + cleanUsername));

            if (!"ACTIVE".equals(user.getStatus())) {
                throw new IllegalArgumentException("User account is inactive.");
            }

            boolean subscriptionExpired = false;
            if (user.getCompanyId() != null) {
                Company company = companyRepository.findById(user.getCompanyId()).orElse(null);
                if (company != null) {
                    boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()));
                    if (!isSuperAdmin) {
                        LocalDate today = LocalDate.now();
                        if ((company.getSubscriptionEndDate() != null && today.isAfter(company.getSubscriptionEndDate()))
                                || "EXPIRED".equalsIgnoreCase(company.getSubscriptionStatus())) {
                            subscriptionExpired = true;
                        }
                        
                        if (subscriptionExpired) {
                            boolean isCompanyAdmin = user.getRoles().stream().anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()));
                            if (!isCompanyAdmin) {
                                throw new IllegalArgumentException("Your company subscription has expired. Please contact your administrator.");
                            }
                        }
                    }
                }
            }

            Map<String, Object> tokenClaims = new HashMap<>();
            if (user.getCompanyId() != null) {
                tokenClaims.put("companyId", user.getCompanyId());
            }
            if (user.getBranchId() != null) {
                tokenClaims.put("branchId", user.getBranchId());
            }
            String jwtToken = jwtUtil.generateToken(tokenClaims, userDetails.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            // Write Login History Success
            logLoginHistory(user, cleanUsername, ipAddress, userAgent, "SUCCESS");
            auditService.log(cleanUsername, "USER_LOGIN", "app_users", user.getId(), ipAddress, "User logged in successfully");

            response.put("token", jwtToken);
            response.put("refreshToken", refreshToken.getToken());
            response.put("username", user.getUsername());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles().stream().map(r -> r.getCode()).toList());
            response.put("companyId", user.getCompanyId());
            response.put("branchId", user.getBranchId());
            response.put("subscriptionExpired", subscriptionExpired);

            return response;
        } catch (BadCredentialsException e) {
            Optional<AppUser> userOpt = userRepository.findByUsernameAndIsDeletedFalse(cleanUsername);
            userOpt.ifPresent(appUser -> logLoginHistory(appUser, cleanUsername, ipAddress, userAgent, "FAILED"));
            throw e;
        } catch (IllegalArgumentException e) {
            Optional<AppUser> userOpt = userRepository.findByUsernameAndIsDeletedFalse(cleanUsername);
            userOpt.ifPresent(appUser -> logLoginHistory(appUser, cleanUsername, ipAddress, userAgent, "FAILED"));
            throw e;
        } catch (Exception e) {
            Optional<AppUser> userOpt = userRepository.findByUsernameAndIsDeletedFalse(cleanUsername);
            userOpt.ifPresent(appUser -> logLoginHistory(appUser, cleanUsername, ipAddress, userAgent, "FAILED"));
            throw new IllegalArgumentException("Invalid username or password: " + e.getMessage());
        }
    }

    @Transactional
    public void logout(String username, String ipAddress) {
        AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        refreshTokenService.deleteByUserId(user.getId());
        auditService.log(username, "USER_LOGOUT", "app_users", user.getId(), ipAddress, "User logged out successfully");
    }

    @Transactional
    public Map<String, String> refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    Map<String, Object> tokenClaims = new HashMap<>();
                    if (user.getCompanyId() != null) {
                        tokenClaims.put("companyId", user.getCompanyId());
                    }
                    if (user.getBranchId() != null) {
                        tokenClaims.put("branchId", user.getBranchId());
                    }
                    String token = jwtUtil.generateToken(tokenClaims, user.getUsername());
                    Map<String, String> tokens = new HashMap<>();
                    tokens.put("token", token);
                    tokens.put("refreshToken", requestRefreshToken);
                    return tokens;
                })
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is not in database!"));
    }

    @Transactional
    public void forgotPassword(String email) {
        // Find user by email
        AppUser user = userRepository.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()) && !u.getIsDeleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active user associated with email: " + email));

        // In real system, send email with token. Here we create a log entry and placeholder.
        auditService.log(user.getUsername(), "FORGOT_PASSWORD_REQUEST", "app_users", user.getId(), null,
                "Password reset requested for email: " + email);
    }

    @Transactional
    public void resetPassword(String username, String token, String newPassword) {
        // Find user
        AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Password policy checks
        validatePasswordPolicy(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditService.log(username, "PASSWORD_RESET_SUCCESS", "app_users", user.getId(), null,
                "Password reset successfully with token validation");
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword, String ipAddress) {
        AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password does not match current password.");
        }

        validatePasswordPolicy(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditService.log(username, "PASSWORD_CHANGE", "app_users", user.getId(), ipAddress, "User changed password successfully");
    }

    public AppUser getProfile(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public AppUser updateProfile(String username, AppUser profileDetails) {
        AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String requestedUsername = profileDetails.getUsername() == null
                ? null
                : profileDetails.getUsername().trim();
        if (requestedUsername != null && !requestedUsername.isBlank()
                && !requestedUsername.equalsIgnoreCase(user.getUsername())) {
            if (requestedUsername.length() < 3 || requestedUsername.length() > 50) {
                throw new IllegalArgumentException("Username must be between 3 and 50 characters.");
            }
            if (!requestedUsername.matches("^[a-zA-Z0-9._-]+$")) {
                throw new IllegalArgumentException("Username may only contain letters, numbers, dot, underscore, and hyphen.");
            }
            if (userRepository.existsByUsernameAndIsDeletedFalse(requestedUsername)) {
                throw new IllegalArgumentException("Username already taken. Please choose another.");
            }
            user.setUsername(requestedUsername);
        }

        user.setName(profileDetails.getName());
        user.setEmail(profileDetails.getEmail());
        user.setPhone(profileDetails.getPhone());
        user.setDescription(profileDetails.getDescription());

        return userRepository.save(user);
    }

    private void logLoginHistory(AppUser user, String username, String ipAddress, String userAgent, String status) {
        LoginHistory history = new LoginHistory();
        history.setUser(user);
        history.setUsername(username);
        history.setIpAddress(ipAddress);
        history.setUserAgent(userAgent);
        history.setStatus(status);
        history.setLoginTime(LocalDateTime.now());
        loginHistoryRepository.save(history);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true; // Special characters
        }

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
    }

    @Transactional
    public Company renewSubscription(String username, Long planId, String paymentMethod) {
        AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        
        if (user.getCompanyId() == null) {
            throw new IllegalArgumentException("User is not associated with any company.");
        }
        
        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + user.getCompanyId()));
        
        SaaSPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found."));
        
        LocalDate today = LocalDate.now();
        LocalDate startDate = today;
        if (company.getSubscriptionEndDate() != null && !today.isAfter(company.getSubscriptionEndDate()) 
                && "ACTIVE".equalsIgnoreCase(company.getSubscriptionStatus())) {
            startDate = company.getSubscriptionEndDate().plusDays(1);
        }
        
        LocalDate endDate = startDate.plusMonths(1);
        String codeUpper = plan.getCode().toUpperCase();
        String periodUpper = plan.getBillingPeriod().toUpperCase();
        if (codeUpper.contains("YEAR") || "YEARLY".equals(periodUpper)) {
            endDate = startDate.plusYears(1);
        } else if (codeUpper.contains("QUARTER") || codeUpper.contains("3_MONTH")) {
            endDate = startDate.plusMonths(3);
        } else if (codeUpper.contains("TRIAL")) {
            endDate = startDate.plusDays(14);
        }
        
        // Update company properties
        company.setSubscriptionPlan(plan);
        company.setSubscriptionStartDate(startDate);
        company.setSubscriptionEndDate(endDate);
        company.setSubscriptionRenewalDate(endDate);
        company.setSubscriptionStatus("ACTIVE");
        company.setMaxUsers(plan.getMaxUsers());
        company.setMaxVehicles(plan.getMaxVehicles());
        company.setStatus("ACTIVE");
        Company savedCompany = companyRepository.save(company);
        
        // Record SaaSTenantSubscription history
        SaaSTenantSubscription sub = new SaaSTenantSubscription();
        sub.setCompanyId(company.getId());
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        sub.setStartDate(startDate);
        sub.setEndDate(endDate);
        sub.setAmountPaid(plan.getPrice());
        sub.setPaymentMethod(paymentMethod != null ? paymentMethod : "UPI");
        sub.setPaymentStatus("PAID");
        sub.setCreatedBy(username);
        subscriptionRepository.save(sub);
        
        // Sync active license key expiry
        List<SaaSLicense> licenses = licenseRepository.findAll().stream()
                .filter(l -> l.getCompanyId().equals(company.getId()) && "ACTIVE".equals(l.getStatus()))
                .toList();
        for (SaaSLicense lic : licenses) {
            lic.setExpiryDate(endDate);
            lic.setMaxUsers(plan.getMaxUsers());
            lic.setMaxVehicles(plan.getMaxVehicles());
            licenseRepository.save(lic);
        }
        
        auditService.log(username, "RENEW_SUBSCRIPTION", "companies", company.getId(), null,
                "Renewed subscription to plan: " + plan.getName() + " until " + endDate);
                
        return savedCompany;
    }

    @Transactional(readOnly = true)
    public List<SaaSPlan> getActivePlans() {
        return planRepository.findAll().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()) && !p.getIsDeleted())
                .toList();
    }
}
