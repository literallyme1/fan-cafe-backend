package com.example.fan_cafe.auth.interfaces.dto;


import com.example.fan_cafe.auth.domain.JwtToken;
import com.example.fan_cafe.global.security.JwtTokenResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginResponse {

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJSUzI1NiJ9.access.signature")
    private final String accessToken;
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJSUzI1NiJ9.refresh.signature")
    private final String refreshToken;
    @Schema(description = "로그인 회원 정보", example = "{\"id\":101,\"email\":\"fan@example.com\",\"nickname\":\"별빛팬\",\"role\":\"USER\"}")
    private final UserInfoResponse userInfo;

    public static LoginResponse from(JwtToken jwtToken, UserInfoResponse userInfo) {
        return LoginResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .userInfo(userInfo)
                .build();
    }
}
