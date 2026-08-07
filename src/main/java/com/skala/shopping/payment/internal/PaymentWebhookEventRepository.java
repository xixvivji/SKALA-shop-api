package com.skala.shopping.payment.internal;

import com.skala.shopping.payment.internal.domain.PaymentWebhookEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> { }
