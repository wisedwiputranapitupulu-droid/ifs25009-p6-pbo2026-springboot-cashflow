package org.delcom.ifs25009springbootcashflow.usecase;

import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import java.util.Comparator;
import java.util.List;

public class UserUseCase {
    private final IUserRepository users;

    public UserUseCase(IUserRepository users) {
        this.users = users;
    }

    public List<User> listUsers() {
        return users.findAll().stream().sorted(Comparator.comparingInt(User::getId)).toList();
    }

    public User getUser(int id) throws DomainException {
        if (id <= 0) {
            throw new ValidationException("ID tidak valid");
        }
        return users.findById(id);
    }

    public User updateProfile(int id, String fullName, String newPassword) throws DomainException {
        User user = getUser(id);
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Nama lengkap tidak boleh kosong");
        }
        user.setFullName(fullName.trim());
        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                throw new ValidationException("Password minimal 6 karakter");
            }
            user.setPasswordHash(PasswordHasher.hash(newPassword));
        }
        return users.save(user);
    }

    public User updatePhoto(int id, String photoPath) throws DomainException {
        User user = getUser(id);
        user.setPhotoPath(photoPath);
        return users.save(user);
    }
}
