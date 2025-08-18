package com.example.fan_cafe.post.events;

public record PostLikedEvent(Long postId, Long likerId, Long postAuthorId) {}
