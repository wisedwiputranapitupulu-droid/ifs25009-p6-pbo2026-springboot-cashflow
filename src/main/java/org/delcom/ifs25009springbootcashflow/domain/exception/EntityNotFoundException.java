package org.delcom.ifs25009springbootcashflow.domain.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entity, Object id) {
        super(entity + " dengan ID " + id + " tidak ditemukan");
    }
}
