package com.skala.shopping.member.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateMemberRequest", description = "내 회원 이름 변경 요청")
public final class UpdateMemberRequest {

    @Schema(description = "변경할 이름", example = "김스칼라")
    @NotBlank
    @Size(max = 100)
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
