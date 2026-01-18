
package com.example.demo.security;

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
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link KeycloakJwtAuthenticationConverter}.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakJwtAuthenticationConverterTest {

    @InjectMocks
    private KeycloakJwtAuthenticationConverter converter;

    @Mock
    private Jwt jwt;

    @Test
    void converterExists() {
        assertThat(converter).isNotNull();
    }

    @Test
    void convertReturnsAuthenticationToken() {
        given(jwt.getClaim("realm_access")).willReturn(null);
        given(jwt.getClaim("resource_access")).willReturn(null);
        given(jwt.getClaim("groups")).willReturn(null);
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("testuser");
    }

    @Test
    void extractsRealmRoles() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("admin", "user"));
        given(jwt.getClaim("realm_access")).willReturn(realmAccess);
        given(jwt.getClaim("resource_access")).willReturn(null);
        given(jwt.getClaim("groups")).willReturn(null);
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void extractsResourceRoles() {
        Map<String, Object> clientAccess = Map.of("roles", List.of("client-admin"));
        Map<String, Object> resourceAccess = Map.of("my-client", clientAccess);
        given(jwt.getClaim("realm_access")).willReturn(null);
        given(jwt.getClaim("resource_access")).willReturn(resourceAccess);
        given(jwt.getClaim("groups")).willReturn(null);
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_CLIENT-ADMIN");
    }

    @Test
    void extractsGroupRoles() {
        given(jwt.getClaim("realm_access")).willReturn(null);
        given(jwt.getClaim("resource_access")).willReturn(null);
        given(jwt.getClaim("groups")).willReturn(List.of("/ADMIN", "/users"));
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USERS");
    }

    @Test
    void extractsNestedGroupRoles() {
        given(jwt.getClaim("realm_access")).willReturn(null);
        given(jwt.getClaim("resource_access")).willReturn(null);
        given(jwt.getClaim("groups")).willReturn(List.of("/org/department/ADMIN"));
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void usesSubjectWhenNoPreferredUsername() {
        given(jwt.getClaim("realm_access")).willReturn(null);
        given(jwt.getClaim("resource_access")).willReturn(null);
        given(jwt.getClaim("groups")).willReturn(null);
        given(jwt.getClaimAsString("preferred_username")).willReturn(null);
        given(jwt.getSubject()).willReturn("subject-123");

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("subject-123");
    }

    @Test
    void combinesAllRoleSources() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("realm-role"));
        Map<String, Object> clientAccess = Map.of("roles", List.of("client-role"));
        Map<String, Object> resourceAccess = Map.of("my-client", clientAccess);
        given(jwt.getClaim("realm_access")).willReturn(realmAccess);
        given(jwt.getClaim("resource_access")).willReturn(resourceAccess);
        given(jwt.getClaim("groups")).willReturn(List.of("/group-role"));
        given(jwt.getClaimAsString("preferred_username")).willReturn("testuser");

        AbstractAuthenticationToken token = converter.convert(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_REALM-ROLE", "ROLE_CLIENT-ROLE", "ROLE_GROUP-ROLE");
    }
}
