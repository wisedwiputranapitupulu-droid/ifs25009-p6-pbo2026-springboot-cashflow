package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import java.time.LocalDateTime;
import java.util.List;

public class TransactionRepository extends InMemoryRepository<Transaction, Integer> implements ITransactionRepository {
    private int sequence = 0;

    @Override protected String entityName() { return "Transaksi"; }
    @Override protected Integer idOf(Transaction t) { return t.getId(); }
    @Override protected boolean isNew(Transaction t) { return t.getId() == 0; }

    @Override
    protected void assignId(Transaction t) {
        t.setId(++sequence);
        if (t.getCreatedAt() == null) {
            t.setCreatedAt(LocalDateTime.now());
        }
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return findBy(t -> t.getUserId() == userId);
    }
}
