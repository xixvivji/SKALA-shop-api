package com.skala.shopping.auth.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.auth.internal.domain.AuthAccount;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthApplicationServiceTests {

    private AuthAccountRepository repository;
    private PasswordEncoder passwordEncoder;
    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuthAccountRepository.class);
        passwordEncoder = spy(new BCryptPasswordEncoder(4));
        service = new AuthApplicationService(
                repository,
                passwordEncoder,
                mock(JwtTokenService.class)
        );
    }

    @Test
    void comparesAgainstAValidDummyHashWhenLoginIdDoesNotExist() {
        when(repository.findByLoginId("unknown-user")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.login("unknown-user", "wrong-password")
        );

        assertEquals(ErrorCode.NOT_AUTHENTICATED, exception.errorCode());
        verify(passwordEncoder).matches(
                "wrong-password",
                AuthApplicationService.DUMMY_PASSWORD_HASH
        );
        assertFalse(new BCryptPasswordEncoder().matches(
                "wrong-password",
                AuthApplicationService.DUMMY_PASSWORD_HASH
        ));
    }

    @Test
    void stillPerformsBcryptComparisonForAnInactiveAccount() {
        String passwordHash = passwordEncoder.encode("original-password");
        AuthAccount account = new AuthAccount(
                UUID.randomUUID(),
                "inactive-user",
                passwordHash,
                Instant.parse("2026-08-05T00:00:00Z")
        );
        account.deactivate(Instant.parse("2026-08-05T00:01:00Z"));
        when(repository.findByLoginId("inactive-user")).thenReturn(Optional.of(account));

        assertThrows(
                BusinessException.class,
                () -> service.login("inactive-user", "original-password")
        );

        verify(passwordEncoder).matches("original-password", passwordHash);
    }

    @Test
    void changesAdminPasswordOnlyAfterCurrentPasswordMatches() {
        String oldPassword = "old-admin-password";
        String newPassword = "new-admin-password";
        String oldHash = passwordEncoder.encode(oldPassword);
        UUID adminId = UUID.randomUUID();
        AuthAccount admin = AuthAccount.createAdmin(
                adminId,
                "admin",
                oldHash,
                Instant.parse("2026-08-05T00:00:00Z")
        );
        when(repository.findByIdForPasswordChange(adminId)).thenReturn(Optional.of(admin));

        service.changeAdminPassword(adminId, oldPassword, newPassword);

        assertNotEquals(oldHash, admin.passwordHash());
        assertTrue(passwordEncoder.matches(newPassword, admin.passwordHash()));
        assertFalse(passwordEncoder.matches(oldPassword, admin.passwordHash()));
    }

    @Test
    void leavesAdminPasswordUnchangedWhenCurrentPasswordIsWrong() {
        String oldHash = passwordEncoder.encode("old-admin-password");
        UUID adminId = UUID.randomUUID();
        AuthAccount admin = AuthAccount.createAdmin(
                adminId,
                "admin",
                oldHash,
                Instant.parse("2026-08-05T00:00:00Z")
        );
        when(repository.findByIdForPasswordChange(adminId)).thenReturn(Optional.of(admin));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.changeAdminPassword(
                        adminId,
                        "wrong-admin-password",
                        "new-admin-password"
                )
        );

        assertEquals(ErrorCode.NOT_AUTHENTICATED, exception.errorCode());
        assertEquals(oldHash, admin.passwordHash());
    }
}
