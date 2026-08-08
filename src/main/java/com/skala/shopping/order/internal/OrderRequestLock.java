package com.skala.shopping.order.internal;

import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Serializes the same member/idempotency command across all PostgreSQL-backed app instances. */
@Component
class OrderRequestLock {

    private final EntityManager entityManager;

    OrderRequestLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    void acquire(UUID memberId, UUID commandId) {
        long key = lockKey(memberId, commandId);
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", key)
                .getSingleResult();
    }

    private long lockKey(UUID memberId, UUID commandId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((memberId + ":" + commandId).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
