package com.example.fan_cafe.campaign.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull @DecimalMin("1000") BigDecimal targetAmount,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime deadlineAt
) {
}
