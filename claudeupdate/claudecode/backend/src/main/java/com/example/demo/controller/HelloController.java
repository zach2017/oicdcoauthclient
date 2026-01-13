package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hello Controller - Protected by BFF session-based authentication.
 * All endpoints require ADMIN role from Keycloak group membership.
 */
@RestController
@RequestMapping("/api/hello")
public class HelloController {

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
     * Personalized greeting with user details from OAuth2User
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> helloMe(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = new HashMap<>();
        String username = oauth2User.getAttribute("preferred_username");
        response.put("message", String.format("Hello, %s!", username));
        response.put("subject", oauth2User.getAttribute("sub"));
        response.put("email", oauth2User.getAttribute("email"));
        response.put("name", oauth2User.getAttribute("name"));
        response.put("preferredUsername", username);
        response.put("groups", oauth2User.getAttribute("groups"));

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed user information including all attributes
     */
    @GetMapping("/userinfo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            Authentication authentication,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        Map<String, Object> response = new HashMap<>();
        response.put("principal", authentication.getName());
        response.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        // Extract user info from OAuth2User attributes
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", oauth2User.getAttribute("sub"));
        userInfo.put("preferred_username", oauth2User.getAttribute("preferred_username"));
        userInfo.put("email", oauth2User.getAttribute("email"));
        userInfo.put("email_verified", oauth2User.getAttribute("email_verified"));
        userInfo.put("name", oauth2User.getAttribute("name"));
        userInfo.put("given_name", oauth2User.getAttribute("given_name"));
        userInfo.put("family_name", oauth2User.getAttribute("family_name"));
        userInfo.put("groups", oauth2User.getAttribute("groups"));

        response.put("userInfo", userInfo);

        // Session info (instead of token info)
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("authenticationType", "OAuth2");
        sessionInfo.put("provider", "Keycloak");
        sessionInfo.put("timestamp", Instant.now().toString());

        response.put("sessionInfo", sessionInfo);

        // All available attributes
        response.put("allAttributes", oauth2User.getAttributes());

        return ResponseEntity.ok(response);
    }

    /**
     * Admin-only action endpoint
     */
    @PostMapping("/action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performAdminAction(
            @RequestBody(required = false) Map<String, Object> payload,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Admin action performed successfully");
        response.put("performedBy", oauth2User.getAttribute("preferred_username"));
        response.put("timestamp", Instant.now().toString());

        if (payload != null) {
            response.put("receivedPayload", payload);
        }

        return ResponseEntity.ok(response);
    }
}
