package com.tecsharp.apis.pois.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityPrimitivesTest {

    @Test
    void generatedKeysAreUniqueWellFormedAndHashStable() {
        String a = ApiKeys.generate();
        String b = ApiKeys.generate();
        assertNotEquals(a, b);
        assertTrue(ApiKeys.looksLikeKey(a));
        assertFalse(ApiKeys.looksLikeKey("tr_short"));
        assertFalse(ApiKeys.looksLikeKey(null));
        assertEquals(ApiKeys.sha256(a), ApiKeys.sha256(a));
        assertEquals(64, ApiKeys.sha256(a).length());
        assertEquals(11, ApiKeys.displayPrefix(a).length());
    }

    @Test
    void rateLimiterBlocksAfterLimitAndResetsNextWindow() {
        var limiter = new FixedWindowRateLimiter(2, 60_000);
        long t = 120_000;
        assertTrue(limiter.hit("ip", t).allowed());
        var second = limiter.hit("ip", t + 1);
        assertTrue(second.allowed());
        assertEquals(0, second.remaining());
        assertFalse(limiter.hit("ip", t + 2).allowed());
        assertTrue(limiter.hit("other", t + 2).allowed());
        assertTrue(limiter.hit("ip", t + 60_000).allowed());
        assertEquals(240, limiter.hit("ip", t + 60_000).resetEpochSeconds());
    }
}
