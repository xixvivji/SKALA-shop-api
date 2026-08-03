package com.skala.shopping.member;

import java.util.UUID;

public final class MemberResponse {

    private final UUID id;
    private final String customerId;
    private final String name;
    private final String status;

    public MemberResponse(UUID id, String customerId, String name, String status) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
