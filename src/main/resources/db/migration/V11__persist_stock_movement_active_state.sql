ALTER TABLE inventory.stock_movements
    ADD COLUMN active_after BOOLEAN;

-- 초기화·예약·조정은 활성 재고에서만 성공할 수 있었다. RELEASE의 기존 데이터는
-- 별도 이력이 없으므로 마이그레이션 시점의 재고 상태를 가장 가까운 값으로 사용한다.
UPDATE inventory.stock_movements AS movement
SET active_after = CASE
    WHEN movement.movement_type IN ('INITIALIZE', 'RESERVE', 'ADJUST_IN', 'ADJUST_OUT')
        THEN TRUE
    ELSE stock.status = 'ACTIVE'
END
FROM inventory.stocks AS stock
WHERE stock.product_id = movement.product_id;

ALTER TABLE inventory.stock_movements
    ALTER COLUMN active_after SET NOT NULL;
