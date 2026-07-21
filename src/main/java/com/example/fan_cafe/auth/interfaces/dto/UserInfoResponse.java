package com.example.fan_cafe.auth.interfaces.dto;

import com.example.fan_cafe.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserInfoResponse {

    @Schema(description = "회원 식별자", example = "101")
    private final Long id;
    @Schema(description = "회원 이메일", example = "fan@example.com")
    private final String email;
    @Schema(description = "회원 닉네임", example = "별빛팬")
    private final String nickname;
    @Schema(description = "회원 권한", example = "USER")
    private final String role;

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().toString())
                .build();
    }
}
