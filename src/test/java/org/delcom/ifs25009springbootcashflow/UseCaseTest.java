package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.delcom.ifs25009springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25009springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.UserRepository;
import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UseCaseTest {
    private UserRepository users;
    private TransactionRepository transactions;
    private AuthUseCase auth;
    private FinanceUseCase finance;
    private FinanceAiUseCase ai;
    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        users = new UserRepository();
        transactions = new TransactionRepository();
        auth = new AuthUseCase(users);
        finance = new FinanceUseCase(transactions);
        ai = new FinanceAiUseCase(new InMemoryFinanceAiService(), transactions);
        userUseCase = new UserUseCase(users);
    }

    // ---------- Auth ----------
    @Test
    void registerAndLogin() throws Exception {
        User u = auth.register(" wise ", "secret1", " Wise Napitupulu ");
        assertEquals("wise", u.getUsername());
        assertEquals("Wise Napitupulu", u.getFullName());
        assertEquals(PasswordHasher.hash("secret1"), u.getPasswordHash());
        assertEquals(u.getId(), auth.login("wise", "secret1").getId());
        assertEquals("andi", auth.register("andi", "secret1", null).getFullName());
        assertEquals("budi", auth.register("budi", "secret1", "  ").getFullName());
    }

    @Test
    void registerValidation() throws Exception {
        assertThrows(ValidationException.class, () -> auth.register(null, "secret1", "x"));
        assertThrows(ValidationException.class, () -> auth.register("a", "secret1", "x"));
        assertThrows(ValidationException.class, () -> auth.register("wise", null, "x"));
        assertThrows(ValidationException.class, () -> auth.register("wise", "123", "x"));
        auth.register("wise", "secret1", "x");
        assertThrows(ValidationException.class, () -> auth.register("wise", "secret1", "x"));
    }

    @Test
    void loginValidation() throws Exception {
        auth.register("wise", "secret1", "x");
        assertThrows(ValidationException.class, () -> auth.login(null, "p"));
        assertThrows(ValidationException.class, () -> auth.login(" ", "p"));
        assertThrows(ValidationException.class, () -> auth.login("wise", null));
        assertThrows(ValidationException.class, () -> auth.login("wise", ""));
        assertThrows(ValidationException.class, () -> auth.login("nobody", "secret1"));
        assertThrows(ValidationException.class, () -> auth.login("wise", "salah"));
    }

    @Test
    void ensureRootAdminIsIdempotent() throws Exception {
        User a = auth.ensureRootAdmin("admin", "admin");
        assertTrue(a.isAdmin());
        assertEquals(a.getId(), auth.ensureRootAdmin("admin", "other").getId());
        assertEquals(1, users.findAll().size());
    }

    // ---------- Finance ----------
    @Test
    void addTransactionValidation() throws Exception {
        assertThrows(ValidationException.class, () -> finance.addTransaction(0, "x", "10", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, null, "10", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "  ", "10", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", null, TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", " ", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", "abc", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", "12.5", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", "0", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", "-1", TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> finance.addTransaction(1, "x", "10", null, null));
        assertEquals(0, transactions.findAll().size());
    }

    @Test
    void listFilterSortAndBalance() throws Exception {
        finance.addTransaction(1, " Gaji ", " 5000000 ", TransactionType.INCOME, null);
        finance.addTransaction(1, "Makan siang", "25000", TransactionType.EXPENSE, "p.png");
        finance.addTransaction(1, "Listrik", "350000", TransactionType.EXPENSE, null);
        finance.addTransaction(2, "Milik orang lain", "1", TransactionType.INCOME, null);

        List<Transaction> all = finance.listTransactions(1, null, null, null);
        assertEquals(3, all.size());
        assertEquals("Gaji", all.get(2).getDescription());
        assertEquals(3, all.get(0).getId() - all.get(2).getId() + 1);
        assertEquals(1, finance.listTransactions(1, "  MAKAN ", TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST).size());
        assertEquals(1, finance.listTransactions(1, "", TransactionTypeFilter.INCOME, null).size());
        assertEquals(2, finance.listTransactions(1, null, TransactionTypeFilter.EXPENSE, SortOption.AMOUNT_HIGHEST).size());
        assertEquals("Listrik", finance.listTransactions(1, null, TransactionTypeFilter.EXPENSE, SortOption.AMOUNT_HIGHEST).get(0).getDescription());

        Balance b = finance.getBalance(1);
        assertEquals(3, b.count());
        assertEquals(5_000_000, b.income());
        assertEquals(375_000, b.expense());
        assertEquals(4_625_000, b.balance());
        assertEquals(0, finance.getBalance(99).balance());
    }

    @Test
    void getAndDelete() throws Exception {
        Transaction t = finance.addTransaction(1, "x", "10", TransactionType.INCOME, null);
        assertEquals(t.getId(), finance.getTransaction(t.getId()).getId());
        assertThrows(ValidationException.class, () -> finance.getTransaction(0));
        assertThrows(EntityNotFoundException.class, () -> finance.getTransaction(77));
        assertThrows(ValidationException.class, () -> finance.deleteTransaction(2, t.getId()));
        assertThrows(EntityNotFoundException.class, () -> finance.deleteTransaction(1, 77));
        finance.deleteTransaction(1, t.getId());
        assertEquals(0, finance.getBalance(1).count());
    }

    // ---------- AI ----------
    @Test
    void aiGenerateAndSave() throws Exception {
        assertThrows(ValidationException.class, () -> ai.generateTransactions(null));
        assertThrows(ValidationException.class, () -> ai.generateTransactions("  "));
        List<AiGeneratedTransaction> items = ai.generateTransactions(" gaji november ");
        assertEquals(2, items.size());
        List<Transaction> saved = ai.saveGeneratedTransactions(1, items);
        assertEquals(2, saved.size());
        assertEquals(2, finance.getBalance(1).count());
    }

    @Test
    void aiSaveValidation() throws Exception {
        TransactionType in = TransactionType.INCOME;
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(0, List.of(new AiGeneratedTransaction("x", 1, in))));
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(1, null));
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(1, List.of()));
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(1, List.of(new AiGeneratedTransaction(" ", 1, in))));
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(1, List.of(new AiGeneratedTransaction("x", 0, in))));
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(1, List.of(new AiGeneratedTransaction("x", 1, null))));
        // satu item buruk membatalkan seluruh simpanan
        assertThrows(ValidationException.class, () -> ai.saveGeneratedTransactions(1,
                List.of(new AiGeneratedTransaction("ok", 1, in), new AiGeneratedTransaction("bad", -1, in))));
        assertEquals(0, transactions.findAll().size());
    }

    // ---------- User ----------
    @Test
    void userUseCase() throws Exception {
        User a = auth.register("wise", "secret1", "Wise");
        auth.register("andi", "secret1", "Andi");
        assertEquals(2, userUseCase.listUsers().size());
        assertEquals(a.getId(), userUseCase.listUsers().get(0).getId());
        assertEquals(a.getId(), userUseCase.getUser(a.getId()).getId());
        assertThrows(ValidationException.class, () -> userUseCase.getUser(0));
        assertThrows(EntityNotFoundException.class, () -> userUseCase.getUser(99));

        String oldHash = a.getPasswordHash();
        userUseCase.updateProfile(a.getId(), " Wise D ", null);
        assertEquals("Wise D", userUseCase.getUser(a.getId()).getFullName());
        assertEquals(oldHash, userUseCase.getUser(a.getId()).getPasswordHash());
        userUseCase.updateProfile(a.getId(), "Wise D", "");
        assertEquals(oldHash, userUseCase.getUser(a.getId()).getPasswordHash());
        userUseCase.updateProfile(a.getId(), "Wise D", "newsecret");
        assertNotEquals(oldHash, userUseCase.getUser(a.getId()).getPasswordHash());
        assertThrows(ValidationException.class, () -> userUseCase.updateProfile(a.getId(), " ", null));
        assertThrows(ValidationException.class, () -> userUseCase.updateProfile(a.getId(), null, null));
        assertThrows(ValidationException.class, () -> userUseCase.updateProfile(a.getId(), "x", "123"));

        assertEquals("f.png", userUseCase.updatePhoto(a.getId(), "f.png").getPhotoPath());
    }
}
