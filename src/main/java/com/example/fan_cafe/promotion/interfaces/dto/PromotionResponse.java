package com.example.fan_cafe.promotion.interfaces.dto;


import com.example.fan_cafe.promotion.domain.Promotion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
@Getter
public class PromotionResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private LocalDate startAt;
    private LocalDate endAt;

    public static PromotionResponse from(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .imageUrl(promotion.getImageUrl())
                .startAt(promotion.getStartAt())
                .endAt(promotion.getEndAt())
                .build();
    }
}
