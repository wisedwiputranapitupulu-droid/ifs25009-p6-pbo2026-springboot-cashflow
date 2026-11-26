package org.delcom.ifs25009springbootcashflow.domain.entity;

import java.util.Comparator;

public enum SortOption {
    NEWEST_FIRST("Terbaru di atas",
            Comparator.comparing(Transaction::getCreatedAt).thenComparingInt(Transaction::getId).reversed()),
    OLDEST_FIRST("Terlama di atas",
            Comparator.comparing(Transaction::getCreatedAt).thenComparingInt(Transaction::getId)),
    AMOUNT_HIGHEST("Jumlah terbesar",
            Comparator.comparingLong(Transaction::getAmount).reversed().thenComparingInt(Transaction::getId)),
    AMOUNT_LOWEST("Jumlah terkecil",
            Comparator.comparingLong(Transaction::getAmount).thenComparingInt(Transaction::getId)),
    DESCRIPTION_AZ("Keterangan A-Z",
            Comparator.comparing((Transaction t) -> t.getDescription().toLowerCase()).thenComparingInt(Transaction::getId)),
    TYPE_ORDER("Tipe (Pemasukan dulu)",
            Comparator.comparing(Transaction::getType).thenComparingInt(Transaction::getId));

    private final String label;
    private final Comparator<Transaction> comparator;

    SortOption(String label, Comparator<Transaction> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() { return label; }

    public Comparator<Transaction> comparator() { return comparator; }
}
