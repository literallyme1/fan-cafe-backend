package com.example.fan_cafe.merchandise_page.interfaces.dto;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
@Getter
public class PromotionSummaryDto {

    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private LocalDate startAt;
    private LocalDate endAt;

    public static PromotionSummaryDto from(Promotion promotion) {
        return PromotionSummaryDto.builder()
                .id(promotion.getId())
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .imageUrl(promotion.getImageUrl())
                .startAt(promotion.getStartAt())
                .endAt(promotion.getEndAt())
                .build();
    }
}
