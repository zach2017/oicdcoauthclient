package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple tests for {@link SecurityConfig} - verifies beans exist and configuration is correct.
 */
@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void securityFilterChainBeanExists() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void corsConfigurationSourceBeanExists() {
        assertThat(corsConfigurationSource).isNotNull();
    }

    @Test
    void corsAllowedOriginsConfigured() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest());
        
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).isNotEmpty();
    }

    @Test
    void corsAllowedMethodsConfigured() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest());
        
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Test
    void corsAllowedHeadersConfigured() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest());
        
        assertThat(config.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }

    @Test
    void corsCredentialsAllowed() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest());
        
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsMaxAgeConfigured() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest());
        
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }
}