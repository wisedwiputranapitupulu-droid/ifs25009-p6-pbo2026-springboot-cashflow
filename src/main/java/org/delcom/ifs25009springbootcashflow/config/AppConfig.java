package org.delcom.ifs25009springbootcashflow.config;

import org.delcom.ifs25009springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25009springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25009springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25009springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25009springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25009springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25009springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25009springbootcashflow.usecase.UserUseCase;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Composition root: seluruh wiring bean ada di sini. */
@Configuration
public class AppConfig {

    @Bean
    public IUserRepository userRepository(DataSource dataSource) {
        SchemaInitializer.initialize(dataSource);
        return new JdbcUserRepository(dataSource);
    }

    @Bean
    public ITransactionRepository transactionRepository(DataSource dataSource) {
        return new JdbcTransactionRepository(dataSource);
    }

    @Bean
    public IFinanceAiService financeAiService(
            @Value("${OPENAI_ENDPOINT:https://delcom.org/api/v1}") String endpoint,
            @Value("${OPENAI_KEY:key}") String key,
            @Value("${OPENAI_MODEL:nama-model}") String model) {
        return new OpenAiFinanceService(endpoint, key, model);
    }

    @Bean
    public AuthUseCase authUseCase(IUserRepository users) {
        return new AuthUseCase(users);
    }

    @Bean
    public UserUseCase userUseCase(IUserRepository users) {
        return new UserUseCase(users);
    }

    @Bean
    public FinanceUseCase financeUseCase(ITransactionRepository transactions) {
        return new FinanceUseCase(transactions);
    }

    @Bean
    public FinanceAiUseCase financeAiUseCase(IFinanceAiService ai, ITransactionRepository transactions) {
        return new FinanceAiUseCase(ai, transactions);
    }

    @Bean
    public FinancePresenter financePresenter() {
        return new FinancePresenter();
    }

    @Bean
    public FileStorage fileStorage(@Value("${app.upload-dir}") String uploadDir) {
        return new FileStorage(uploadDir);
    }

    @Bean
    public ApplicationRunner seedRootAdmin(
            AuthUseCase authUseCase,
            @Value("${app.root-username}") String rootUsername,
            @Value("${app.root-password}") String rootPassword) {
        return args -> authUseCase.ensureRootAdmin(rootUsername, rootPassword);
    }

    @Bean
    public WebMvcConfigurer uploadsMapping(FileStorage fileStorage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(fileStorage.root().toUri().toString());
            }
        };
    }
}
