package com.skala.shopping.auth.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.skala.shopping.common.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryAuthenticationRateLimiterTests {

    private MutableClock clock;
    private InMemoryAuthenticationRateLimiter limiter;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getRateLimit().setLogin(limit(2, 2));
        properties.getRateLimit().setRegistration(limit(2, 2));
        properties.getRateLimit().setPasswordReset(limit(2, 2));
        clock = new MutableClock(Instant.parse("2026-08-05T00:00:00Z"));
        limiter = new InMemoryAuthenticationRateLimiter(properties, clock);
    }

    @Test
    void limitsOneIpEvenWhenAccountIdsAreDifferent() {
        limiter.checkLogin("203.0.113.10", "customer-a");
        limiter.checkLogin("203.0.113.10", "customer-b");

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkLogin("203.0.113.10", "customer-c")
        );

        assertEquals(60, exception.retryAfterSeconds());
    }

    @Test
    void limitsOneAccountAcrossDifferentIpsAndNormalizesCase() {
        limiter.checkPasswordReset("203.0.113.11", "SkalaUser");
        limiter.checkPasswordReset("203.0.113.12", "skalauser");

        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkPasswordReset("203.0.113.13", " SKALAUSER ")
        );
    }

    @Test
    void keepsLimitsIndependentForEachAuthenticationOperation() {
        limiter.checkLogin("203.0.113.20", "same-user");
        limiter.checkLogin("203.0.113.20", "same-user");

        assertDoesNotThrow(() -> limiter.checkRegistration("203.0.113.20", "same-user"));
        assertDoesNotThrow(() -> limiter.checkPasswordReset("203.0.113.20", "same-user"));
    }

    @Test
    void defaultsToFiftyThousandTrackedKeys() {
        assertEquals(
                50_000,
                new SecurityProperties().getRateLimit().getMaxTrackedKeys()
        );
    }

    @Test
    void allowsRequestsAgainAfterWindowExpires() {
        limiter.checkRegistration("203.0.113.30", "customer-a");
        limiter.checkRegistration("203.0.113.30", "customer-b");
        assertThrows(
                RateLimitExceededException.class,
                () -> limiter.checkRegistration("203.0.113.30", "customer-c")
        );

        clock.advance(Duration.ofSeconds(60));

        assertDoesNotThrow(() -> limiter.checkRegistration("203.0.113.30", "customer-c"));
    }

    @Test
    void failsClosedWhenTrackedKeyCapacityIsFull() {
        SecurityProperties properties = properties(2, 100);
        InMemoryAuthenticationRateLimiter capacityLimited =
                new InMemoryAuthenticationRateLimiter(properties, clock);
        capacityLimited.checkLogin("203.0.113.40", "customer-a");

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> capacityLimited.checkLogin("203.0.113.41", "customer-b")
        );

        assertEquals(60, exception.retryAfterSeconds());
        assertEquals(2, capacityLimited.trackedKeyCount());
    }

    @Test
    void admitsNewKeysAfterFullCapacityWindowExpires() {
        SecurityProperties properties = properties(2, 100);
        InMemoryAuthenticationRateLimiter capacityLimited =
                new InMemoryAuthenticationRateLimiter(properties, clock);
        capacityLimited.checkLogin("203.0.113.40", "customer-a");
        clock.advance(Duration.ofSeconds(60));

        assertDoesNotThrow(
                () -> capacityLimited.checkLogin("203.0.113.41", "customer-b")
        );
        assertEquals(2, capacityLimited.trackedKeyCount());
    }

    @Test
    void neverExceedsTrackedKeyCapacityUnderConcurrentUniqueRequests() throws Exception {
        int maximumTrackedKeys = 8;
        int attempts = 24;
        SecurityProperties properties = properties(maximumTrackedKeys, 100);
        InMemoryAuthenticationRateLimiter capacityLimited =
                new InMemoryAuthenticationRateLimiter(properties, clock);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < attempts; index++) {
                int requestIndex = index;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        capacityLimited.checkRegistration(
                                "203.0.113." + requestIndex,
                                "customer-" + requestIndex
                        );
                        return true;
                    } catch (RateLimitExceededException exception) {
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            long rejected = 0;
            for (Future<Boolean> result : results) {
                if (!result.get(5, TimeUnit.SECONDS)) {
                    rejected++;
                }
            }
            assertTrue(rejected > 0);
            assertEquals(maximumTrackedKeys, capacityLimited.trackedKeyCount());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void enforcesRequestLimitAtomicallyForConcurrentSameKey() throws Exception {
        int maximumRequests = 5;
        int attempts = 20;
        SecurityProperties properties = properties(100, maximumRequests);
        InMemoryAuthenticationRateLimiter concurrentLimiter =
                new InMemoryAuthenticationRateLimiter(properties, clock);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < attempts; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        concurrentLimiter.checkLogin("203.0.113.50", "same-customer");
                        return true;
                    } catch (RateLimitExceededException exception) {
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(maximumRequests, accepted);
            assertEquals(2, concurrentLimiter.trackedKeyCount());
        } finally {
            executor.shutdownNow();
        }
    }

    private SecurityProperties properties(int maximumTrackedKeys, int maximumRequests) {
        SecurityProperties properties = new SecurityProperties();
        properties.getRateLimit().setMaxTrackedKeys(maximumTrackedKeys);
        properties.getRateLimit().setLogin(limit(maximumRequests, maximumRequests));
        properties.getRateLimit().setRegistration(limit(maximumRequests, maximumRequests));
        properties.getRateLimit().setPasswordReset(limit(maximumRequests, maximumRequests));
        return properties;
    }

    private SecurityProperties.EndpointLimit limit(int ipMaximum, int accountMaximum) {
        return new SecurityProperties.EndpointLimit(
                ipMaximum,
                accountMaximum,
                Duration.ofMinutes(1)
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
