package com.example.fan_cafe.user.interfaces.dto;

import com.example.fan_cafe.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProfileResponse {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String role;
    private final String introduction;
    private final String avatarUrl;
    private final int followerCount;
    private final int followingCount;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().toString())
                .introduction(user.getIntroduction())
                .avatarUrl(user.getAvatarUrl())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .build();
    }
}
