package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.global.common.HasCreatedAt;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record FollowResponse(
        @Schema(description = "팔로우 식별자", example = "5101")
        Long id,
        @Schema(description = "대상 회원 식별자", example = "202")
        Long userId,
        @Schema(description = "대상 회원 닉네임", example = "달빛응원단")
        String nickname,
        @Schema(description = "프로필 이미지", example = "https://cdn.fancafe.kr/users/202/avatar.jpg")
        String avatarUrl,
        @Schema(description = "팔로우 시각", example = "2026-07-20T13:10:00")
        LocalDateTime createdAt,
        @Schema(description = "맞팔로우 여부", example = "true")
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
