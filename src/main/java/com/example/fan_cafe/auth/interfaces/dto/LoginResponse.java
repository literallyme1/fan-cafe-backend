package com.example.fan_cafe.auth.interfaces.dto;


import com.example.fan_cafe.global.security.JwtTokenResponse;
import lombok.Builder;

@Builder
public class LoginResponse {

    private final JwtTokenResponse jwtToken;
    private final UserInfoResponse userInfo;

    public static LoginResponse from(JwtTokenResponse jwtToken, UserInfoResponse userInfo) {
        return LoginResponse.builder()
                .jwtToken(jwtToken)
                .userInfo(userInfo)
                .build();
    }
}
