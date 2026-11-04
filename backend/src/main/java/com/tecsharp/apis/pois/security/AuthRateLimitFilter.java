package com.tecsharp.apis.pois.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Frena fuerza bruta en login/registro: N intentos por minuto por IP. */
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final FixedWindowRateLimiter limiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"POST".equals(request.getMethod()) || !(path.equals("/auth/login") || path.equals("/auth/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!limiter.hit("auth:" + ApiKeyFilter.clientIp(request)).allowed()) {
            ApiKeyFilter.writeError(response, 429, "Demasiados intentos. Espera un minuto");
            return;
        }
        chain.doFilter(request, response);
    }
}
