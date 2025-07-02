package com.example.fan_cafe.global.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class JwtTokenResponse {

    private final String accessToken;
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
