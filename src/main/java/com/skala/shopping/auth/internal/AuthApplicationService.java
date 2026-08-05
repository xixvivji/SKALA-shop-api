package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.AuthAccountApi;
import com.skala.shopping.auth.BcryptPasswordPolicy;
import com.skala.shopping.auth.internal.domain.AuthAccount;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService implements AuthAccountApi {

    private static final String PASSWORD_RESET_FAILURE_MESSAGE =
            "입력한 회원 정보를 확인할 수 없습니다.";

    private final AuthAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final Clock clock = Clock.systemUTC();

    public AuthApplicationService(
            AuthAccountRepository repository,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
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

    @Transactional(readOnly = true)
    public LoginResult login(String loginId, String rawPassword) {
        AuthAccount account = repository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_AUTHENTICATED,
                        "고객 ID 또는 비밀번호가 올바르지 않습니다."
                ));
        if (!account.isActive()
                || !BcryptPasswordPolicy.isCompatible(rawPassword)
                || !passwordEncoder.matches(rawPassword, account.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.NOT_AUTHENTICATED,
                    "고객 ID 또는 비밀번호가 올바르지 않습니다."
            );
        }
        var token = tokenService.issue(account);
        return new LoginResult(
                account.id(),
                account.loginId(),
                account.role().name(),
                token.getValue(),
                token.getExpiresAt()
        );
    }

    private void validatePasswordForEncoding(String rawPassword) {
        if (!BcryptPasswordPolicy.isCompatible(rawPassword)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    BcryptPasswordPolicy.VALIDATION_MESSAGE
            );
        }
    }
}
