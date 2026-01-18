package com.example.demo.config;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private TestService testService; 

    @InjectMocks
    private TestController testController;

    @BeforeEach
    void setUp() {
        // Setup MockMvc with the Controller Advice attached
        mockMvc = MockMvcBuilders.standaloneSetup(testController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleAuthenticationException_ShouldReturn401() throws Exception {
        // Use doThrow for void methods
        doThrow(new AuthenticationException("Auth failed") {})
            .when(testService).doSomething();

        mockMvc.perform(get("/test/trigger"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Authentication required")))
                .andExpect(jsonPath("$.message", is("Please provide a valid access token")));
    }

    @Test
    void handleAccessDeniedException_ShouldReturn403() throws Exception {
        // Use doThrow for void methods
        doThrow(new AccessDeniedException("Forbidden"))
            .when(testService).doSomething();

        mockMvc.perform(get("/test/trigger"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Access denied")))
                .andExpect(jsonPath("$.message", is("You don't have the required permissions. ADMIN role is required.")));
    }

    @Test
    void handleGenericException_ShouldReturn500() throws Exception {
        String errorMessage = "Something went wrong";
        
        // Use doThrow for void methods
        doThrow(new RuntimeException(errorMessage))
            .when(testService).doSomething();

        mockMvc.perform(get("/test/trigger"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal server error")))
                .andExpect(jsonPath("$.message", is(errorMessage)));
    }

    // --- Helper Classes ---

    /**
     * A simple controller that relies on a service.
     * We test how the ExceptionHandler catches service failures.
     */
    @RestController
    static class TestController {
        private final TestService service;
        public TestController(TestService service) { this.service = service; }

        @GetMapping("/test/trigger")
        public void trigger() {
            service.doSomething();
        }
    }

    /**
     * Interface for mocking. Since doSomething() is void, 
     * we must use doThrow().when() in tests.
     */
    interface TestService {
        void doSomething();
    }
}