package com.example.demo.security;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link KeycloakAuthenticationEntryPoint}.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakAuthenticationEntryPointTest {

    @InjectMocks
    private KeycloakAuthenticationEntryPoint entryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    @Test
    void entryPointExists() {
        assertThat(entryPoint).isNotNull();
    }

    @Test
    void returnsJsonForApiRequest() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getHeader("Accept")).willReturn("application/json");
        given(request.getHeader("X-Requested-With")).willReturn(null);
        given(request.getScheme()).willReturn("http");
        given(request.getServerName()).willReturn("localhost");
        given(request.getServerPort()).willReturn(8080);
        given(request.getRequestURI()).willReturn("/api/test");
        given(request.getQueryString()).willReturn(null);
        given(response.getWriter()).willReturn(printWriter);

        entryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(stringWriter.toString()).contains("unauthorized");
    }

    @Test
    void returnsJsonForXmlHttpRequest() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getHeader("Accept")).willReturn(null);
        given(request.getHeader("X-Requested-With")).willReturn("XMLHttpRequest");
        given(request.getScheme()).willReturn("http");
        given(request.getServerName()).willReturn("localhost");
        given(request.getServerPort()).willReturn(8080);
        given(request.getRequestURI()).willReturn("/api/test");
        given(request.getQueryString()).willReturn(null);
        given(response.getWriter()).willReturn(printWriter);

        entryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(stringWriter.toString()).contains("Authentication required");
    }

    @Test
    void jsonResponseContainsLoginUrl() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getHeader("Accept")).willReturn("application/json");
        given(request.getHeader("X-Requested-With")).willReturn(null);
        given(request.getScheme()).willReturn("http");
        given(request.getServerName()).willReturn("localhost");
        given(request.getServerPort()).willReturn(8080);
        given(request.getRequestURI()).willReturn("/api/test");
        given(request.getQueryString()).willReturn(null);
        given(response.getWriter()).willReturn(printWriter);

        entryPoint.commence(request, response, authException);

        assertThat(stringWriter.toString()).contains("login_url");
        assertThat(stringWriter.toString()).contains("timestamp");
    }

    @Test
    void jsonResponseContainsTimestamp() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        given(request.getHeader("Accept")).willReturn("application/json");
        given(request.getHeader("X-Requested-With")).willReturn(null);
        given(request.getScheme()).willReturn("https");
        given(request.getServerName()).willReturn("example.com");
        given(request.getServerPort()).willReturn(443);
        given(request.getRequestURI()).willReturn("/api/resource");
        given(request.getQueryString()).willReturn("id=123");
        given(response.getWriter()).willReturn(printWriter);

        entryPoint.commence(request, response, authException);

        String jsonResponse = stringWriter.toString();
        assertThat(jsonResponse).contains("\"error\":\"unauthorized\"");
        assertThat(jsonResponse).contains("\"timestamp\":");
    }
}
