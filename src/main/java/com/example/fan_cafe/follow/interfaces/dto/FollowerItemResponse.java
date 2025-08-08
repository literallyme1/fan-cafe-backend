package com.example.fan_cafe.follow.interfaces.dto;

import java.time.LocalDateTime;

public record FollowerItemResponse(
    Long userId,
    String name,
    String avatarUrl,
    boolean isFollowedByViewer,
    LocalDateTime followedAt
){}
