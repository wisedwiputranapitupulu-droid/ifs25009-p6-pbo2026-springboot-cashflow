package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvTest {
    @TempDir
    Path dir;

    @Test
    void loadsOnlyValidLinesAndKeepsExistingProperties() throws Exception {
        Path env = dir.resolve(".env");
        Files.writeString(env, "# komentar\n\nCF_TEST_A = nilai-a\nCF_TEST_B=x=y\nBARIS_RUSAK\n=tanpa-kunci\nCF_TEST_KEEP=baru\n");
        System.setProperty("CF_TEST_KEEP", "lama");
        try {
            DotEnv.load(env);
            assertEquals("nilai-a", System.getProperty("CF_TEST_A"));
            assertEquals("x=y", System.getProperty("CF_TEST_B"));
            assertEquals("lama", System.getProperty("CF_TEST_KEEP"));
            assertNull(System.getProperty("BARIS_RUSAK"));
        } finally {
            System.clearProperty("CF_TEST_A");
            System.clearProperty("CF_TEST_B");
            System.clearProperty("CF_TEST_KEEP");
        }
    }

    @Test
    void missingFileIsIgnored() throws Exception {
        DotEnv.load(dir.resolve("tidak-ada.env"));
        DotEnv.load(); // .env di working dir (boleh ada atau tidak)
    }

    @Test
    void environmentVariablesTakePrecedence() throws Exception {
        Map.Entry<String, String> environmentVariable = System.getenv().entrySet().stream()
                .filter(entry -> System.getProperty(entry.getKey()) == null)
                .findFirst()
                .orElseThrow();
        Path env = dir.resolve(".env");
        Files.writeString(env, environmentVariable.getKey() + "=nilai-file\n");
        DotEnv.load(env);
        assertNull(System.getProperty(environmentVariable.getKey()));
    }
}
