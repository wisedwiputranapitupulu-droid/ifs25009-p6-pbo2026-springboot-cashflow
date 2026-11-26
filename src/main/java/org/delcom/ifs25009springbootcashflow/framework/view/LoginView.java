package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.AuthUseCase;

@Route("login")
@PageTitle("Masuk | Cashflow")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final transient AuthUseCase authUseCase;
    private final transient IUserRepository users;
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final ProgressBar progress = new ProgressBar();
    private final Button loginButton = new Button("Masuk");

    public LoginView(AuthUseCase authUseCase, IUserRepository users) {
        this.authUseCase = authUseCase;
        this.users = users;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("auth-page");

        progress.setIndeterminate(true);
        progress.setVisible(false);
        username.setWidthFull();
        password.setWidthFull();
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.setId("login-submit");
        loginButton.addClickShortcut(Key.ENTER);
        loginButton.addClickListener(e -> doLogin());
        nativeInputId(username, "login-username");
        nativeInputId(password, "login-password");

        Button toRegister = new Button("Belum punya akun? Daftar",
                e -> UI.getCurrent().navigate(RegisterView.class));
        toRegister.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        PageCard card = new PageCard(new H2("Masuk ke Cashflow"),
                new Paragraph("Catat pemasukan dan pengeluaran Anda."),
                username, password, progress, loginButton, toRegister);
        card.addClassName("auth-card");
        add(card);
    }

    /** Menetapkan ID pada elemen input native (bukan wrapper web component) untuk pengujian otomasi. */
    static void nativeInputId(Component field, String id) {
        field.getElement().executeJs(
                "const el = this; const apply = () => { if (el.inputElement) { el.inputElement.id = $0; }"
                        + " else { setTimeout(apply, 50); } }; apply();", id);
    }

    private void doLogin() {
        progress.setVisible(true);
        loginButton.setEnabled(false);
        try {
            User user = authUseCase.login(username.getValue(), password.getValue());
            AuthSession.login(user);
            UI.getCurrent().navigate("");
        } catch (DomainException e) {
            Notify.error(e.getMessage());
        } finally {
            progress.setVisible(false);
            loginButton.setEnabled(true);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(users) != null) {
            event.forwardTo(FinanceView.class);
        }
    }
}
