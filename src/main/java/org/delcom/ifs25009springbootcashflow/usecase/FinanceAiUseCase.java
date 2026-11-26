package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FinanceAiUseCase {
    private final IFinanceAiService ai;
    private final ITransactionRepository transactions;

    public FinanceAiUseCase(IFinanceAiService ai, ITransactionRepository transactions) {
        this.ai = ai;
        this.transactions = transactions;
    }

    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException("Prompt tidak boleh kosong");
        }
        return ai.generateTransactions(prompt.trim());
    }

    public List<Transaction> saveGeneratedTransactions(int userId, List<AiGeneratedTransaction> items)
            throws DomainException {
        if (userId <= 0) {
            throw new ValidationException("ID tidak valid");
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Tidak ada transaksi untuk disimpan");
        }
        for (AiGeneratedTransaction item : items) {
            FinanceUseCase.validateDescription(item.description());
            FinanceUseCase.validateAmount(item.amount());
            if (item.type() == null) {
                throw new ValidationException("Tipe transaksi wajib dipilih");
            }
        }
        List<Transaction> saved = new ArrayList<>();
        for (AiGeneratedTransaction item : items) {
            saved.add(transactions.save(new Transaction(0, userId, item.description().trim(), item.amount(),
                    item.type(), null, LocalDateTime.now())));
        }
        return saved;
    }
}
