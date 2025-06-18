package com.example.fan_cafe.user;

import lombok.Builder;

@Builder
public class UserRegisterResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;

    public static UserRegisterResponse from(User user) {
        return UserRegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().toString())
                .build();
    }
}
