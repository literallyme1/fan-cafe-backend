package com.example.fan_cafe.follow.events;

public record FollowedEvent(Long followerId, Long targetId, String followerName) {}

