package com.example.fan_cafe.auth.interfaces.dto;


import com.example.fan_cafe.auth.domain.JwtToken;
import com.example.fan_cafe.global.security.JwtTokenResponse;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final UserInfoResponse userInfo;

    public static LoginResponse from(JwtToken jwtToken, UserInfoResponse userInfo) {
        return LoginResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .userInfo(userInfo)
                .build();
    }
}
