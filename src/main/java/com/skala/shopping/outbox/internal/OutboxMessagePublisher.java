package com.skala.shopping.outbox.internal;

interface OutboxMessagePublisher { void publish(String eventType, String payload); }
