package org.delcom.ifs25009springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;

/** Dialog modal: header biru primer (#2563EB), footer abu-abu (#F3F4F6) dengan tombol di kanan. */
public class AppDialog extends Dialog {
    public AppDialog(String title, String badge) {
        addClassName("app-dialog");
        setCloseOnOutsideClick(false);
        setDraggable(false);
        setResizable(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName("app-dialog-title");
        getHeader().add(titleSpan);
        if (badge != null) {
            Span b = new Span(badge);
            b.addClassName("app-dialog-badge");
            getHeader().add(b);
        }
    }

    public void addActions(Button... buttons) {
        getFooter().add(buttons);
    }
}
