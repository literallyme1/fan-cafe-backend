package com.example.fan_cafe.promotion.interfaces.dto;

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
    private String title;

    private String description;

    private String imageUrl;

    private LocalDate startAt;

    private LocalDate endAt;
}
