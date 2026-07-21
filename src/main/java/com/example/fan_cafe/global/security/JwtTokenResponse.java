package com.example.fan_cafe.global.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class JwtTokenResponse {

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJSUzI1NiJ9.access.signature")
    private final String accessToken;
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJSUzI1NiJ9.refresh.signature")
    private final String refreshToken;

    public static JwtTokenResponse from(String accessToken) {
        return JwtTokenResponse.builder()
                .accessToken(accessToken)
                .build();

    }
    public static JwtTokenResponse from(String accessToken, String refreshToken) {
        return JwtTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }
}
