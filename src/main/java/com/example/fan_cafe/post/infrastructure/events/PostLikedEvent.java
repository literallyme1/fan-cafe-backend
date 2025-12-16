package com.example.fan_cafe.post.infrastructure.events;

public record PostLikedEvent(Long postId, Long likerId, Long postAuthorId) {}
