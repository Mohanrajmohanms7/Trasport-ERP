package com.transport.erp.config;

import com.transport.erp.security.CustomUserDetailsService;
import com.transport.erp.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*,https://localhost:*,https://127.0.0.1:*,https://transport-frontend.onrender.com}")
    private String allowedOrigins;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    static final String[] ADMIN_ROLES = {"SUPER_ADMIN", "COMPANY_ADMIN", "ADMIN"};
    static final String[] ADMIN_WRITE_PATHS = {
            "/api/v1/users/**", "/api/v1/roles/**", "/api/v1/permissions/**", "/api/v1/companies/**",
            "/api/v1/branches/**", "/api/v1/settings/**", "/api/v1/financial-years/**", "/api/v1/setup/**"
    };
    static final String[] ACCOUNTING_ROLES = {"SUPER_ADMIN", "COMPANY_ADMIN", "ADMIN", "ACCOUNTANT"};
    static final String[] ACCOUNTING_WRITE_PATHS = {"/api/v1/journal/**", "/api/v1/accounts/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public auth only — never open master/ops APIs without JWT
                .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/plans"
                ).permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/platform-admin/**").hasRole("SUPER_ADMIN")
                // Tenant administration: only admins may change users, roles, company, branches, settings, years.
                .requestMatchers(HttpMethod.POST, ADMIN_WRITE_PATHS).hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.PUT, ADMIN_WRITE_PATHS).hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.PATCH, ADMIN_WRITE_PATHS).hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.DELETE, ADMIN_WRITE_PATHS).hasAnyRole(ADMIN_ROLES)
                // Manual journal entries and chart of accounts: accounts staff only.
                .requestMatchers(HttpMethod.POST, ACCOUNTING_WRITE_PATHS).hasAnyRole(ACCOUNTING_ROLES)
                .requestMatchers(HttpMethod.PUT, ACCOUNTING_WRITE_PATHS).hasAnyRole(ACCOUNTING_ROLES)
                .requestMatchers(HttpMethod.DELETE, ACCOUNTING_WRITE_PATHS).hasAnyRole(ACCOUNTING_ROLES)
                // Everyone else must be signed in; a DRIVER-only login is limited to the driver app APIs.
                .anyRequest().access(new DriverScopeAuthorizationManager())

            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Unauthorized\",\"data\":null,\"errors\":[\"Authentication required\"]}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Forbidden\",\"data\":null,\"errors\":[\"Access denied\"]}"
                    );
                })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
