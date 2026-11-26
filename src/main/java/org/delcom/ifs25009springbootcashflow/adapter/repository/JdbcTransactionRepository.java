package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.sql.DataSource;

public class JdbcTransactionRepository implements ITransactionRepository {
    private static final String COLUMNS = "id, user_id, description, amount, type, proof_path, created_at";
    private static final String ORDER = " ORDER BY created_at DESC, id DESC";
    private final DataSource dataSource;

    public JdbcTransactionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(rs.getInt("id"), rs.getInt("user_id"), rs.getString("description"),
                rs.getLong("amount"), TransactionType.valueOf(rs.getString("type")),
                rs.getString("proof_path"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    @Override
    public Transaction save(Transaction t) throws EntityNotFoundException {
        try (Connection c = dataSource.getConnection()) {
            if (t.getId() == 0) {
                if (t.getCreatedAt() == null) {
                    t.setCreatedAt(LocalDateTime.now());
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transactions (user_id, description, amount, type, proof_path, created_at) VALUES (?,?,?,?,?,?)",
                        new String[]{"id"})) {
                    ps.setInt(1, t.getUserId());
                    ps.setString(2, t.getDescription());
                    ps.setLong(3, t.getAmount());
                    ps.setString(4, t.getType().name());
                    ps.setString(5, t.getProofPath());
                    ps.setTimestamp(6, Timestamp.valueOf(t.getCreatedAt()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        t.setId(keys.getInt(1));
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE transactions SET user_id=?, description=?, amount=?, type=?, proof_path=? WHERE id=?")) {
                    ps.setInt(1, t.getUserId());
                    ps.setString(2, t.getDescription());
                    ps.setLong(3, t.getAmount());
                    ps.setString(4, t.getType().name());
                    ps.setString(5, t.getProofPath());
                    ps.setInt(6, t.getId());
                    if (ps.executeUpdate() == 0) {
                        throw new EntityNotFoundException("Transaksi", t.getId());
                    }
                }
            }
            return t;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Transaction findById(Integer id) throws EntityNotFoundException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM transactions WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new EntityNotFoundException("Transaksi", id);
                }
                return map(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Transaction> query(String where, Integer param) {
        List<Transaction> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM transactions" + where + ORDER)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        return query("", null);
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return query(" WHERE user_id=?", userId);
    }

    @Override
    public List<Transaction> findBy(Predicate<Transaction> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    @Override
    public void deleteById(Integer id) throws EntityNotFoundException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new EntityNotFoundException("Transaksi", id);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
