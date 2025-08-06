package com.example.fan_cafe.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateResetTokenRequest(
        @NotBlank String token
) {}
