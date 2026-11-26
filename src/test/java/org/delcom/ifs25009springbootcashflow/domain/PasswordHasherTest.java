package org.delcom.ifs25009springbootcashflow.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    @Test
    void reportsUnavailableDigest() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> PasswordHasher.hash("password", () -> {
                    throw new NoSuchAlgorithmException("SHA-256 unavailable");
                }));
        assertEquals("SHA-256 unavailable", error.getCause().getMessage());
    }
}
