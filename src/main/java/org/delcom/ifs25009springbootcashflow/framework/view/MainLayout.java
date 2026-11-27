package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;

/** Layout utama: sidebar, menu avatar (Profil / Keluar), dan pengecekan login global. */
public class MainLayout extends AppLayout implements BeforeEnterObserver {
    private final transient IUserRepository users;
    private final transient FinancePresenter presenter;
    private int builtForUser = -1;

    public MainLayout(IUserRepository users, FinancePresenter presenter) {
        this.users = users;
        this.presenter = presenter;
        setPrimarySection(Section.DRAWER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User user = AuthSession.currentUser(users);
        if (user == null) {
            event.forwardTo(LoginView.class);
            return;
        }
        if (builtForUser != user.getId()) {
            build(user);
            builtForUser = user.getId();
        }
    }

    private void build(User user) {
        Span title = new Span("Cashflow");
        title.addClassName("app-title");
        addToDrawer(new Div(title));

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Cashflow", FinanceView.class, VaadinIcon.WALLET.create()));
        nav.addItem(new SideNavItem("Profil", ProfileView.class, VaadinIcon.USER.create()));
        if (user.isAdmin()) {
            nav.addItem(new SideNavItem("Pengguna", UsersView.class, VaadinIcon.USERS.create()));
        }
        addToDrawer(new Scroller(nav));

        Avatar avatar = new Avatar(user.getFullName());
        String photo = presenter.fileUrl(user.getPhotoPath());
        if (photo != null) {
            avatar.setImage(photo);
        }
        avatar.addClassName("pointer");
        MenuBar menu = new MenuBar();
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        var root = menu.addItem(avatar);
        root.getSubMenu().addItem(user.getFullName() + " (" + user.getRole() + ")").setEnabled(false);
        root.getSubMenu().addItem("Profil", e -> UI.getCurrent().navigate(ProfileView.class));
        root.getSubMenu().addItem("Keluar", e -> {
            AuthSession.logout();
            UI.getCurrent().getPage().setLocation("login");
        });
        Div spacer = new Div();
        spacer.addClassName("grow");
        Div header = new Div(spacer, menu);
        header.addClassName("app-header");
        header.getElement().setAttribute("role", "banner");
        addToNavbar(new DrawerToggle(), header);
    }
}
