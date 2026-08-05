package com.skala.shopping.inventory.internal;

import com.skala.shopping.inventory.internal.domain.Stock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockRepository extends JpaRepository<Stock, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO inventory.stocks (
                product_id,
                available_quantity,
                status,
                version,
                created_at,
                updated_at
            ) VALUES (
                :productId,
                :availableQuantity,
                'ACTIVE',
                0,
                :now,
                :now
            )
            ON CONFLICT (product_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("productId") UUID productId,
            @Param("availableQuantity") int availableQuantity,
            @Param("now") java.time.Instant now
    );

    @Modifying
    @Query(value = """
            INSERT INTO inventory.stocks AS stock (
                product_id,
                available_quantity,
                status,
                version,
                created_at,
                updated_at
            ) VALUES (
                :productId,
                0,
                'INACTIVE',
                0,
                :now,
                :now
            )
            ON CONFLICT (product_id) DO UPDATE SET
                status = 'INACTIVE',
                version = stock.version + 1,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void deactivateOrInsert(
            @Param("productId") UUID productId,
            @Param("now") java.time.Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from Stock stock where stock.productId = :productId")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") UUID productId);
}
