package com.example.fan_cafe.follow.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FollowingListResponse(
        @Schema(description = "팔로잉 목록", example = "[{\"id\":5101,\"userId\":202,\"nickname\":\"달빛응원단\"}]")
        List<FollowResponse> following,
        @Schema(description = "다음 조회 커서", example = "{\"id\":5101,\"at\":\"2026-07-20T13:10:00\"}")
        Cursor nextCursor,
        @Schema(description = "다음 팔로잉 존재 여부", example = "true")
        boolean hasNext
){
    public static FollowingListResponse from(List<FollowResponse> following,
                                             Cursor cursor){
        return new FollowingListResponse(following, cursor, cursor != null);
    }
}
