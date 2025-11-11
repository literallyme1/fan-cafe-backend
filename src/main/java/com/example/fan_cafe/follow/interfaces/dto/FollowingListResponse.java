package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;

import java.util.List;

public record FollowingListResponse(
        List<FollowResponse> following,
        Cursor nextCursor,
        boolean hasNext
){
    public static FollowingListResponse from(List<FollowResponse> following,
                                             Cursor cursor){
        return new FollowingListResponse(following, cursor, cursor != null);
    }
}
