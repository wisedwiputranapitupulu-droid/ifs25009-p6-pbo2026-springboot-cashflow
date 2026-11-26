package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import java.time.LocalDateTime;
import java.util.List;

public class FinanceUseCase {
    private final ITransactionRepository transactions;

    public FinanceUseCase(ITransactionRepository transactions) {
        this.transactions = transactions;
    }

    public static long parseAmount(String text) throws ValidationException {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Jumlah wajib diisi");
        }
        long value;
        try {
            value = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Jumlah harus berupa angka bulat");
        }
        validateAmount(value);
        return value;
    }

    public static void validateAmount(long amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih besar dari 0");
        }
    }

    public static void validateDescription(String description) throws ValidationException {
        if (description == null || description.isBlank()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
    }

    private static void validateId(int id) throws ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID tidak valid");
        }
    }

    public Transaction addTransaction(int userId, String description, String amountText,
                                      TransactionType type, String proofPath) throws DomainException {
        validateId(userId);
        validateDescription(description);
        long amount = parseAmount(amountText);
        if (type == null) {
            throw new ValidationException("Tipe transaksi wajib dipilih");
        }
        return transactions.save(new Transaction(0, userId, description.trim(), amount, type,
                proofPath, LocalDateTime.now()));
    }

    public Transaction getTransaction(int id) throws DomainException {
        validateId(id);
        return transactions.findById(id);
    }

    public List<Transaction> listTransactions(int userId, String keyword, TransactionTypeFilter filter,
                                              SortOption sort) {
        TransactionTypeFilter f = filter == null ? TransactionTypeFilter.ALL : filter;
        SortOption s = sort == null ? SortOption.NEWEST_FIRST : sort;
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        return transactions.findByUserId(userId).stream()
                .filter(f::matches)
                .filter(t -> key.isEmpty() || t.getDescription().toLowerCase().contains(key))
                .sorted(s.comparator())
                .toList();
    }

    public Balance getBalance(int userId) {
        List<Transaction> list = transactions.findByUserId(userId);
        long income = 0;
        long expense = 0;
        for (Transaction t : list) {
            if (t.getType() == TransactionType.INCOME) {
                income += t.getAmount();
            } else {
                expense += t.getAmount();
            }
        }
        return new Balance(list.size(), income, expense);
    }

    public void deleteTransaction(int requesterId, int id) throws DomainException {
        Transaction t = getTransaction(id);
        if (t.getUserId() != requesterId) {
            throw new ValidationException("Transaksi bukan milik Anda");
        }
        transactions.deleteById(id);
    }
}
