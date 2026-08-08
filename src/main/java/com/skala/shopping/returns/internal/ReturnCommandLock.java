package com.skala.shopping.returns.internal;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ReturnCommandLock {

    private final EntityManager entityManager;

    ReturnCommandLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    void acquire(UUID commandId) {
        // 아직 명령 이력 행이 없는 첫 요청도 같은 키끼리 직렬화할 수 있도록
        // PostgreSQL 트랜잭션 범위 advisory lock을 사용합니다.
        long lockKey = commandId.getMostSignificantBits() ^ commandId.getLeastSignificantBits();
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
