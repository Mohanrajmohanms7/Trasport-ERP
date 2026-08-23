package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Company;
import com.transport.erp.model.SaaSPlan;
import com.transport.erp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class ForgotRequest {
        @NotBlank(message = "Email is required")
        private String email;
    }

    @Data
    public static class ResetRequest {
        @NotBlank(message = "Username is required")
        private String username;
        @NotBlank(message = "Token is required")
        private String token;
        @NotBlank(message = "New password is required")
        private String newPassword;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Old password is required")
        private String oldPassword;
        @NotBlank(message = "New password is required")
        private String newPassword;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        Map<String, Object> data = authService.login(request.getUsername(), request.getPassword(), ipAddress, userAgent);
        return ApiResponse.success(data, "Login successful");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String ipAddress = servletRequest.getRemoteAddr();
        authService.logout(username, ipAddress);
        return ApiResponse.success(null, "Logout successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest request) {
        Map<String, String> data = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(data, "Token refreshed successfully");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotRequest request) {
        authService.forgotPassword(request.getEmail());
        return ApiResponse.success(null, "Password reset instruction registered successfully");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetRequest request) {
        authService.resetPassword(request.getUsername(), request.getToken(), request.getNewPassword());
        return ApiResponse.success(null, "Password reset successfully");
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String ipAddress = servletRequest.getRemoteAddr();
        authService.changePassword(username, request.getOldPassword(), request.getNewPassword(), ipAddress);
        return ApiResponse.success(null, "Password changed successfully");
    }

    @GetMapping("/profile")
    public ApiResponse<AppUser> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser profile = authService.getProfile(username);
        return ApiResponse.success(profile, "User profile fetched successfully");
    }

    @PutMapping("/profile")
    public ApiResponse<AppUser> updateProfile(@RequestBody AppUser profileDetails) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser updated = authService.updateProfile(username, profileDetails);
        return ApiResponse.success(updated, "User profile updated successfully");
    }

    @PostMapping("/renew-subscription")
    public ApiResponse<Company> renewSubscription(
            @RequestParam Long planId,
            @RequestParam(required = false) String paymentMethod) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Company company = authService.renewSubscription(username, planId, paymentMethod);
        return ApiResponse.success(company, "Subscription renewed successfully");
    }

    @GetMapping("/plans")
    public ApiResponse<List<SaaSPlan>> getActivePlans() {
        List<SaaSPlan> plans = authService.getActivePlans();
        return ApiResponse.success(plans, "Active plans fetched successfully");
    }
}
