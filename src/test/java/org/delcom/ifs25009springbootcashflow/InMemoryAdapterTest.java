package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.delcom.ifs25009springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.UserRepository;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;

class InMemoryAdapterTest {
    @Test
    void transactionRepositoryCrud() throws Exception {
        TransactionRepository repo = new TransactionRepository();
        Transaction a = repo.save(new Transaction(0, 1, "a", 10, TransactionType.INCOME, null, null));
        assertEquals(1, a.getId());
        assertNotNull(a.getCreatedAt());
        LocalDateTime when = LocalDateTime.of(2026, 1, 1, 0, 0);
        Transaction b = repo.save(new Transaction(0, 2, "b", 20, TransactionType.EXPENSE, null, when));
        assertEquals(when, b.getCreatedAt());
        assertSame(a, repo.findById(1));
        assertEquals(2, repo.findAll().size());
        assertEquals(1, repo.findByUserId(2).size());
        assertEquals(1, repo.findBy(t -> t.getAmount() > 15).size());

        a.setDescription("a2");
        assertEquals("a2", repo.save(a).getDescription());
        Transaction ghost = new Transaction(99, 1, "g", 1, TransactionType.INCOME, null, when);
        assertThrows(EntityNotFoundException.class, () -> repo.save(ghost));
        assertThrows(EntityNotFoundException.class, () -> repo.findById(99));
        repo.deleteById(1);
        assertThrows(EntityNotFoundException.class, () -> repo.deleteById(1));
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void userRepositoryCrud() throws Exception {
        UserRepository repo = new UserRepository();
        User u = repo.save(new User(0, "wise", "h", "Wise", Role.USER, null));
        assertEquals(1, u.getId());
        assertTrue(repo.findByUsername("wise").isPresent());
        assertTrue(repo.findByUsername("nope").isEmpty());
        assertSame(u, repo.findById(1));
        assertEquals(1, repo.findBy(x -> x.getRole() == Role.USER).size());
        repo.deleteById(1);
        assertTrue(repo.findAll().isEmpty());
    }
}
