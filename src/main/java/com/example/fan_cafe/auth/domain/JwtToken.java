package com.example.fan_cafe.auth.domain;

public record JwtToken(
        String accessToken,
        String refreshToken
) {
    public static JwtToken from(String accessToken, String refreshToken) {
        return new JwtToken(accessToken, refreshToken);
    }
}