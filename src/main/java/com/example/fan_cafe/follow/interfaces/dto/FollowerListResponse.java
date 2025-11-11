package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;

import java.util.List;

public record FollowerListResponse (
        List<FollowResponse> follower,
        Cursor nextCursor,
        boolean hasNext
){
    public static FollowerListResponse from(List<FollowResponse> follower,
                                            Cursor cursor){
        return new FollowerListResponse(follower, cursor, cursor != null);
    }
}
