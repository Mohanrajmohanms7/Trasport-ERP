package com.transport.erp.security;

import com.transport.erp.model.AppUser;
import com.transport.erp.model.Company;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.CompanyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Public auth endpoints must not run JWT parsing (stale Bearer tokens can break login)
        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/forgot-password")
                || path.equals("/api/v1/auth/reset-password")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.warn("Unable to extract username from JWT token: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Check company subscription status
                    AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username).orElse(null);
                    if (user != null && user.getCompanyId() != null) {
                        Company company = companyRepository.findById(user.getCompanyId()).orElse(null);
                        if (company != null) {
                            boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()));
                            if (!isSuperAdmin) {
                                LocalDate today = LocalDate.now();
                                boolean isExpired = (company.getSubscriptionEndDate() != null && today.isAfter(company.getSubscriptionEndDate()))
                                        || "EXPIRED".equalsIgnoreCase(company.getSubscriptionStatus());
                                
                                if (isExpired) {
                                    String path = request.getRequestURI();
                                    if (!path.contains("/api/v1/auth/renew-subscription") && !path.contains("/api/v1/auth/logout") && !path.contains("/api/v1/plans")) {
                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                        response.setContentType("application/json");
                                        response.setCharacterEncoding("UTF-8");
                                        response.getWriter().write("{\"success\":false,\"message\":\"SUBSCRIPTION_EXPIRED\",\"data\":null}");
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore invalid/stale tokens — request continues as anonymous
                logger.debug("Skipping invalid JWT: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
