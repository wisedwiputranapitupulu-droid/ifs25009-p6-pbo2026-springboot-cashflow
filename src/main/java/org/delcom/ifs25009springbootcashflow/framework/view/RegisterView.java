package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.AuthUseCase;

@Route("register")
@PageTitle("Daftar | Cashflow")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {
    private final transient AuthUseCase authUseCase;
    private final transient IUserRepository users;
    private final TextField fullName = new TextField("Nama lengkap");
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password (min. 6 karakter)");
    private final ProgressBar progress = new ProgressBar();
    private final Button registerButton = new Button("Daftar");

    public RegisterView(AuthUseCase authUseCase, IUserRepository users) {
        this.authUseCase = authUseCase;
        this.users = users;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("auth-page");

        progress.setIndeterminate(true);
        progress.setVisible(false);
        fullName.setWidthFull();
        username.setWidthFull();
        password.setWidthFull();
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.addClickShortcut(Key.ENTER);
        registerButton.addClickListener(e -> doRegister());
        Button toLogin = new Button("Sudah punya akun? Masuk", e -> UI.getCurrent().navigate(LoginView.class));
        toLogin.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        PageCard card = new PageCard(new H2("Daftar Akun Baru"), fullName, username, password,
                progress, registerButton, toLogin);
        card.addClassName("auth-card");
        add(card);
    }

    private void doRegister() {
        progress.setVisible(true);
        registerButton.setEnabled(false);
        try {
            authUseCase.register(username.getValue(), password.getValue(), fullName.getValue());
            Notify.success("Registrasi berhasil, silakan masuk");
            UI.getCurrent().navigate(LoginView.class);
        } catch (DomainException e) {
            Notify.error(e.getMessage());
        } finally {
            progress.setVisible(false);
            registerButton.setEnabled(true);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(users) != null) {
            event.forwardTo(FinanceView.class);
        }
    }
}
