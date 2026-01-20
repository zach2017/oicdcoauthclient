package com.example.demo.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.HelloService;
/**
 * Public Controller - No authentication required.
 * Useful for health checks and public information.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    HelloService hello;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "Keycloak Resource Server");
       
        hello.sayHelloAsync();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Keycloak Resource Server");
        response.put("version", "1.0.0");
        response.put("description", "Spring Boot OAuth2 Resource Server with Keycloak");
        response.put("authProvider", "Keycloak");
        return ResponseEntity.ok(response);
    }
}
