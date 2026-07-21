package com.example.fan_cafe.global.security;


import com.example.fan_cafe.global.jakson.NoTrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequest {

    @NotBlank
    @JsonDeserialize(using = NoTrimStringDeserializer.class)
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJSUzI1NiJ9.refresh.signature")
    private String refreshToken;
}
