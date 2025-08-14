package com.example.fan_cafe.auth.interfaces.dto;

import com.example.fan_cafe.global.jakson.NoTrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordConfirmRequest (
    @NotBlank
    String token,

    @JsonDeserialize(using = NoTrimStringDeserializer.class)
    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$",
            message = "비밀번호는 문자와 숫자를 포함한 6자 이상이어야 합니다."
    )
    String newPassword
){}
