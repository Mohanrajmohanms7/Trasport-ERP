package com.transport.erp.security;

import com.transport.erp.model.AppUser;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.service.RolePermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central role-permission check (V92): every /api/v1 call is mapped to a module and an action; a role whose saved grid
 * does not allow it gets 403 "Your role cannot Approve Invoices." Roles without a saved grid keep full access. Runs on
 * top of the existing role / tenant / plan rules (it can only take access away, never add it).
 */
public class ModulePermissionFilter extends OncePerRequestFilter {

    private final RolePermissionService permissions;
    private final AppUserRepository users;
    private final Map<String, long[]> companyCache = new ConcurrentHashMap<>();   // username -> {companyId, loadedAt}

    public ModulePermissionFilter(RolePermissionService permissions, AppUserRepository users) {
        this.permissions = permissions;
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return !uri.startsWith("/api/v1/") || uri.startsWith("/api/v1/auth") || uri.startsWith("/api/v1/platform-admin")
                || uri.startsWith("/api/v1/driver-app") || uri.equals("/api/v1/role-permissions/me") || "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) { chain.doFilter(req, res); return; }
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority a : auth.getAuthorities()) if (a.getAuthority() != null && a.getAuthority().startsWith("ROLE_")) roles.add(a.getAuthority().substring(5));
        if (roles.contains("SUPER_ADMIN") || roles.isEmpty() || roles.stream().allMatch("DRIVER"::equals)) { chain.doFilter(req, res); return; }
        String path = req.getRequestURI().substring("/api/v1".length());
        RolePermissionService.Module module = RolePermissionService.moduleFor(path);
        if (module == null) { chain.doFilter(req, res); return; }
        RolePermissionService.Action action = RolePermissionService.actionFor(req.getMethod(), path);
        if (action == RolePermissionService.Action.VIEW && module.listsOpen()) { chain.doFilter(req, res); return; }   // dropdowns elsewhere
        Long companyId = companyOf(auth.getName());
        if (companyId == null || permissions.allowed(companyId, roles, module, action)) { chain.doFilter(req, res); return; }
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        String verb = action.name().charAt(0) + action.name().substring(1).toLowerCase(Locale.ROOT);
        res.getWriter().write("{\"success\":false,\"message\":\"Not Allowed\",\"data\":null,\"errors\":[\"Your role cannot "
                + verb + " " + module.label().replace("\"", "") + ".\",\"Action: Ask your admin for this permission.\"]}");
    }

    private Long companyOf(String username) {
        long now = System.currentTimeMillis();
        long[] c = companyCache.get(username);
        if (c != null && now - c[1] < 60_000) return c[0] == 0 ? null : c[0];
        AppUser u = users.findByUsernameAndIsDeletedFalse(username).orElse(null);
        long cid = u == null || u.getCompanyId() == null ? 0 : u.getCompanyId();
        companyCache.put(username, new long[]{cid, now});
        return cid == 0 ? null : cid;
    }
}
