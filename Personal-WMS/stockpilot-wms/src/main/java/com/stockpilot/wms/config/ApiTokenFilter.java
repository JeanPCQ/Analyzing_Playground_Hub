package com.stockpilot.wms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads "Authorization: Bearer <token>" on /api/** requests and, if the token is valid,
 * marks the request as logged in. Created in SecurityConfig (not a @Component on purpose,
 * so Spring Boot doesn't also register it for every request).
 */
public class ApiTokenFilter extends OncePerRequestFilter {

    private final ApiSessionService sessionService;

    public ApiTokenFilter(ApiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            sessionService.validate(token).ifPresent(session -> {
                var authorities = session.authorities().stream().map(SimpleGrantedAuthority::new).toList();
                var auth = new UsernamePasswordAuthenticationToken(session.username(), null, authorities);
                auth.setDetails(session);
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
