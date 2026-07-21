package com.example.fan_cafe.merchandise_page.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "프로모션 식별자", example = "701")
    private Long id;
    @Schema(description = "프로모션 제목", example = "월드투어 굿즈 사전 판매")
    private String title;
    @Schema(description = "프로모션 설명", example = "공연 전 공식 굿즈를 먼저 만나보세요.")
    private String description;
    @Schema(description = "프로모션 이미지", example = "https://cdn.fancafe.kr/promotions/701/banner.jpg")
    private String imageUrl;
    @Schema(description = "시작일", example = "2026-07-21")
    private LocalDate startAt;
    @Schema(description = "종료일", example = "2026-08-10")
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
