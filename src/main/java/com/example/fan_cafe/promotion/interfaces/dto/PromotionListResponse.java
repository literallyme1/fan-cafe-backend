package com.example.fan_cafe.promotion.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PromotionListResponse {

    private List<PromotionResponse> promotions;
    private boolean hasNext;

    public static PromotionListResponse of(List<PromotionResponse> promotions, boolean hasNext) {
        return PromotionListResponse.builder()
                .promotions(promotions)
                .hasNext(hasNext)
                .build();
    }
}
