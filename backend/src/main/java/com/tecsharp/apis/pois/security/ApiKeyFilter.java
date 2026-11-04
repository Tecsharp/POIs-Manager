package com.tecsharp.apis.pois.security;

import com.tecsharp.apis.pois.services.apikey.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Control de consumo de /pois/**:
 * - Con API key (X-API-Key o Authorization: Bearer tr_...): cuota diaria por key, persistida en BD.
 * - Sin key: acceso gratuito limitado por IP y por minuto.
 */
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final FixedWindowRateLimiter anonymousLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equals(request.getMethod())
                || !path.startsWith("/pois")
                || path.equals("/pois/status");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String rawKey = extractKey(request);
        FixedWindowRateLimiter.Result quota;
        if (rawKey != null) {
            ApiKeyService.Consumption c = apiKeyService.consume(rawKey);
            if (c == null) {
                writeError(response, 401, "API key inválida o revocada");
                return;
            }
            quota = c.quota();
            response.setHeader("X-RateLimit-Scope", "api-key");
        } else {
            quota = anonymousLimiter.hit("anon:" + clientIp(request));
            response.setHeader("X-RateLimit-Scope", "anonymous");
        }
        response.setHeader("X-RateLimit-Limit", String.valueOf(quota.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(quota.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(quota.resetEpochSeconds()));
        if (!quota.allowed()) {
            response.setHeader("Retry-After", String.valueOf(
                    Math.max(1, quota.resetEpochSeconds() - System.currentTimeMillis() / 1000)));
            writeError(response, 429, rawKey != null
                    ? "Cuota diaria de tu API key agotada"
                    : "Límite gratuito alcanzado. Espera un minuto o regístrate para obtener una API key");
            return;
        }
        chain.doFilter(request, response);
    }

    private String extractKey(HttpServletRequest request) {
        String key = request.getHeader("X-API-Key");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer " + ApiKeys.PREFIX)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    /** El backend solo es accesible a través de nginx, que fija X-Real-IP; por eso se confía en esa cabecera. */
    static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        return ip != null && !ip.isBlank() ? ip : request.getRemoteAddr();
    }

    public static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"errorDTO\":{\"errorCode\":\"" + status + "\",\"errorMessage\":\"" + message + "\"}}");
    }
}
