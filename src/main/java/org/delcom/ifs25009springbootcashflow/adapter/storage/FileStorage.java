package org.delcom.ifs25009springbootcashflow.adapter.storage;

import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Menyimpan file unggahan ke UPLOAD_DIR dengan nama UUID + ekstensi. */
public class FileStorage {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final FileOperations SYSTEM_FILES = new FileOperations() {
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
            return Files.deleteIfExists(path);
        }
    };
    private final Path root;
    private final FileOperations fileOperations;

    public FileStorage(String uploadDir) {
        this(uploadDir, SYSTEM_FILES);
    }

    FileStorage(String uploadDir, FileOperations fileOperations) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.fileOperations = fileOperations;
        try {
            fileOperations.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Tidak dapat membuat folder upload", e);
        }
    }

    interface FileOperations {
        void createDirectories(Path path) throws IOException;
        void write(Path path, byte[] data) throws IOException;
        boolean deleteIfExists(Path path) throws IOException;
    }

    public Path root() {
        return root;
    }

    /** Menyimpan file dan mengembalikan path relatif (nama file) di dalam folder upload. */
    public String save(String originalName, byte[] data) throws ValidationException {
        if (data == null || data.length == 0) {
            throw new ValidationException("File kosong");
        }
        if (data.length > MAX_BYTES) {
            throw new ValidationException("Ukuran file maksimal 5 MB");
        }
        String name = originalName == null ? "" : originalName;
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(ext)) {
            throw new ValidationException("Format file harus gambar (png, jpg, jpeg, gif, webp)");
        }
        String stored = UUID.randomUUID() + "." + ext;
        try {
            fileOperations.write(root.resolve(stored), data);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menyimpan file", e);
        }
        return stored;
    }

    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path target = root.resolve(storedPath).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            fileOperations.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menghapus file", e);
        }
    }
}
