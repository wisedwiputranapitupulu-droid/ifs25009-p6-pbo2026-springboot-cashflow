package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25009springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Cashflow")
public class FinanceView extends VerticalLayout {
    private final transient FinanceUseCase finance;
    private final transient FinanceAiUseCase aiUseCase;
    private final transient FinancePresenter presenter;
    private final transient FileStorage fileStorage;
    private final transient IUserRepository users;

    private final Span totalCount = new Span();
    private final Span totalIncome = new Span();
    private final Span totalExpense = new Span();
    private final Span totalBalance = new Span();
    private final TextField search = new TextField();
    private final ComboBox<TransactionTypeFilter> filter = new ComboBox<>();
    private final ComboBox<SortOption> sort = new ComboBox<>();
    private final Grid<Transaction> grid;
    private int userId;

    public FinanceView(FinanceUseCase finance, FinanceAiUseCase aiUseCase, FinancePresenter presenter,
                       FileStorage fileStorage, IUserRepository users) {
        this.finance = finance;
        this.aiUseCase = aiUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.users = users;
        setSizeFull();
        setPadding(true);

        grid = TransactionGrid.create(presenter, this::confirmDelete);
        add(summaryCards(), toolbar(), grid);
        expand(grid);
    }

    private HorizontalLayout summaryCards() {
        HorizontalLayout row = new HorizontalLayout(
                card("Total transaksi", totalCount, "card-neutral"),
                card("Pemasukan", totalIncome, "card-income"),
                card("Pengeluaran", totalExpense, "card-expense"),
                card("Saldo", totalBalance, "card-balance"));
        row.setWidthFull();
        row.getStyle().set("flex-wrap", "wrap");
        return row;
    }

    private Div card(String label, Span value, String cls) {
        Span l = new Span(label);
        l.addClassName("summary-label");
        value.addClassName("summary-value");
        Div d = new Div(l, value);
        d.addClassNames("summary-card", cls);
        return d;
    }

