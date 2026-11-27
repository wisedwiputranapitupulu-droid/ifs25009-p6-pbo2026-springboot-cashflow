package org.delcom.ifs25009springbootcashflow;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
@Meta(name = "description", content = "Cashflow membantu Anda mencatat dan mengelola pemasukan serta pengeluaran.")
@PWA(name = "Cashflow", shortName = "Cashflow", iconPath = "icons/icon.png")
public class App implements AppShellConfigurator, UIInitListener {
    public static void main(String[] args) throws Exception {
        DotEnv.load();
        SpringApplication.run(App.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.setPageTitle("Cashflow");
    }

    @Override
    public void uiInit(UIInitEvent event) {
        event.getUI().getPage().executeJs(
                "document.documentElement.lang = 'id';"
                        + "document.querySelectorAll('vaadin-connection-indicator').forEach((indicator) => {"
                        + "indicator.setAttribute('role', 'status');"
                        + "indicator.setAttribute('aria-label', 'Status koneksi aplikasi');"
                        + "});");
    }

}
