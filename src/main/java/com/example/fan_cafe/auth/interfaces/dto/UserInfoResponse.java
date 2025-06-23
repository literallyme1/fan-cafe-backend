package com.example.fan_cafe.auth.interfaces.dto;

import com.example.fan_cafe.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserInfoResponse {

    private final Long id;
    private final String email;
    private final String nickname;
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
