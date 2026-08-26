package com.example.fan_cafe.campaign.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContributionCreateRequest(
        @NotNull @DecimalMin("1000") BigDecimal amount,
        @NotBlank String paymentKey
) {
}
