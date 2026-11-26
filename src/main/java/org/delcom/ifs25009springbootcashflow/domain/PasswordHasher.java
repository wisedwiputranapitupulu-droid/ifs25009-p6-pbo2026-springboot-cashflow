package org.delcom.ifs25009springbootcashflow.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
    private PasswordHasher() { }

    @FunctionalInterface
    interface DigestFactory {
        MessageDigest create() throws NoSuchAlgorithmException;
    }

    public static String hash(String raw) {
        return hash(raw, () -> MessageDigest.getInstance("SHA-256"));
    }

    static String hash(String raw, DigestFactory digestFactory) {
        try {
            MessageDigest md = digestFactory.create();
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verify(String raw, String hash) {
        return raw != null && hash != null && hash(raw).equals(hash);
    }
}
