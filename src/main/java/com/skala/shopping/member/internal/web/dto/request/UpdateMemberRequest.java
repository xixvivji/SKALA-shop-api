package com.skala.shopping.member.internal.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public final class UpdateMemberRequest {

    @NotBlank
    private String name;

    public UpdateMemberRequest() {
    }

    public UpdateMemberRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
