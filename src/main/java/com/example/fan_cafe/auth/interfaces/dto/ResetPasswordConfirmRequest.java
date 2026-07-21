package com.example.fan_cafe.auth.interfaces.dto;

import com.example.fan_cafe.global.jakson.NoTrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ResetPasswordConfirmRequest (
    @NotBlank
    @Schema(description = "비밀번호 재설정 토큰", example = "rst_20260721_a8f3c1")
    String token,

    @JsonDeserialize(using = NoTrimStringDeserializer.class)
    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$",
            message = "비밀번호는 문자와 숫자를 포함한 6자 이상이어야 합니다."
    )
    @Schema(description = "새 비밀번호", example = "NewFan2026!")
    String newPassword
){}
