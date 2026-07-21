package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FollowerListResponse (
        @Schema(description = "팔로워 목록", example = "[{\"id\":5102,\"userId\":203,\"nickname\":\"응원별\"}]")
        List<FollowResponse> follower,
        @Schema(description = "다음 조회 커서", example = "{\"id\":5102,\"at\":\"2026-07-20T14:10:00\"}")
        Cursor nextCursor,
        @Schema(description = "다음 팔로워 존재 여부", example = "true")
        boolean hasNext
){
    public static FollowerListResponse from(List<FollowResponse> follower,
                                            Cursor cursor){
        return new FollowerListResponse(follower, cursor, cursor != null);
    }
}
