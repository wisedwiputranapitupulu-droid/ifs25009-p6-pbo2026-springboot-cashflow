package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.config.AppConfig;
import org.delcom.ifs25009springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.usecase.AuthUseCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Menguji composition root (AppConfig) dengan H2, tanpa menjalankan server Vaadin. */
class AppTest {
    @TempDir
    Path dir;

    @Test
    void compositionRootWiresEverything() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        AppConfig config = new AppConfig();

        IUserRepository users = config.userRepository(ds);
        ITransactionRepository transactions = config.transactionRepository(ds);
        IFinanceAiService ai = config.financeAiService("http://localhost/v1", "key", "model");
        AuthUseCase auth = config.authUseCase(users);
        assertNotNull(config.userUseCase(users));
        assertNotNull(config.financeUseCase(transactions));
        assertNotNull(config.financeAiUseCase(ai, transactions));
        assertNotNull(config.financePresenter());

        ApplicationRunner seed = config.seedRootAdmin(auth, "root", "rootpass");
        seed.run(null);
        seed.run(null);
        assertTrue(users.findByUsername("root").orElseThrow().isAdmin());
        assertEquals(1, users.findAll().size());

        FileStorage storage = config.fileStorage(dir.toString());
        WebMvcConfigurer mvc = config.uploadsMapping(storage);
        mvc.addResourceHandlers(new ResourceHandlerRegistry(new StaticApplicationContext(), new MockServletContext()));
    }
}
