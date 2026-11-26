package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tes JDBC memakai H2 in-memory. */
class JdbcRepositoryTest {
    private JdbcUserRepository users;
    private JdbcTransactionRepository transactions;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource = ds;
        SchemaInitializer.initialize(ds);
        SchemaInitializer.initialize(ds); // idempotent (IF NOT EXISTS)
        users = new JdbcUserRepository(ds);
        transactions = new JdbcTransactionRepository(ds);
    }

    @Test
    void userCrud() throws Exception {
        User u = users.save(new User(0, "wise", "hash", "Wise", Role.USER, null));
        assertTrue(u.getId() > 0);
        User loaded = users.findById(u.getId());
        assertEquals("wise", loaded.getUsername());
        assertEquals(Role.USER, loaded.getRole());
        assertTrue(users.findByUsername("wise").isPresent());
        assertTrue(users.findByUsername("nope").isEmpty());

        loaded.setFullName("Wise D");
        loaded.setPhotoPath("p.png");
        users.save(loaded);
        assertEquals("Wise D", users.findById(u.getId()).getFullName());
        assertEquals("p.png", users.findById(u.getId()).getPhotoPath());

        users.save(new User(0, "admin", "h", "Admin", Role.ADMIN, null));
        assertEquals(2, users.findAll().size());
        assertEquals(1, users.findBy(x -> x.getRole() == Role.ADMIN).size());

        assertThrows(EntityNotFoundException.class, () -> users.findById(999));
        assertThrows(EntityNotFoundException.class,
                () -> users.save(new User(999, "ghost", "h", "G", Role.USER, null)));
        users.deleteById(loaded.getId());
        assertThrows(EntityNotFoundException.class, () -> users.deleteById(loaded.getId()));
    }

    @Test
    void transactionCrudAndOrdering() throws Exception {
        User u = users.save(new User(0, "wise", "hash", "Wise", Role.USER, null));
        User other = users.save(new User(0, "andi", "hash", "Andi", Role.USER, null));
        Transaction older = transactions.save(new Transaction(0, u.getId(), "lama", 100, TransactionType.INCOME,
                null, LocalDateTime.of(2026, 1, 1, 10, 0)));
        Transaction newer = transactions.save(new Transaction(0, u.getId(), "baru", 50, TransactionType.EXPENSE,
                "b.png", LocalDateTime.of(2026, 1, 2, 10, 0)));
        Transaction noDate = transactions.save(new Transaction(0, other.getId(), "tanpa tanggal", 5,
                TransactionType.INCOME, null, null));
        assertTrue(noDate.getCreatedAt() != null);

        var mine = transactions.findByUserId(u.getId());
        assertEquals(2, mine.size());
        assertEquals("baru", mine.get(0).getDescription()); // terbaru di atas
        assertEquals("b.png", mine.get(0).getProofPath());
        assertEquals(TransactionType.EXPENSE, mine.get(0).getType());
        assertEquals(3, transactions.findAll().size());
        assertEquals(1, transactions.findBy(t -> t.getAmount() == 100).size());

        older.setDescription("lama (edit)");
        older.setAmount(120);
        transactions.save(older);
        assertEquals("lama (edit)", transactions.findById(older.getId()).getDescription());
        assertEquals(120, transactions.findById(older.getId()).getAmount());

        assertThrows(EntityNotFoundException.class, () -> transactions.findById(999));
        assertThrows(EntityNotFoundException.class, () -> transactions.save(
                new Transaction(999, u.getId(), "x", 1, TransactionType.INCOME, null, LocalDateTime.now())));
        transactions.deleteById(newer.getId());
        assertThrows(EntityNotFoundException.class, () -> transactions.deleteById(newer.getId()));
        assertEquals(1, transactions.findByUserId(u.getId()).size());
    }

    @Test
    void sqlErrorsAreWrapped() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().execute("DROP TABLE transactions");
            c.createStatement().execute("DROP TABLE users");
        }
        assertThrows(IllegalStateException.class, () -> users.findAll());
        assertThrows(IllegalStateException.class, () -> users.findByUsername("x"));
        assertThrows(IllegalStateException.class, () -> users.findById(1));
        assertThrows(IllegalStateException.class, () -> users.deleteById(1));
        assertThrows(IllegalStateException.class, () -> users.save(new User(0, "x", "h", "x", Role.USER, null)));
        assertThrows(IllegalStateException.class, () -> users.save(new User(1, "x", "h", "x", Role.USER, null)));
        assertThrows(IllegalStateException.class, () -> transactions.findAll());
        assertThrows(IllegalStateException.class, () -> transactions.findById(1));
        assertThrows(IllegalStateException.class, () -> transactions.deleteById(1));
        assertThrows(IllegalStateException.class, () -> transactions.save(
                new Transaction(0, 1, "x", 1, TransactionType.INCOME, null, LocalDateTime.now())));
        assertThrows(IllegalStateException.class, () -> transactions.save(
                new Transaction(1, 1, "x", 1, TransactionType.INCOME, null, LocalDateTime.now())));
        JdbcDataSource broken = new JdbcDataSource();
        broken.setURL("jdbc:h2:tcp://127.0.0.1:1/none");
        assertThrows(IllegalStateException.class, () -> SchemaInitializer.initialize(broken));
    }
}
