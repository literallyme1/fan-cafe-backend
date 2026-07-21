package com.example.fan_cafe.promotion.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PromotionRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    @Schema(description = "프로모션 제목", example = "월드투어 굿즈 사전 판매")
    private String title;

    @Schema(description = "프로모션 설명", example = "공연 전 공식 굿즈를 먼저 만나보세요.")
    private String description;

    @Schema(description = "기존 이미지 URL", example = "https://cdn.fancafe.kr/promotions/701/banner.jpg")
    private String imageUrl;

    @Schema(description = "시작일", example = "2026-07-21")
    private LocalDate startAt;

    @Schema(description = "종료일", example = "2026-08-10")
    private LocalDate endAt;

    @Schema(description = "기존 이미지 삭제 여부", example = "false")
    private boolean deleteImage;
}
