package org.delcom.ifs25009springbootcashflow.adapter.repository;

import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Implementasi generik in-memory (untuk unit test). */
public abstract class InMemoryRepository<T, ID> implements IRepository<T, ID> {
    protected final Map<ID, T> store = new LinkedHashMap<>();

    protected abstract String entityName();

    protected abstract ID idOf(T entity);

    protected abstract boolean isNew(T entity);

    protected abstract void assignId(T entity);

    @Override
    public T save(T entity) throws EntityNotFoundException {
        if (isNew(entity)) {
            assignId(entity);
        } else if (!store.containsKey(idOf(entity))) {
            throw new EntityNotFoundException(entityName(), idOf(entity));
        }
        store.put(idOf(entity), entity);
        return entity;
    }

    @Override
    public T findById(ID id) throws EntityNotFoundException {
        T entity = store.get(id);
        if (entity == null) {
            throw new EntityNotFoundException(entityName(), id);
        }
        return entity;
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<T> findBy(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).toList();
    }

    @Override
    public void deleteById(ID id) throws EntityNotFoundException {
        if (store.remove(id) == null) {
            throw new EntityNotFoundException(entityName(), id);
        }
    }
}
