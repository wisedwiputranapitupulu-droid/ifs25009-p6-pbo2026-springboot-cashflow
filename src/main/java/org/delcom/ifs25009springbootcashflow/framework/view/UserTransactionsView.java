package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.domain.entity.Balance;
import org.delcom.ifs25009springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;

/** Transaksi pengguna lain, read-only (ADMIN saja). */
@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna | Cashflow")
public class UserTransactionsView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {
    private final transient FinanceUseCase finance;
    private final transient UserUseCase userUseCase;
    private final transient IUserRepository users;
    private final transient FinancePresenter presenter;
    private final H1 title = new H1("Transaksi pengguna");
    private final Span summary = new Span();
    private final Grid<Transaction> grid;
    private int targetUserId;

    public UserTransactionsView(FinanceUseCase finance, UserUseCase userUseCase, IUserRepository users,
                                FinancePresenter presenter) {
        this.finance = finance;
        this.userUseCase = userUseCase;
        this.users = users;
        this.presenter = presenter;
        getElement().setAttribute("role", "main");
        setSizeFull();
        grid = TransactionGrid.create(presenter, null);
        Button back = new Button("Kembali", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(UsersView.class));
        add(back, title, summary, grid);
        expand(grid);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        targetUserId = parameter == null ? 0 : parameter;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User user = AuthSession.currentUser(users);
        if (user == null) {
            event.forwardTo(LoginView.class);
        } else if (!user.isAdmin()) {
            event.forwardTo(FinanceView.class);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        try {
            User target = userUseCase.getUser(targetUserId);
            title.setText("Transaksi " + target.getFullName() + " (" + target.getUsername() + ") - read-only");
            grid.setItems(finance.listTransactions(targetUserId, null, null, null));
            Balance b = finance.getBalance(targetUserId);
            summary.setText("Pemasukan " + presenter.formatMoney(b.income()) + " | Pengeluaran "
                    + presenter.formatMoney(b.expense()) + " | " + presenter.formatBalance(b));
        } catch (DomainException e) {
            Notify.error(e.getMessage());
            UI.getCurrent().navigate(UsersView.class);
        }
    }
}
