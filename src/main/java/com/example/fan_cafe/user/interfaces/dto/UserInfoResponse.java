package com.example.fan_cafe.user.interfaces.dto;

import com.example.fan_cafe.user.domain.User;
import lombok.Builder;

@Builder
public class UserInfoResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().toString())
                .build();
    }
}
