package org.delcom.ifs25009springbootcashflow.domain.entity;

public enum TransactionTypeFilter {
    ALL("Semua"), INCOME("Pemasukan"), EXPENSE("Pengeluaran");

    private final String label;

    TransactionTypeFilter(String label) { this.label = label; }

    public String label() { return label; }

    public boolean matches(Transaction t) {
        return switch (this) {
            case ALL -> true;
            case INCOME -> t.getType() == TransactionType.INCOME;
            case EXPENSE -> t.getType() == TransactionType.EXPENSE;
        };
    }
}
