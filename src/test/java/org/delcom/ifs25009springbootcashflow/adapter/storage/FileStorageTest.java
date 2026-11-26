package org.delcom.ifs25009springbootcashflow.adapter.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageTest {
    @TempDir
    Path dir;

    @Test
    void reportsWriteFailures() {
        FileStorage storage = new FileStorage(dir.resolve("uploads").toString(), new FileStorage.FileOperations() {
            @Override
            public void createDirectories(Path path) throws IOException {
                Files.createDirectories(path);
            }

            @Override
            public void write(Path path, byte[] data) throws IOException {
                throw new IOException("disk unavailable");
            }

            @Override
            public boolean deleteIfExists(Path path) throws IOException {
                return Files.deleteIfExists(path);
            }
        });

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> storage.save("receipt.png", new byte[]{1}));
        assertEquals("Gagal menyimpan file", error.getMessage());
        assertEquals("disk unavailable", error.getCause().getMessage());
    }

    @Test
    void reportsDeleteFailures() {
        FileStorage storage = new FileStorage(dir.resolve("uploads").toString(), new FileStorage.FileOperations() {
            @Override
            public void createDirectories(Path path) throws IOException {
                Files.createDirectories(path);
            }

            @Override
            public void write(Path path, byte[] data) throws IOException {
                Files.write(path, data);
            }

            @Override
            public boolean deleteIfExists(Path path) throws IOException {
                throw new IOException("disk unavailable");
            }
        });

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> storage.delete("receipt.png"));
        assertEquals("Gagal menghapus file", error.getMessage());
        assertEquals("disk unavailable", error.getCause().getMessage());
    }
}
