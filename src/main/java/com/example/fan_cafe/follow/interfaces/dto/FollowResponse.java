package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.global.common.HasCreatedAt;

import java.time.LocalDateTime;

public record FollowResponse(
        Long id,
        Long userId,
        String nickname,
        String avatarUrl,
        LocalDateTime createdAt,
        boolean isFollowingBack
) implements HasCreatedAt {
    public static FollowResponse from(Follow follow, boolean isFollowed) {
        return new FollowResponse(
                follow.getId(),
                follow.getFollowing().getId(),
                follow.getFollowing().getNickname(),
                follow.getFollowing().getAvatarUrl(),
                follow.getCreatedAt(),
                isFollowed
        );
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
