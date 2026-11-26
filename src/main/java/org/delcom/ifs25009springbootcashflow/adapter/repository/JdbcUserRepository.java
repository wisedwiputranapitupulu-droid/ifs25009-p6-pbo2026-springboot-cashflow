package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.sql.DataSource;

public class JdbcUserRepository implements IUserRepository {
    private static final String COLUMNS = "id, username, password_hash, full_name, role, photo_path";
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("full_name"), Role.valueOf(rs.getString("role")), rs.getString("photo_path"));
    }

    @Override
    public User save(User u) throws EntityNotFoundException {
        try (Connection c = dataSource.getConnection()) {
            if (u.getId() == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users (username, password_hash, full_name, role, photo_path) VALUES (?,?,?,?,?)",
                        new String[]{"id"})) {
                    ps.setString(1, u.getUsername());
                    ps.setString(2, u.getPasswordHash());
                    ps.setString(3, u.getFullName());
                    ps.setString(4, u.getRole().name());
                    ps.setString(5, u.getPhotoPath());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        u.setId(keys.getInt(1));
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET username=?, password_hash=?, full_name=?, role=?, photo_path=? WHERE id=?")) {
                    ps.setString(1, u.getUsername());
                    ps.setString(2, u.getPasswordHash());
                    ps.setString(3, u.getFullName());
                    ps.setString(4, u.getRole().name());
                    ps.setString(5, u.getPhotoPath());
                    ps.setInt(6, u.getId());
                    if (ps.executeUpdate() == 0) {
                        throw new EntityNotFoundException("User", u.getId());
                    }
                }
            }
            return u;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public User findById(Integer id) throws EntityNotFoundException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new EntityNotFoundException("User", id);
                }
                return map(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<User> findBy(Predicate<User> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    @Override
    public void deleteById(Integer id) throws EntityNotFoundException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new EntityNotFoundException("User", id);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
