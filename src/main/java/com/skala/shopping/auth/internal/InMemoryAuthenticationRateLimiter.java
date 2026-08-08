package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.AuthenticationRateLimitApi;
import com.skala.shopping.common.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "shopping.security.rate-limit.store", havingValue = "memory", matchIfMissing = true)
class InMemoryAuthenticationRateLimiter implements AuthenticationRateLimitApi {

    private static final long CLEANUP_INTERVAL = 256;
    private static final int MAX_KEY_LENGTH = 128;

    private final SecurityProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Semaphore availableTrackedKeys;
    private final AtomicLong requestSequence = new AtomicLong();
    private final AtomicReference<Instant> nextCapacityCleanup =
            new AtomicReference<>(Instant.MIN);
    private final Object cleanupMonitor = new Object();

    @Autowired
    InMemoryAuthenticationRateLimiter(SecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InMemoryAuthenticationRateLimiter(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.availableTrackedKeys = new Semaphore(
                properties.getRateLimit().getMaxTrackedKeys()
        );
    }

    @Override
    public void checkLogin(String clientAddress, String loginId) {
        check("login", clientAddress, loginId, properties.getRateLimit().getLogin());
    }

    @Override
    public void checkRegistration(String clientAddress, String loginId) {
        check("registration", clientAddress, loginId, properties.getRateLimit().getRegistration());
    }

    @Override
    public void checkPasswordReset(String clientAddress, String loginId) {
        check("password-reset", clientAddress, loginId, properties.getRateLimit().getPasswordReset());
    }

    private void check(
            String operation,
            String clientAddress,
            String loginId,
            SecurityProperties.EndpointLimit limit
    ) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }

        Instant now = clock.instant();
        cleanupExpiredCounters(now);
        consume(
                operation + ":ip:" + normalize(clientAddress, false),
                limit.getMaxRequestsPerIp(),
                limit.getWindow(),
                now
        );
        consume(
                operation + ":account:" + normalize(loginId, true),
                limit.getMaxRequestsPerAccount(),
                limit.getWindow(),
                now
        );
    }

    private void consume(String key, int maximumRequests, Duration window, Instant now) {
        cleanupExpiredCountersAtCapacity(key, now);
        Instant expiresAt = now.plus(window);
        AtomicReference<Instant> blockedUntil = new AtomicReference<>();
        AtomicBoolean capacityExceeded = new AtomicBoolean();
        counters.compute(key, (ignored, current) -> {
            if (current == null) {
                if (!availableTrackedKeys.tryAcquire()) {
                    capacityExceeded.set(true);
                    return null;
                }
                return new WindowCounter(1, expiresAt);
            }
            if (!now.isBefore(current.expiresAt)) {
                return new WindowCounter(1, expiresAt);
            }
            if (current.requests >= maximumRequests) {
                blockedUntil.set(current.expiresAt);
                return current;
            }
            return new WindowCounter(current.requests + 1, current.expiresAt);
        });

        if (capacityExceeded.get()) {
            throw new RateLimitExceededException(
                    retryAfterSeconds(now, expiresAt)
            );
        }

        Instant retryAt = blockedUntil.get();
        if (retryAt != null) {
            throw new RateLimitExceededException(retryAfterSeconds(now, retryAt));
        }
    }

    private long retryAfterSeconds(Instant now, Instant retryAt) {
        long remainingMillis = Math.max(1, Duration.between(now, retryAt).toMillis());
        return Math.max(1, (remainingMillis + 999) / 1_000);
    }

    private void cleanupExpiredCounters(Instant now) {
        if (requestSequence.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        removeExpiredCounters(now);
    }

    private void cleanupExpiredCountersAtCapacity(String key, Instant now) {
        if (counters.containsKey(key) || availableTrackedKeys.availablePermits() > 0) {
            return;
        }
        synchronized (cleanupMonitor) {
            if (counters.containsKey(key) || availableTrackedKeys.availablePermits() > 0) {
                return;
            }
            if (!now.isBefore(nextCapacityCleanup.get())) {
                removeExpiredCounters(now);
                nextCapacityCleanup.set(now.plusSeconds(1));
            }
        }
    }

    private void removeExpiredCounters(Instant now) {
        counters.forEach((key, counter) -> {
            if (!now.isBefore(counter.expiresAt) && counters.remove(key, counter)) {
                availableTrackedKeys.release();
            }
        });
    }

    private String normalize(String value, boolean caseInsensitive) {
        String normalized = StringUtils.hasText(value) ? value.trim() : "unknown";
        if (caseInsensitive) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized.substring(0, Math.min(normalized.length(), MAX_KEY_LENGTH));
    }

    int trackedKeyCount() {
        return counters.size();
    }

    private static final class WindowCounter {

        private final int requests;
        private final Instant expiresAt;

        private WindowCounter(int requests, Instant expiresAt) {
            this.requests = requests;
            this.expiresAt = expiresAt;
        }
    }
}
