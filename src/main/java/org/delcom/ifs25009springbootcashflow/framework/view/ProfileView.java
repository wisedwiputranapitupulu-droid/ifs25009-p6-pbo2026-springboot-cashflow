package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profil | Cashflow")
public class ProfileView extends VerticalLayout {
    private final transient UserUseCase userUseCase;
    private final transient IUserRepository users;
    private final transient FileStorage fileStorage;
    private final transient FinancePresenter presenter;
    private final TextField fullName = new TextField("Nama lengkap");
    private final PasswordField newPassword = new PasswordField("Password baru (kosongkan jika tidak diubah)");
    private final Avatar avatar = new Avatar();
    private final ProgressBar progress = new ProgressBar();
    private final AtomicReference<byte[]> photoData = new AtomicReference<>();
    private final AtomicReference<String> photoName = new AtomicReference<>();
    private int userId;

    public ProfileView(UserUseCase userUseCase, IUserRepository users, FileStorage fileStorage,
                       FinancePresenter presenter) {
        this.userUseCase = userUseCase;
        this.users = users;
        this.fileStorage = fileStorage;
        this.presenter = presenter;
        setPadding(true);

        avatar.setHeight("96px");
        avatar.setWidth("96px");
        fullName.setWidthFull();
        newPassword.setWidthFull();
        progress.setIndeterminate(true);
        progress.setVisible(false);

        Upload upload = new Upload(UploadHandler.inMemory((meta, data) -> {
            photoName.set(meta.fileName());
            photoData.set(data);
        }));
        upload.setMaxFiles(1);
        upload.setMaxFileSize((int) FileStorage.MAX_BYTES);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        upload.setDropLabel(new Span("Foto profil - seret gambar ke sini"));
        upload.addFileRemovedListener(e -> photoData.set(null));

        Button save = new Button("Simpan perubahan");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save(save));

        PageCard card = new PageCard(new H2("Profil Saya"), avatar, fullName, newPassword, upload, progress, save);
        card.addClassName("profile-card");
        add(card);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        User user = AuthSession.currentUser(users);
        if (user != null) {
            userId = user.getId();
            fullName.setValue(user.getFullName());
            avatar.setName(user.getFullName());
            String url = presenter.fileUrl(user.getPhotoPath());
            if (url != null) {
                avatar.setImage(url);
            }
        }
    }

    private void save(Button button) {
        progress.setVisible(true);
        button.setEnabled(false);
        String newPhoto = null;
        try {
            User before = userUseCase.getUser(userId);
            String oldPhoto = before.getPhotoPath();
            userUseCase.updateProfile(userId, fullName.getValue(), newPassword.getValue());
            if (photoData.get() != null) {
                newPhoto = fileStorage.save(photoName.get(), photoData.get());
                userUseCase.updatePhoto(userId, newPhoto);
                fileStorage.delete(oldPhoto);
            }
            Notify.success("Profil diperbarui");
            UI.getCurrent().getPage().reload();
        } catch (DomainException e) {
            fileStorage.delete(newPhoto);
            Notify.error(e.getMessage());
        } finally {
            progress.setVisible(false);
            button.setEnabled(true);
        }
    }
}
