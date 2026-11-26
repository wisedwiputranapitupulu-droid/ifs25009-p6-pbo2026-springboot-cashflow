package org.delcom.ifs25009springbootcashflow.domain.repository;

import org.delcom.ifs25009springbootcashflow.domain.exception.EntityNotFoundException;
import java.util.List;
import java.util.function.Predicate;

/** Port generik CRUD. */
public interface IRepository<T, ID> {
    /** Menyimpan entity baru (id 0) atau memperbarui entity yang sudah ada. */
    T save(T entity) throws EntityNotFoundException;

    T findById(ID id) throws EntityNotFoundException;

    List<T> findAll();

    List<T> findBy(Predicate<T> predicate);

    void deleteById(ID id) throws EntityNotFoundException;
}
