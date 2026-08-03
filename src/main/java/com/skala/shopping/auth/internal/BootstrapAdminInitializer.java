package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.internal.domain.AuthAccount;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(
        prefix = "shopping.security.bootstrap-admin",
        name = "enabled",
        havingValue = "true"
)
class BootstrapAdminInitializer implements ApplicationRunner {

    private final AuthAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;
    private final Clock clock = Clock.systemUTC();

    BootstrapAdminInitializer(
            AuthAccountRepository repository,
            PasswordEncoder passwordEncoder,
            SecurityProperties properties
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        var bootstrapAdmin = properties.getBootstrapAdmin();
        validate(bootstrapAdmin);

        var existing = repository.findByLoginId(bootstrapAdmin.getLoginId());
        if (existing.isPresent()) {
            var account = existing.get();
            if (account.isActive() && account.isAdmin()) {
                return;
            }
            throw new IllegalStateException("Bootstrap admin login ID is already in use");
        }

        repository.save(AuthAccount.createAdmin(
                UUID.randomUUID(),
                bootstrapAdmin.getLoginId(),
                passwordEncoder.encode(bootstrapAdmin.getPassword()),
                clock.instant()
        ));
    }

    private void validate(SecurityProperties.BootstrapAdmin bootstrapAdmin) {
        if (!StringUtils.hasText(bootstrapAdmin.getLoginId())) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_LOGIN_ID is required when bootstrap is enabled");
        }
        if (!StringUtils.hasText(bootstrapAdmin.getPassword())
                || bootstrapAdmin.getPassword().length() < 12) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters"
            );
        }
    }
}
