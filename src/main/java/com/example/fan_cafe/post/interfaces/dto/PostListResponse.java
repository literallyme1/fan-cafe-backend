package com.example.fan_cafe.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.fan_cafe.global.common.Cursor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PostListResponse {

    @Schema(description = "게시글 목록", example = "[{\"id\":3201,\"title\":\"서울 콘서트 첫날 후기\"}]")
    private final List<PostResponse> data;
    @Schema(description = "다음 조회 커서", example = "{\"id\":3191,\"at\":\"2026-07-21T20:00:00\"}")
    private final Cursor nextCursor;
    @Schema(description = "조회 시작 커서", example = "{\"id\":3201,\"at\":\"2026-07-21T22:10:00\"}")
    private final Cursor afterCursor;
    @Schema(description = "다음 게시글 존재 여부", example = "true")
    private final boolean hasNext;

    public static PostListResponse fromAfterCursor(List<PostResponse> data,
                                                    Cursor afterCursor
    ) {
        return PostListResponse.builder()
                .data(data)
                .hasNext(false)
                .nextCursor(null)
                .afterCursor(afterCursor)
                .build();
    }

    public static PostListResponse fromCursors(List<PostResponse> data,
                                                    Cursor nextCursor,
                                                    Cursor afterCursor
    ) {
        return PostListResponse.builder()
                .data(data)
                .hasNext(nextCursor != null)
                .nextCursor(nextCursor)
                .afterCursor(afterCursor)
                .build();
    }





}
