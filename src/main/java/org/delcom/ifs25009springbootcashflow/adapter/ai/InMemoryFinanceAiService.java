package org.delcom.ifs25009springbootcashflow.adapter.ai;

import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.repository.IFinanceAiService;
import java.util.ArrayList;
import java.util.List;

/** Stub AI deterministik untuk unit test (tanpa jaringan). */
public class InMemoryFinanceAiService implements IFinanceAiService {
    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) {
        List<AiGeneratedTransaction> result = new ArrayList<>();
        if (prompt.toLowerCase().contains("gaji")) {
            result.add(new AiGeneratedTransaction("Gaji bulanan", 5_000_000L, TransactionType.INCOME));
        }
        result.add(new AiGeneratedTransaction(prompt, 25_000L, TransactionType.EXPENSE));
        return result;
    }
}
