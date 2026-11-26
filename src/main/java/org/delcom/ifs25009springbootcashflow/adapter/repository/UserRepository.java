package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import java.util.Optional;

public class UserRepository extends InMemoryRepository<User, Integer> implements IUserRepository {
    private int sequence = 0;

    @Override protected String entityName() { return "User"; }
    @Override protected Integer idOf(User u) { return u.getId(); }
    @Override protected boolean isNew(User u) { return u.getId() == 0; }
    @Override protected void assignId(User u) { u.setId(++sequence); }

    @Override
    public Optional<User> findByUsername(String username) {
        return store.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }
}
