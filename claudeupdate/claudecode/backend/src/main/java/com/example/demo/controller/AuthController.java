package com.example.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication controller for BFF pattern.
 * Handles login, logout, user info, and CSRF token endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Get current authenticated user information.
     * Returns user details from OAuth2 authentication.
     *
     * @param oauth2User The authenticated OAuth2 user
     * @return User information including username, email, name, and groups
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", oauth2User.getAttribute("preferred_username"));
        userInfo.put("email", oauth2User.getAttribute("email"));
        userInfo.put("name", oauth2User.getAttribute("name"));
        userInfo.put("groups", oauth2User.getAttribute("groups"));

        // Add additional attributes if needed
        userInfo.put("given_name", oauth2User.getAttribute("given_name"));
        userInfo.put("family_name", oauth2User.getAttribute("family_name"));

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Initiate OAuth2 login flow.
     * Redirects to Spring Security's OAuth2 authorization endpoint.
     *
     * @param response HTTP response for redirect
     */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/keycloak");
    }

    /**
     * OAuth2 login success callback.
     * After successful authentication, redirects user back to frontend.
     *
     * @param response HTTP response for redirect
     */
    @GetMapping("/success")
    public void loginSuccess(HttpServletResponse response) throws IOException {
        response.sendRedirect("http://localhost:3000");
    }

    /**
     * Logout endpoint.
     * Invalidates the session and clears authentication.
     *
     * @param session HTTP session to invalidate
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get CSRF token for state-changing requests.
     * Frontend should include this token in POST/PUT/DELETE/PATCH requests.
     *
     * @param csrfToken CSRF token automatically injected by Spring Security
     * @return CSRF token details including token value and header name
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken csrfToken) {
        Map<String, String> response = new HashMap<>();
        response.put("token", csrfToken.getToken());
        response.put("headerName", csrfToken.getHeaderName());
        response.put("parameterName", csrfToken.getParameterName());
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for authentication service.
     *
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "BFF Authentication");
        return ResponseEntity.ok(response);
    }
}
