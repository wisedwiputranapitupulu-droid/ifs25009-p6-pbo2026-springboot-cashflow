package org.delcom.ifs25009springbootcashflow.domain.entity;

/** Ringkasan keuangan: jumlah transaksi, total pemasukan, total pengeluaran, dan saldo. */
public record Balance(int count, long income, long expense) {
    public long balance() { return income - expense; }
}
