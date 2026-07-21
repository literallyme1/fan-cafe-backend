package com.example.fan_cafe.follow.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FollowerItemResponse(
    @Schema(description = "회원 식별자", example = "202") Long userId,
    @Schema(description = "회원 닉네임", example = "달빛응원단") String name,
    @Schema(description = "프로필 이미지", example = "https://cdn.fancafe.kr/users/202/avatar.jpg") String avatarUrl,
    @Schema(description = "내 팔로우 여부", example = "true") boolean isFollowedByViewer,
    @Schema(description = "팔로우 시각", example = "2026-07-20T13:10:00") LocalDateTime followedAt
){}
