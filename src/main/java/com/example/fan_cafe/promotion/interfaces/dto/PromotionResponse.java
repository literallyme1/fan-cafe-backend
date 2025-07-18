package com.example.fan_cafe.promotion.interfaces.dto;


import com.example.fan_cafe.promotion.domain.Promotion;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Getter
@NoArgsConstructor
public class PromotionResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private LocalDate startAt;
    private LocalDate endAt;

    @QueryProjection
    public PromotionResponse(Long id, String title,
                             String description,
                             String imageUrl,
                             LocalDate startAt,
                             LocalDate endAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startAt = startAt;
        this.endAt = endAt;
    }

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
