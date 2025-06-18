package com.example.fan_cafe.user.interfaces.dto;


import lombok.Builder;

@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private UserInfoResponse userInfo;

    public static LoginResponse from(String accessToken, String refreshToken, UserInfoResponse userInfo) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .build();
    }
}
