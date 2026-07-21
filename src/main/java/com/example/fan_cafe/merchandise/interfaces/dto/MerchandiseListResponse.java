package com.example.fan_cafe.merchandise.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.fan_cafe.merchandise.domain.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MerchandiseListResponse {
    @Schema(description = "상품 카테고리", example = "FASHION")
    private Category category;
    @Schema(description = "카테고리 상품 목록", example = "[{\"id\":501,\"name\":\"2026 월드투어 공식 티셔츠\"}]")
    private List<MerchandiseResponse> merchandises;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    public static MerchandiseListResponse of(Category category, List<MerchandiseResponse> merchandises, boolean hasNext) {
        return MerchandiseListResponse.builder()
                .category(category)
                .merchandises(merchandises)
                .hasNext(hasNext)
                .build();
    }
}
