package com.example.fan_cafe.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record ValidateResetTokenRequest(
        @NotBlank
        @Schema(description = "비밀번호 재설정 토큰", example = "rst_20260721_a8f3c1")
        String token
) {}
