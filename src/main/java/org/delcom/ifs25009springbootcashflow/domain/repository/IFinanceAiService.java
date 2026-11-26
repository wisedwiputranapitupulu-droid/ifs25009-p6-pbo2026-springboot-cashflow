package org.delcom.ifs25009springbootcashflow.domain.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import java.util.List;

public interface IFinanceAiService {
    List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException;
}
