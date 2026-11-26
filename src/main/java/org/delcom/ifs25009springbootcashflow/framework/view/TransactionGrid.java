package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import java.util.function.Consumer;

/** Grid transaksi yang dipakai FinanceView (dengan hapus) dan UserTransactionsView (read-only). */
public final class TransactionGrid {
    private TransactionGrid() { }

    public static Grid<Transaction> create(FinancePresenter presenter, Consumer<Transaction> onDelete) {
        Grid<Transaction> grid = new Grid<>();
        GridStyles.apply(grid);
        grid.addColumn(Transaction::getId).setHeader("ID").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setSortable(true).setFlexGrow(1);
        grid.addColumn(t -> presenter.formatMoney(t.getAmount()))
                .setHeader("Jumlah").setComparator(Transaction::getAmount).setAutoWidth(true)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);
        grid.addComponentColumn(t -> {
            Span badge = new Span(t.getType().label());
            badge.getElement().getThemeList().add(t.getType() == TransactionType.INCOME ? "badge success" : "badge error");
            return badge;
        }).setHeader("Tipe").setComparator(Transaction::getType).setAutoWidth(true);
        grid.addComponentColumn(t -> {
            String url = presenter.fileUrl(t.getProofPath());
            if (url == null) {
                return new Span("-");
            }
            Image thumb = new Image(url, "Bukti " + t.getId());
            thumb.addClassName("proof-thumb");
            thumb.addClickListener(e -> new ImagePreviewDialog(url, "Bukti transaksi #" + t.getId()).open());
            return thumb;
        }).setHeader("Bukti").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(t -> presenter.formatDate(t.getCreatedAt()))
                .setHeader("Waktu").setComparator(Transaction::getCreatedAt).setAutoWidth(true);
        if (onDelete != null) {
            grid.addComponentColumn(t -> {
                Button delete = new Button(VaadinIcon.TRASH.create(), e -> onDelete.accept(t));
                delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON);
                delete.setAriaLabel("Hapus transaksi " + t.getId());
                return delete;
            }).setHeader("Aksi").setAutoWidth(true).setFlexGrow(0);
        }
        return grid;
    }
}
