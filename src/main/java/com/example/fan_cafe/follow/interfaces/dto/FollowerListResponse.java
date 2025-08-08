package com.example.fan_cafe.follow.interfaces.dto;

import java.util.List;

public record FollowerListResponse (
        List<FollowerItemResponse> items,
        boolean hasNext
){}
