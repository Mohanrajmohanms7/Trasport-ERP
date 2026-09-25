package com.transport.erp.config;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Supplier;

/**
 * Staff: any signed-in user. A login whose ONLY role is DRIVER (the driver app) may call just the
 * driver-facing APIs; every other endpoint (invoices, ledgers, payroll of others, users...) is refused.
 * A login whose only role is VIEWER is read-only.
 */
public class DriverScopeAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Any method. */
    static final List<String> DRIVER_PATHS = List.of(
            "/api/v1/auth/**",
            "/api/v1/maintenance-requests/**",
            "/api/v1/driver-payrolls/my/**",
            "/api/v1/driver-payrolls/my",
            "/api/v1/files/**",
            "/api/v1/attachments/**"
    );

    /** Read-only lookups the driver app needs for its forms. */
    static final List<String> DRIVER_READ_PATHS = List.of(
            "/api/v1/vehicles/**", "/api/v1/vehicles",
            "/api/v1/drivers/**", "/api/v1/drivers",
            "/api/v1/lookups/**", "/api/v1/uoms/**"
    );

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext ctx) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return new AuthorizationDecision(false);
        }
        String path = ctx.getRequest().getRequestURI();
        String ctxPath = ctx.getRequest().getContextPath();
        if (ctxPath != null && !ctxPath.isEmpty() && path.startsWith(ctxPath)) path = path.substring(ctxPath.length());
        String method = ctx.getRequest().getMethod();

        if (isOnly(auth, "ROLE_VIEWER")) {
            // Read-only role: may look at everything, change nothing (except own password / logout).
            return new AuthorizationDecision("GET".equalsIgnoreCase(method) || MATCHER.match("/api/v1/auth/**", path));
        }
        if (!isOnly(auth, "ROLE_DRIVER")) {
            return new AuthorizationDecision(true);
        }
        for (String p : DRIVER_PATHS) {
            if (MATCHER.match(p, path)) return new AuthorizationDecision(true);
        }
        if ("GET".equalsIgnoreCase(method)) {
            for (String p : DRIVER_READ_PATHS) {
                if (MATCHER.match(p, path)) return new AuthorizationDecision(true);
            }
        }
        return new AuthorizationDecision(false);
    }

    /** True when the user's only role authority is the given one (permission authorities are ignored). */
    static boolean isOnly(Authentication auth, String roleAuthority) {
        boolean found = false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String code = a.getAuthority();
            if (roleAuthority.equals(code)) {
                found = true;
            } else if (code != null && code.startsWith("ROLE_")) {
                return false;
            }
        }
        return found;
    }
}
