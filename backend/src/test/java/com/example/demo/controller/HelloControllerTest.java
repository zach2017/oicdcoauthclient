package com.example.demo.controller;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link HelloController}.
 */
@ExtendWith(MockitoExtension.class)
class HelloControllerTest {

    @InjectMocks
    private HelloController helloController;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @Test
    void controllerExists() {
        assertThat(helloController).isNotNull();
    }

    @Test
    void helloReturnsResponse() {
        given(authentication.getName()).willReturn("testuser");
        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        given(authentication.getAuthorities()).willReturn(authorities);

        ResponseEntity<Map<String, Object>> response = helloController.hello(authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Hello, Admin!");
        assertThat(response.getBody().get("user")).isEqualTo("testuser");
    }

    @Test
    void helloMeReturnsUserDetails() {
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");
        given(jwt.getSubject()).willReturn("user-123");
        given(jwt.getClaimAsString("email")).willReturn("test@example.com");
        given(jwt.getClaimAsString("name")).willReturn("Test User");
        given(jwt.getClaimAsStringList("groups")).willReturn(List.of("/admins"));
        given(jwt.getIssuedAt()).willReturn(Instant.now());
        given(jwt.getExpiresAt()).willReturn(Instant.now().plusSeconds(3600));

        ResponseEntity<Map<String, Object>> response = helloController.helloMe(jwt);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Hello, testuser!");
        assertThat(response.getBody().get("subject")).isEqualTo("user-123");
    }

    @Test
    void getUserInfoReturnsCompleteInfo() {
        given(authentication.getName()).willReturn("testuser");
        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        given(authentication.getAuthorities()).willReturn(authorities);
        given(jwt.getSubject()).willReturn("user-123");
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");
        given(jwt.getClaimAsString("email")).willReturn("test@example.com");
        given(jwt.getClaim("email_verified")).willReturn(true);
        given(jwt.getClaimAsString("name")).willReturn("Test User");
        given(jwt.getClaimAsString("given_name")).willReturn("Test");
        given(jwt.getClaimAsString("family_name")).willReturn("User");
        given(jwt.getClaimAsStringList("groups")).willReturn(List.of("/admins"));
        given(jwt.getIssuer()).willReturn(null);
        given(jwt.getAudience()).willReturn(List.of("account"));
        given(jwt.getIssuedAt()).willReturn(Instant.now());
        given(jwt.getExpiresAt()).willReturn(Instant.now().plusSeconds(3600));

        ResponseEntity<Map<String, Object>> response = helloController.getUserInfo(authentication, jwt);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("principal")).isEqualTo("testuser");
        assertThat(response.getBody()).containsKey("userInfo");
        assertThat(response.getBody()).containsKey("tokenInfo");
    }

    @Test
    void performAdminActionWithoutPayload() {
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        ResponseEntity<Map<String, Object>> response = helloController.performAdminAction(null, jwt);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("performedBy")).isEqualTo("testuser");
    }

    @Test
    void performAdminActionWithPayload() {
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");
        Map<String, Object> payload = Map.of("action", "test");

        ResponseEntity<Map<String, Object>> response = helloController.performAdminAction(payload, jwt);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsKey("receivedPayload");
    }
}