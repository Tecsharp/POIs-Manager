package com.tecsharp.apis.pois.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generación y hash de API keys. Formato: tr_ + 43 caracteres base64url (256 bits aleatorios). */
public final class ApiKeys {

    public static final String PREFIX = "tr_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeys() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean looksLikeKey(String value) {
        return value != null && value.startsWith(PREFIX) && value.length() == PREFIX.length() + 43;
    }

    public static String displayPrefix(String key) {
        return key.substring(0, PREFIX.length() + 8);
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
