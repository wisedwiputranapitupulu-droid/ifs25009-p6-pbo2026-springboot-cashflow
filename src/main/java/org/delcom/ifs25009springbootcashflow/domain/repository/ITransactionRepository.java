package org.delcom.ifs25009springbootcashflow.domain.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import java.util.List;

public interface ITransactionRepository extends IRepository<Transaction, Integer> {
    List<Transaction> findByUserId(int userId);
}
