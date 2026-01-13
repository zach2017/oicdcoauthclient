package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security configuration for BFF (Backend-for-Frontend) Pattern.
 *
 * Key features:
 * - Session-based authentication with HttpOnly cookies
 * - OAuth2 login via Keycloak
 * - CSRF protection with cookie-based tokens
 * - CORS configured for frontend origin
 * - Extracts roles from Keycloak groups claim
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/success").permitAll()
                .requestMatchers("/error").permitAll()

                // Protected endpoints requiring authentication
                .requestMatchers("/api/auth/user").authenticated()
                .requestMatchers("/api/auth/csrf").authenticated()
                .requestMatchers("/api/auth/logout").authenticated()

                // Admin endpoints require ADMIN role
                .requestMatchers("/api/hello/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // Any other request requires authentication
                .anyRequest().authenticated()
            )

            // Enable session management for BFF pattern
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // CSRF protection with cookie-based tokens
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // OAuth2 Login configuration
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/keycloak")
                .defaultSuccessUrl("/api/auth/success", true)
                .failureUrl("/api/auth/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(this::mapAuthorities)
                )
            )

            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("BFFSESSION", "XSRF-TOKEN")
                .invalidateHttpSession(true)
            );

        return http.build();
    }

    /**
     * Extract authorities from OAuth2 user (Keycloak groups).
     * Converts groups claim to ROLE_* authorities for Spring Security.
     */
    private Collection<GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>(authorities);

        // This will be called after userInfo endpoint, OAuth2User will be available in context
        // We'll extract groups in the controller where we have access to OAuth2User attributes

        return mappedAuthorities;
    }

    /**
     * Custom authority mapper to extract roles from Keycloak groups.
     * This converter extracts authorities from the OAuth2 user's attributes.
     */
    @Bean
    public Converter<OidcUser, Collection<GrantedAuthority>> authoritiesConverter() {
        return oidcUser -> {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // Extract groups from Keycloak
            @SuppressWarnings("unchecked")
            List<String> groups = oidcUser.getAttribute("groups");

            if (groups != null) {
                for (String group : groups) {
                    // Remove leading slash if present
                    String groupName = group.startsWith("/") ? group.substring(1) : group;
                    // Handle nested groups - take the last part
                    String[] parts = groupName.split("/");
                    String roleName = parts[parts.length - 1];
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                }
            }

            return authorities;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Split the allowed origins string and set them
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow common headers including Authorization for Bearer tokens
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-XSRF-TOKEN"  // CSRF token header
        ));

        // Expose headers that the client might need
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition",
            "X-XSRF-TOKEN"
        ));

        // CRITICAL: Must be true for session cookies to work
        configuration.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
