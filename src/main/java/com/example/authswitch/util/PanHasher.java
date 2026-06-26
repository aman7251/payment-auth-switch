package com.example.authswitch.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes a PAN (card number) with SHA-256 so we never store raw card numbers.
 * The hash is deterministic, so the same PAN always maps to the same lookup key.
 * (Real systems use a keyed HMAC or a tokenization vault; SHA-256 keeps the demo simple.)
 */
public final class PanHasher {

    private PanHasher() {
    }

    public static String sha256Hex(String pan) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(pan.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
