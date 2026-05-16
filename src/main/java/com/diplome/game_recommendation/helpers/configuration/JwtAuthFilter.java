package com.diplome.game_recommendation.helpers.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserAuthenticationProvider provider;

    public JwtAuthFilter(UserAuthenticationProvider provider) {
        this.provider = provider;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            String path = request.getRequestURI();
    if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
        filterChain.doFilter(request, response);
        return;
    }
        if (header != null) {
            String[] authElements = header.split(" ");

            if (authElements.length == 2 && "Bearer".equals(authElements[0])) {
                try {
                    String token = authElements[1];

                    if (token != null && !token.trim().isEmpty()) {
                        var authentication = provider.validateToken(token);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (RuntimeException e) {
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
