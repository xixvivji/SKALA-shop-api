package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.AuthAccountApi;
import com.skala.shopping.auth.BcryptPasswordPolicy;
import com.skala.shopping.auth.internal.domain.AuthAccount;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService implements AuthAccountApi {

    static final String DUMMY_PASSWORD_HASH =
            "$2a$10$SmZa6n8k9HIKyr12FWoNYu7ajNYskdEwr78a/kkFYRoH.v8kv2U/G";
    private static final int MINIMUM_ADMIN_PASSWORD_LENGTH = 12;
    private static final String LOGIN_FAILURE_MESSAGE =
            "고객 ID 또는 비밀번호가 올바르지 않습니다.";
    private static final String PASSWORD_RESET_FAILURE_MESSAGE =
            "입력한 회원 정보를 확인할 수 없습니다.";

    private final AuthAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final RefreshSessionStore refreshSessionStore;
    private final SecurityProperties properties;
    private final Clock clock = Clock.systemUTC();

    public AuthApplicationService(
            AuthAccountRepository repository,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService, RefreshSessionStore refreshSessionStore, SecurityProperties properties
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshSessionStore = refreshSessionStore;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void createAccount(UUID memberId, String loginId, String rawPassword) {
        validatePasswordForEncoding(rawPassword);
        if (repository.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATED, "이미 사용 중인 고객 ID입니다.");
        }
        repository.save(new AuthAccount(
                memberId,
                loginId,
                passwordEncoder.encode(rawPassword),
                clock.instant()
        ));
    }

    @Override
    @Transactional
    public void resetPassword(UUID memberId, String rawPassword) {
        validatePasswordForEncoding(rawPassword);
        AuthAccount account = repository.findById(memberId)
                .filter(AuthAccount::isActive)
                .filter(candidate -> !candidate.isAdmin())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        PASSWORD_RESET_FAILURE_MESSAGE
                ));
        account.changePassword(passwordEncoder.encode(rawPassword), clock.instant());
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID memberId) {
        AuthAccount account = repository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
        account.deactivate(clock.instant());
    }

    @Transactional
    public void changeAdminPassword(
            UUID adminId,
            String currentRawPassword,
            String newRawPassword
    ) {
        validatePasswordForEncoding(currentRawPassword);
        validateAdminPassword(newRawPassword);

        AuthAccount account = repository.findByIdForPasswordChange(adminId)
                .filter(AuthAccount::isActive)
                .filter(AuthAccount::isAdmin)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_AUTHENTICATED));

        if (!passwordEncoder.matches(currentRawPassword, account.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.NOT_AUTHENTICATED,
                    "현재 비밀번호가 올바르지 않습니다."
            );
        }
        if (passwordEncoder.matches(newRawPassword, account.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다."
            );
        }
        account.changePassword(passwordEncoder.encode(newRawPassword), clock.instant());
    }

    @Transactional(readOnly = true)
    public LoginResult login(String loginId, String rawPassword) {
        Optional<AuthAccount> candidate = repository.findByLoginId(loginId);
        String passwordHash = candidate
                .map(AuthAccount::passwordHash)
                .orElse(DUMMY_PASSWORD_HASH);

        boolean passwordMatches = BcryptPasswordPolicy.isCompatible(rawPassword)
                && passwordEncoder.matches(rawPassword, passwordHash);
        if (candidate.isEmpty() || !candidate.get().isActive() || !passwordMatches) {
            throw loginFailure();
        }
        AuthAccount account = candidate.get();
        var token = tokenService.issue(account);
        String refreshToken = refreshSessionStore.issue(account.id(), account.credentialVersion(),
                properties.getJwt().getRefreshTokenTtl());
        return new LoginResult(
                account.id(),
                account.loginId(),
                account.role().name(),
                token.getValue(), refreshToken,
                token.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public LoginResult refresh(String refreshToken) {
        RefreshSessionStore.RefreshPrincipal principal = refreshSessionStore.consume(refreshToken);
        AuthAccount account = repository.findById(principal.memberId()).filter(AuthAccount::isActive)
                .filter(candidate -> candidate.credentialVersion() == principal.credentialVersion())
                .orElseThrow(this::loginFailure);
        var access = tokenService.issue(account);
        String rotated = refreshSessionStore.issue(account.id(), account.credentialVersion(),
                properties.getJwt().getRefreshTokenTtl());
        return new LoginResult(account.id(), account.loginId(), account.role().name(),
                access.getValue(), rotated, access.getExpiresAt());
    }

    public void revokeRefreshToken(String token) { refreshSessionStore.revoke(token); }

    private void validatePasswordForEncoding(String rawPassword) {
        if (!BcryptPasswordPolicy.isCompatible(rawPassword)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    BcryptPasswordPolicy.VALIDATION_MESSAGE
            );
        }
    }

    private void validateAdminPassword(String rawPassword) {
        validatePasswordForEncoding(rawPassword);
        if (rawPassword.length() < MINIMUM_ADMIN_PASSWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "관리자 비밀번호는 12자 이상이어야 합니다."
            );
        }
    }

    private BusinessException loginFailure() {
        return new BusinessException(ErrorCode.NOT_AUTHENTICATED, LOGIN_FAILURE_MESSAGE);
    }
}
