package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.global.common.HasCreatedAt;

public record FollowResponse (

        Long userId,
        String nickname,
        String avatarUrl,

        boolean isFollowingBack
) {
    public static FollowResponse from(Follow follow, boolean isFollowed) {
        return new FollowResponse(
                follow.getFollowing().getId(),
                follow.getFollowing().getNickname(),
                follow.getFollowing().getAvatarUrl(),
                isFollowed
        );
    }
}
