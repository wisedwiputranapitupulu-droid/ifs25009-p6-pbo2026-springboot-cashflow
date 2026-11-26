package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;

/** Daftar pengguna (ADMIN saja). */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("Pengguna | Cashflow")
public class UsersView extends VerticalLayout implements BeforeEnterObserver {
    private final transient UserUseCase userUseCase;
    private final transient IUserRepository users;
    private final Grid<User> grid = new Grid<>();

    public UsersView(UserUseCase userUseCase, IUserRepository users) {
        this.userUseCase = userUseCase;
        this.users = users;
        setSizeFull();
        GridStyles.apply(grid);
        grid.addColumn(User::getId).setHeader("ID").setSortable(true).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(User::getFullName).setHeader("Nama").setSortable(true).setFlexGrow(1);
        grid.addColumn(u -> u.getRole().name()).setHeader("Role").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(u -> {
            Button b = new Button("Lihat transaksi",
                    e -> UI.getCurrent().navigate("user-transactions/" + u.getId()));
            b.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return b;
        }).setHeader("Aksi").setAutoWidth(true).setFlexGrow(0);
        add(new H2("Daftar Pengguna"), grid);
        expand(grid);
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
        grid.setItems(userUseCase.listUsers());
    }
}
