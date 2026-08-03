package com.skala.shopping.wallet.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.wallet.WalletApi;
import com.skala.shopping.wallet.WalletBalance;
import com.skala.shopping.wallet.internal.domain.PointAccount;
import com.skala.shopping.wallet.internal.domain.PointTransaction;
import com.skala.shopping.wallet.internal.domain.PointTransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WalletApplicationService implements WalletApi {

    private final PointAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final Clock clock = Clock.systemUTC();

    WalletApplicationService(
            PointAccountRepository accountRepository,
            PointTransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public WalletBalance openAccount(
            UUID memberId,
            BigDecimal initialBalance,
            UUID referenceId,
            UUID commandId
    ) {
        requireNonNegative(initialBalance);
        if (transactionRepository.existsByCommandId(commandId)) {
            return getBalance(memberId);
        }
        if (accountRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATED, "이미 포인트 계정이 존재합니다.");
        }
        Instant now = clock.instant();
        PointAccount account = accountRepository.save(new PointAccount(memberId, initialBalance, now));
        saveTransaction(
                account,
                PointTransactionType.SIGN_UP,
                initialBalance,
                referenceId,
                commandId,
                now
        );
        return account.toBalance();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalance getBalance(UUID memberId) {
        return accountRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "포인트 계정을 찾을 수 없습니다."))
                .toBalance();
    }

    @Override
    @Transactional
    public WalletBalance debit(UUID memberId, BigDecimal amount, UUID referenceId, UUID commandId) {
        requirePositive(amount);
        PointAccount account = lockedAccount(memberId);
        if (transactionRepository.existsByCommandId(commandId)) {
            return account.toBalance();
        }
        Instant now = clock.instant();
        account.debit(amount, now);
        saveTransaction(account, PointTransactionType.DEBIT, amount, referenceId, commandId, now);
        return account.toBalance();
    }

    @Override
    @Transactional
    public WalletBalance credit(UUID memberId, BigDecimal amount, UUID referenceId, UUID commandId) {
        requirePositive(amount);
        PointAccount account = lockedAccount(memberId);
        if (transactionRepository.existsByCommandId(commandId)) {
            return account.toBalance();
        }
        Instant now = clock.instant();
        account.credit(amount, now);
        saveTransaction(account, PointTransactionType.REFUND, amount, referenceId, commandId, now);
        return account.toBalance();
    }

    private PointAccount lockedAccount(UUID memberId) {
        return accountRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "포인트 계정을 찾을 수 없습니다."));
    }

    private void saveTransaction(
            PointAccount account,
            PointTransactionType type,
            BigDecimal amount,
            UUID referenceId,
            UUID commandId,
            Instant now
    ) {
        transactionRepository.save(new PointTransaction(
                account.toBalance().getMemberId(),
                type,
                amount,
                account.toBalance().getBalance(),
                referenceId,
                commandId,
                now
        ));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "금액은 0보다 커야 합니다.");
        }
    }

    private void requireNonNegative(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "금액은 0 이상이어야 합니다.");
        }
    }
}
