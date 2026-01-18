package com.example.demo.security;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link KeycloakAccessDeniedHandler}.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakAccessDeniedHandlerTest {

    @InjectMocks
    private KeycloakAccessDeniedHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AccessDeniedException accessDeniedException;

    @Mock
    private Principal principal;

    @Test
    void handlerExists() {
        assertThat(handler).isNotNull();
    }

    @Test
    void returnsForbiddenStatus() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void returnsJsonContentType() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        verify(response).setContentType("application/json");
    }

    @Test
    void responseContainsErrorField() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        assertThat(stringWriter.toString()).contains("\"error\":\"access_denied\"");
    }

    @Test
    void responseContainsUsername() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(principal.getName()).willReturn("testuser");
        given(request.getUserPrincipal()).willReturn(principal);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        assertThat(stringWriter.toString()).contains("\"user\":\"testuser\"");
    }

    @Test
    void responseContainsUnknownWhenNoPrincipal() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        assertThat(stringWriter.toString()).contains("\"user\":\"unknown\"");
    }

    @Test
    void responseContainsRequestPath() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin/users");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        assertThat(stringWriter.toString()).contains("\"path\":\"/api/admin/users\"");
    }

    @Test
    void responseContainsRequiredRole() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        assertThat(stringWriter.toString()).contains("\"required_role\":\"ADMIN\"");
    }

    @Test
    void responseContainsTimestamp() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getUserPrincipal()).willReturn(null);
        given(request.getRequestURI()).willReturn("/api/admin");
        given(response.getWriter()).willReturn(printWriter);

        handler.handle(request, response, accessDeniedException);

        assertThat(stringWriter.toString()).contains("\"timestamp\":");
    }
}
