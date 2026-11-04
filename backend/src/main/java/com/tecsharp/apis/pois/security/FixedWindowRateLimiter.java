package com.tecsharp.apis.pois.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter en memoria por ventana fija (p. ej. N peticiones por minuto por IP).
 * shortcut: estado en memoria de una sola instancia; si el backend escala a varias réplicas, mover a Redis.
 */
public class FixedWindowRateLimiter {

    public record Result(boolean allowed, int limit, int remaining, long resetEpochSeconds) {}

    private final int limit;
    private final long windowMs;
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int limit, long windowMs) {
        this.limit = limit;
        this.windowMs = windowMs;
    }

    public Result hit(String key) {
        return hit(key, System.currentTimeMillis());
    }

    Result hit(String key, long nowMs) {
        long window = nowMs / windowMs;
        long[] state = windows.compute(key, (k, v) ->
                (v == null || v[0] != window) ? new long[]{window, 1} : new long[]{window, v[1] + 1});
        if (windows.size() > 50_000) {
            windows.entrySet().removeIf(e -> e.getValue()[0] < window);
        }
        int used = (int) Math.min(state[1], Integer.MAX_VALUE);
        return new Result(used <= limit, limit, Math.max(0, limit - used), (window + 1) * windowMs / 1000);
    }
}
