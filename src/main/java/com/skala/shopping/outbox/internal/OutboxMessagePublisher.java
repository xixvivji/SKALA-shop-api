package com.skala.shopping.outbox.internal;

interface OutboxMessagePublisher { void publish(String key, String eventType, String payload); }
