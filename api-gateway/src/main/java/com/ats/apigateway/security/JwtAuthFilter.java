package com.ats.apigateway.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards /api/admin/** at the gateway so individual services never need to
 * know about auth. Registered explicitly (not @Component-scanned) via
 * {@link SecurityConfig} so it's only mapped to admin routes, not every
 * request.
 */
public class JwtAuthFilter implements Filter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Browser CORS preflight requests never carry the Authorization
        // header, so let them through to Spring's own CORS handling rather
        // than rejecting them here — otherwise the browser blocks the real
        // request before it's even sent.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            respondUnauthorized(response, "Missing bearer token");
            return;
        }

        String token = authHeader.substring("Bearer ".length());
        try {
            jwtService.parse(token);
        } catch (JwtException e) {
            respondUnauthorized(response, "Invalid or expired token");
            return;
        }

        chain.doFilter(req, res);
    }

    private void respondUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