    private FlexLayout toolbar() {
        search.setPlaceholder("Cari keterangan...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> refresh());

        filter.setItems(TransactionTypeFilter.values());
        filter.setItemLabelGenerator(TransactionTypeFilter::label);
        filter.setValue(TransactionTypeFilter.ALL);
        filter.setAllowCustomValue(false);
        filter.setLabel("Tipe");
        filter.addValueChangeListener(e -> refresh());

        sort.setItems(SortOption.values());
        sort.setItemLabelGenerator(SortOption::label);
        sort.setValue(SortOption.NEWEST_FIRST);
        sort.setLabel("Urutan");
        sort.addValueChangeListener(e -> refresh());

        Button reload = new Button("Muat ulang", VaadinIcon.REFRESH.create(), e -> refresh());
        Button ai = new Button("Generate AI", VaadinIcon.MAGIC.create(), e -> openAiDialog());
        ai.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        Button income = new Button("Pemasukan", VaadinIcon.PLUS.create(), e -> openAddDialog(TransactionType.INCOME));
        income.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        Button expense = new Button("Pengeluaran", VaadinIcon.MINUS.create(), e -> openAddDialog(TransactionType.EXPENSE));
        expense.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        FlexLayout bar = new FlexLayout(search, filter, sort, reload, ai, income, expense);
        bar.setWidthFull();
        bar.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        bar.addClassName("toolbar");
        return bar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        User user = AuthSession.currentUser(users);
        if (user != null) {
            userId = user.getId();
            refresh();
        }
    }

    private void refresh() {
        if (userId == 0) {
            return;
        }
        grid.setItems(finance.listTransactions(userId, search.getValue(), filter.getValue(), sort.getValue()));
        Balance b = finance.getBalance(userId);
        totalCount.setText(String.valueOf(b.count()));
        totalIncome.setText(presenter.formatMoney(b.income()));
        totalExpense.setText(presenter.formatMoney(b.expense()));
        totalBalance.setText(presenter.formatMoney(b.balance()));
    }

    // ---------- Tambah transaksi ----------
    private void openAddDialog(TransactionType type) {
        AppDialog dialog = new AppDialog("Tambah " + type.label(), type.label());
        dialog.setWidth("460px");
        TextField description = new TextField("Keterangan");
        description.setWidthFull();
        TextField amount = new TextField("Jumlah (Rp)");
        amount.setHelperText("Angka bulat, contoh: 25000");
        amount.setWidthFull();

        AtomicReference<byte[]> proofData = new AtomicReference<>();
        AtomicReference<String> proofName = new AtomicReference<>();
        Upload upload = new Upload(UploadHandler.inMemory((meta, data) -> {
            proofName.set(meta.fileName());
            proofData.set(data);
        }));
        upload.setMaxFiles(1);
        upload.setMaxFileSize((int) FileStorage.MAX_BYTES);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        upload.setDropLabel(new Span("Bukti transaksi (opsional) - seret gambar ke sini"));
        upload.addFileRemovedListener(e -> {
            proofData.set(null);
            proofName.set(null);
        });

        ProgressBar progress = new ProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);

        VerticalLayout form = new VerticalLayout(description, amount, upload, progress);
        form.setPadding(false);
        dialog.add(form);

        Button cancel = new Button("Batal", e -> dialog.close());
        Button save = new Button("Simpan");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            progress.setVisible(true);
            save.setEnabled(false);
            String storedProof = null;
            try {
                if (proofData.get() != null) {
                    storedProof = fileStorage.save(proofName.get(), proofData.get());
                }
                Transaction t = finance.addTransaction(userId, description.getValue(), amount.getValue(),
                        type, storedProof);
                Notify.success(presenter.savedMessage(t));
                dialog.close();
                refresh();
            } catch (DomainException ex) {
                fileStorage.delete(storedProof);
                Notify.error(ex.getMessage());
            } finally {
                progress.setVisible(false);
                save.setEnabled(true);
            }
        });
        dialog.addActions(cancel, save);
        dialog.open();
    }

    // ---------- Hapus ----------
    private void confirmDelete(Transaction t) {
        AppDialog dialog = new AppDialog("Hapus transaksi", "Konfirmasi");
        dialog.add(new Span("Hapus " + presenter.formatTransaction(t) + " ?"));
        Button cancel = new Button("Batal", e -> dialog.close());
        Button delete = new Button("Hapus");
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        delete.addClickListener(e -> {
            try {
                finance.deleteTransaction(userId, t.getId());
                fileStorage.delete(t.getProofPath());
                Notify.success(presenter.deletedMessage(t));
                dialog.close();
                refresh();
            } catch (DomainException ex) {
                Notify.error(ex.getMessage());
            }
        });
        dialog.addActions(cancel, delete);
        dialog.open();
    }

    // ---------- Generate AI ----------
    private void openAiDialog() {
        AppDialog dialog = new AppDialog("Generate AI", "Catatan keuangan");
        dialog.setWidth("680px");
        TextArea prompt = new TextArea("Ceritakan transaksi Anda");
        prompt.setPlaceholder("Contoh: Hari ini gajian 5 juta, beli makan siang 25 ribu dan bayar listrik 350 ribu");
        prompt.setWidthFull();
        prompt.setMinHeight("110px");

        ProgressBar progress = new ProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);
        Button process = new Button("Proses AI", VaadinIcon.MAGIC.create());
        process.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Grid<AiGeneratedTransaction> preview = new Grid<>();
        preview.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_COMPACT);
        preview.addColumn(AiGeneratedTransaction::description).setHeader("Keterangan").setFlexGrow(1);
        preview.addColumn(t -> presenter.formatMoney(t.amount())).setHeader("Jumlah").setAutoWidth(true);
        preview.addColumn(t -> t.type().label()).setHeader("Tipe").setAutoWidth(true);
        preview.setHeight("220px");

        VerticalLayout body = new VerticalLayout(prompt, process, progress, preview);
        body.setPadding(false);
        dialog.add(body);

        Button close = new Button("Tutup", e -> dialog.close());
        Button add = new Button("Tambahkan ke Catatan");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.setEnabled(false);
        AtomicReference<List<AiGeneratedTransaction>> generated = new AtomicReference<>(List.of());

        process.addClickListener(e -> {
            UI ui = UI.getCurrent();
            String text = prompt.getValue();
            process.setEnabled(false);
            add.setEnabled(false);
            progress.setVisible(true);
            ui.setPollInterval(400);
            CompletableFuture.supplyAsync(() -> {
                try {
                    return aiUseCase.generateTransactions(text);
                } catch (DomainException ex) {
                    throw new CompletionException(ex);
                }
            }).whenComplete((result, error) -> ui.access(() -> {
                ui.setPollInterval(-1);
                progress.setVisible(false);
                process.setEnabled(true);
                if (error != null) {
                    Throwable cause = error.getCause() != null ? error.getCause() : error;
                    Notify.error(cause.getMessage());
                } else {
                    generated.set(result);
                    preview.setItems(result);
                    add.setEnabled(!result.isEmpty());
                }
            }));
        });
        add.addClickListener(e -> {
            try {
                aiUseCase.saveGeneratedTransactions(userId, generated.get());
                Notify.success(generated.get().size() + " transaksi ditambahkan");
                dialog.close();
                refresh();
            } catch (DomainException ex) {
                Notify.error(ex.getMessage());
            }
        });
        dialog.addActions(close, add);
        dialog.open();
    }
}
