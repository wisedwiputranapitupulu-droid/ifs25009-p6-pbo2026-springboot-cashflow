package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoragePresenterTest {
    @TempDir
    Path dir;

    @Test
    void saveAndDelete() throws Exception {
        FileStorage storage = new FileStorage(dir.resolve("uploads").toString());
        assertTrue(Files.isDirectory(storage.root()));
        String name = storage.save("Bukti.JPG", new byte[]{1, 2, 3});
        assertTrue(name.endsWith(".jpg"));
        assertTrue(Files.exists(storage.root().resolve(name)));
        storage.delete(name);
        assertFalse(Files.exists(storage.root().resolve(name)));
        storage.delete(name);          // sudah tidak ada: tidak error
        storage.delete(null);
        storage.delete("  ");
        storage.delete("../../etc/passwd"); // di luar root: diabaikan
    }

    @Test
    void saveValidation() {
        FileStorage storage = new FileStorage(dir.toString());
        assertThrows(ValidationException.class, () -> storage.save("a.png", null));
        assertThrows(ValidationException.class, () -> storage.save("a.png", new byte[0]));
        assertThrows(ValidationException.class, () -> storage.save("a.png", new byte[(int) FileStorage.MAX_BYTES + 1]));
        assertThrows(ValidationException.class, () -> storage.save(null, new byte[]{1}));
        assertThrows(ValidationException.class, () -> storage.save("noext", new byte[]{1}));
        assertThrows(ValidationException.class, () -> storage.save("virus.exe", new byte[]{1}));
    }

    @Test
    void failsWhenRootIsAFile() throws Exception {
        Path file = Files.writeString(dir.resolve("file.txt"), "x");
        assertThrows(IllegalStateException.class, () -> new FileStorage(file.resolve("sub").toString()));
    }

    @Test
    void presenterFormats() {
        FinancePresenter p = new FinancePresenter();
        assertEquals("Rp 1,000,000.00", p.formatMoney(1_000_000));
        assertEquals("Rp 0.00", p.formatMoney(0));
        assertEquals("-Rp 2,500.00", p.formatMoney(-2500));
        assertEquals("Saldo: Rp 4,975,000.00", p.formatBalance(new Balance(2, 5_000_000, 25_000)));
        Transaction t = new Transaction(7, 1, "Gaji", 1_000_000, TransactionType.INCOME, null,
                LocalDateTime.of(2026, 11, 26, 8, 5));
        assertEquals("[7] Gaji | Rp 1,000,000.00 | [Pemasukan]", p.formatTransaction(t));
        assertEquals("Transaksi tersimpan: [7] Gaji | Rp 1,000,000.00 | [Pemasukan]", p.savedMessage(t));
        assertEquals("Transaksi dihapus: [7] Gaji | Rp 1,000,000.00 | [Pemasukan]", p.deletedMessage(t));
        assertEquals("26 Nov 2026 08:05", p.formatDate(t.getCreatedAt()));
        assertEquals("-", p.formatDate(null));
        assertNull(p.fileUrl(null));
        assertNull(p.fileUrl(" "));
        assertEquals("/uploads/a.png", p.fileUrl("a.png"));
    }
}
