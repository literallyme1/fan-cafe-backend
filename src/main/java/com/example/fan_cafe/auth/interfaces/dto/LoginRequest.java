package com.example.fan_cafe.auth.interfaces.dto;

import com.example.fan_cafe.global.jakson.NoTrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "로그인 이메일", example = "fan@example.com")
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
    @JsonDeserialize(using = NoTrimStringDeserializer.class)
    @Schema(description = "로그인 비밀번호", example = "Fan2026!")
    private String password;

    @Schema(description = "로그인 유지 여부", example = "true")
    private boolean rememberMe;
}
