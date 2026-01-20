package com.example.demo.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.HelloService;

/**
 * Hello Controller - Protected by Keycloak OAuth2.
 * All endpoints require ADMIN role from Keycloak group membership.
 */
@RestController
@RequestMapping("/api/hello")
public class HelloController {

     @Autowired
    HelloService hello;

    /**
     * Basic hello endpoint - requires ADMIN role (configured in SecurityConfig)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> hello(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, Admin!");
        response.put("user", authentication.getName());
        response.put("timestamp", Instant.now().toString());
        response.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Personalized greeting with user details from JWT
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> helloMe(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", String.format("Hello, %s!", 
                jwt.getClaimAsString("preferred_username")));
        response.put("subject", jwt.getSubject());
        response.put("email", jwt.getClaimAsString("email"));
        response.put("name", jwt.getClaimAsString("name"));
        response.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
        response.put("groups", jwt.getClaimAsStringList("groups"));
        response.put("tokenIssuedAt", jwt.getIssuedAt());
        response.put("tokenExpiresAt", jwt.getExpiresAt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed user information including all claims
     */
    @GetMapping("/userinfo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("principal", authentication.getName());
        response.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        // Extract user info from JWT claims
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("preferred_username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("email_verified", jwt.getClaim("email_verified"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("given_name", jwt.getClaimAsString("given_name"));
        userInfo.put("family_name", jwt.getClaimAsString("family_name"));
        userInfo.put("groups", jwt.getClaimAsStringList("groups"));
        
        response.put("userInfo", userInfo);
        
        // Token metadata
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("issuer", jwt.getIssuer());
        tokenInfo.put("audience", jwt.getAudience());
        tokenInfo.put("issuedAt", jwt.getIssuedAt());
        tokenInfo.put("expiresAt", jwt.getExpiresAt());
        
        response.put("tokenInfo", tokenInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Admin-only action endpoint
     */
    @PostMapping("/action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performAdminAction(
            @RequestBody(required = false) Map<String, Object> payload,
            @AuthenticationPrincipal Jwt jwt) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Admin action performed successfully");
        response.put("performedBy", jwt.getClaimAsString("preferred_username"));
        response.put("timestamp", Instant.now().toString());
        
        if (payload != null) {
            response.put("receivedPayload", payload);
        }
        
        return ResponseEntity.ok(response);
    }
}
