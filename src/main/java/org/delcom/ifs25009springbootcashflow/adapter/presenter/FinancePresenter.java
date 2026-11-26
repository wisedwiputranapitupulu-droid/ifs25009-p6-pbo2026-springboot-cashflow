package org.delcom.ifs25009springbootcashflow.adapter.presenter;

import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Format teks/uang untuk tampilan; bukan tanggung jawab entity. */
public class FinancePresenter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.US);

    /** Contoh: Rp 1,000,000.00 */
    public String formatMoney(long amount) {
        String number = String.format(Locale.US, "%,.2f", Math.abs((double) amount));
        return (amount < 0 ? "-Rp " : "Rp ") + number;
    }

    /** Contoh: [3] Gaji | Rp 1,000,000.00 | [Pemasukan] */
    public String formatTransaction(Transaction t) {
        return "[" + t.getId() + "] " + t.getDescription() + " | " + formatMoney(t.getAmount())
                + " | [" + t.getType().label() + "]";
    }

    /** Contoh: Saldo: Rp 4,975,000.00 */
    public String formatBalance(Balance b) {
        return "Saldo: " + formatMoney(b.balance());
    }

    public String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE.format(dateTime);
    }

    public String fileUrl(String storedPath) {
        return storedPath == null || storedPath.isBlank() ? null : "/uploads/" + storedPath;
    }

    public String savedMessage(Transaction t) {
        return "Transaksi tersimpan: " + formatTransaction(t);
    }

    public String deletedMessage(Transaction t) {
        return "Transaksi dihapus: " + formatTransaction(t);
    }
}
