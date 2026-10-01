package com.transport.erp.features;

import com.transport.erp.model.AppUser;
import com.transport.erp.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks API calls to features the client's subscription does not include (HTTP 403 FEATURE_DISABLED).
 * Runs after authentication; platform admins and auth / platform endpoints are never blocked.
 * Created in SecurityConfig (not a @Component) so it runs exactly once, inside the security chain.
 */
public class FeatureAccessFilter extends OncePerRequestFilter {

    private final FeatureAccessService features;
    private final AppUserRepository users;

    public FeatureAccessFilter(FeatureAccessService features, AppUserRepository users) {
        this.features = features;
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return !p.startsWith("/api/v1/") || p.startsWith("/api/v1/auth/") || p.startsWith("/api/v1/platform-admin")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities().stream().noneMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()))) {
            FeatureCatalog.Feature f = FeatureCatalog.match(request.getRequestURI(), request.getMethod().toUpperCase());
            if (f != null) {
                AppUser user = users.findByUsernameAndIsDeletedFalse(auth.getName()).orElse(null);
                if (user != null && user.getCompanyId() != null && !features.isEnabled(user.getCompanyId(), f.code())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    String label = f.label().replace("\"", "'");
                    response.getWriter().write("{\"success\":false,\"message\":\"FEATURE_DISABLED\",\"data\":{\"feature\":\"" + f.code()
                            + "\"},\"errors\":[\"" + label + " is not included in your subscription. Contact TransaFlow to enable it.\"]}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
