package com.example.fan_cafe.user.interfaces.dto;

import com.example.fan_cafe.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserResponse {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().toString())
                .build();
    }
}
