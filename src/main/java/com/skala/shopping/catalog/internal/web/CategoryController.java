package com.skala.shopping.catalog.internal.web;

import com.skala.shopping.catalog.CategoryView;
import com.skala.shopping.catalog.internal.CategoryApplicationService;
import com.skala.shopping.common.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "카테고리", description = "상품 카테고리 조회와 관리자 관리")
class CategoryController {

    private final CategoryApplicationService service;

    CategoryController(CategoryApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "카테고리 목록 조회", description = "상품 분류에 사용하는 전체 카테고리를 조회합니다.")
    List<CategoryView> list() {
        return service.getCategories();
    }

    @PostMapping
    @Operation(summary = "카테고리 등록", description = "관리자가 새로운 상품 카테고리를 등록합니다.", security = @SecurityRequirement(name = "cookieAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록된 카테고리"),
            @ApiResponse(responseCode = "400", description = "입력값 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "카테고리명 중복",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CategoryView create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request.getName(), request.getDescription());
    }

    @PutMapping("/{id}")
    @Operation(summary = "카테고리 수정", description = "관리자가 기존 카테고리 정보를 수정합니다.", security = @SecurityRequirement(name = "cookieAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 카테고리"),
            @ApiResponse(responseCode = "400", description = "카테고리 ID 또는 입력값 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "카테고리명 중복",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CategoryView update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return service.update(id, request.getName(), request.getDescription());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "카테고리 삭제", description = "관리자가 상품에 사용되지 않는 카테고리를 삭제합니다.", security = @SecurityRequirement(name = "cookieAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 삭제 완료"),
            @ApiResponse(responseCode = "400", description = "카테고리 ID 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @Schema(name = "CategoryRequest", description = "카테고리 생성·수정 요청")
    public static final class CategoryRequest {

        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        public CategoryRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
