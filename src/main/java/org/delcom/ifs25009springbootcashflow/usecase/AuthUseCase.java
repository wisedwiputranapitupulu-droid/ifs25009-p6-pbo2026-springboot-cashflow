package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.Role;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;

public class AuthUseCase {
    private final IUserRepository users;

    public AuthUseCase(IUserRepository users) {
        this.users = users;
    }

    public User register(String username, String password, String fullName) throws DomainException {
        if (username == null || !username.trim().matches("[A-Za-z0-9_.-]{3,50}")) {
            throw new ValidationException("Username 3-50 karakter (huruf, angka, titik, minus, garis bawah)");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password minimal 6 karakter");
        }
        String uname = username.trim();
        if (users.findByUsername(uname).isPresent()) {
            throw new ValidationException("Username sudah digunakan");
        }
        String name = (fullName == null || fullName.isBlank()) ? uname : fullName.trim();
        return users.save(new User(0, uname, PasswordHasher.hash(password), name, Role.USER, null));
    }

    public User login(String username, String password) throws DomainException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Username dan password wajib diisi");
        }
        User user = users.findByUsername(username.trim()).orElse(null);
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new ValidationException("Username atau password salah");
        }
        return user;
    }

    /** Membuat admin dari ROOT_USERNAME / ROOT_PASSWORD bila belum ada. */
    public User ensureRootAdmin(String username, String password) throws DomainException {
        User existing = users.findByUsername(username).orElse(null);
        if (existing != null) {
            return existing;
        }
        return users.save(new User(0, username, PasswordHasher.hash(password), "Administrator", Role.ADMIN, null));
    }
}
