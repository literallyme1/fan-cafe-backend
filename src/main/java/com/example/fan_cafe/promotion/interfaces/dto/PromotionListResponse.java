package com.example.fan_cafe.promotion.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PromotionListResponse {

    @Schema(description = "프로모션 목록", example = "[{\"id\":701,\"title\":\"월드투어 굿즈 사전 판매\"}]")
    private List<PromotionResponse> promotions;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    public static PromotionListResponse of(List<PromotionResponse> promotions, boolean hasNext) {
        return PromotionListResponse.builder()
                .promotions(promotions)
                .hasNext(hasNext)
                .build();
    }
}
