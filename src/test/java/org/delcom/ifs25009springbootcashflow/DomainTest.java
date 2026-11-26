package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static Transaction tx(int id, String desc, long amount, TransactionType type, int minute) {
        return new Transaction(id, 1, desc, amount, type, null, LocalDateTime.of(2026, 1, 1, 10, minute));
    }

    @Test
    void transactionAccessors() {
        Transaction t = new Transaction();
        LocalDateTime now = LocalDateTime.now();
        t.setId(5);
        t.setUserId(2);
        t.setDescription("Kopi");
        t.setAmount(15000);
        t.setType(TransactionType.EXPENSE);
        t.setProofPath("a.png");
        t.setCreatedAt(now);
        assertEquals(5, t.getId());
        assertEquals(2, t.getUserId());
        assertEquals("Kopi", t.getDescription());
        assertEquals(15000, t.getAmount());
        assertEquals(TransactionType.EXPENSE, t.getType());
        assertEquals("a.png", t.getProofPath());
        assertEquals(now, t.getCreatedAt());
    }

    @Test
    void userAccessors() {
        User u = new User();
        u.setId(3);
        u.setUsername("wise");
        u.setPasswordHash("h");
        u.setFullName("Wise");
        u.setRole(Role.USER);
        u.setPhotoPath("p.png");
        assertEquals(3, u.getId());
        assertEquals("wise", u.getUsername());
        assertEquals("h", u.getPasswordHash());
        assertEquals("Wise", u.getFullName());
        assertEquals(Role.USER, u.getRole());
        assertEquals("p.png", u.getPhotoPath());
        assertFalse(u.isAdmin());
        u.setRole(Role.ADMIN);
        assertTrue(u.isAdmin());
    }

    @Test
    void enumsAndRecords() {
        assertEquals("Pemasukan", TransactionType.INCOME.label());
        assertEquals("Pengeluaran", TransactionType.EXPENSE.label());
        assertEquals("Semua", TransactionTypeFilter.ALL.label());
        Balance b = new Balance(2, 1000, 400);
        assertEquals(600, b.balance());
        AiGeneratedTransaction ai = new AiGeneratedTransaction("x", 1, TransactionType.INCOME);
        assertEquals("x", ai.description());
        assertEquals(Role.USER, Role.valueOf("USER"));
    }

    @Test
    void typeFilterMatches() {
        Transaction in = tx(1, "a", 1, TransactionType.INCOME, 0);
        Transaction out = tx(2, "b", 1, TransactionType.EXPENSE, 0);
        assertTrue(TransactionTypeFilter.ALL.matches(in) && TransactionTypeFilter.ALL.matches(out));
        assertTrue(TransactionTypeFilter.INCOME.matches(in));
        assertFalse(TransactionTypeFilter.INCOME.matches(out));
        assertTrue(TransactionTypeFilter.EXPENSE.matches(out));
        assertFalse(TransactionTypeFilter.EXPENSE.matches(in));
    }

    @Test
    void sortOptions() {
        Transaction a = tx(1, "Beras", 300, TransactionType.EXPENSE, 1);
        Transaction b = tx(2, "Anggur", 100, TransactionType.INCOME, 2);
        Transaction c = tx(3, "Cabai", 200, TransactionType.EXPENSE, 3);
        for (SortOption o : SortOption.values()) {
            assertFalse(o.label().isEmpty());
        }
        assertEquals(List.of(3, 2, 1), ids(sorted(SortOption.NEWEST_FIRST, a, b, c)));
        assertEquals(List.of(1, 2, 3), ids(sorted(SortOption.OLDEST_FIRST, c, b, a)));
        assertEquals(List.of(1, 3, 2), ids(sorted(SortOption.AMOUNT_HIGHEST, a, b, c)));
        assertEquals(List.of(2, 3, 1), ids(sorted(SortOption.AMOUNT_LOWEST, a, b, c)));
        assertEquals(List.of(2, 1, 3), ids(sorted(SortOption.DESCRIPTION_AZ, a, b, c)));
        assertEquals(List.of(2, 1, 3), ids(sorted(SortOption.TYPE_ORDER, a, b, c)));
    }

    private static List<Transaction> sorted(SortOption option, Transaction... items) {
        List<Transaction> list = new ArrayList<>(List.of(items));
        list.sort(option.comparator());
        return list;
    }

    private static List<Integer> ids(List<Transaction> list) {
        return list.stream().map(Transaction::getId).toList();
    }

    @Test
    void passwordHasher() {
        String h = PasswordHasher.hash("abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", h);
        assertTrue(PasswordHasher.verify("abc", h));
        assertFalse(PasswordHasher.verify("abd", h));
        assertFalse(PasswordHasher.verify(null, h));
        assertFalse(PasswordHasher.verify("abc", null));
        assertNotEquals(h, PasswordHasher.hash("abcd"));
    }

    @Test
    void exceptions() {
        DomainException base = new DomainException("x");
        assertEquals("x", base.getMessage());
        assertEquals("User dengan ID 9 tidak ditemukan", new EntityNotFoundException("User", 9).getMessage());
        assertTrue(new ValidationException("v") instanceof DomainException);
        assertTrue(new EntityNotFoundException("a", 1) instanceof DomainException);
    }
}
