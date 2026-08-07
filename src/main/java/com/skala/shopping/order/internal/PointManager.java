package com.skala.shopping.order.internal;

import java.math.BigDecimal;
import java.util.UUID;

interface PointManager {

    BigDecimal balance(UUID memberId);

    BigDecimal debit(UUID memberId, BigDecimal amount, UUID referenceId, UUID commandId);

    BigDecimal credit(UUID memberId, BigDecimal amount, UUID referenceId, UUID commandId);
}
