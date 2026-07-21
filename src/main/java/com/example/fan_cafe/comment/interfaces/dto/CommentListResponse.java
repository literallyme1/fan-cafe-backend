package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CommentListResponse {
    @Schema(description = "댓글 목록", example = "[{\"id\":8802,\"content\":\"이번 앨범 정말 기대돼요!\"}]")
    private List<CommentResponse> comments;
    @Schema(description = "조회 시작 커서", example = "{\"id\":8802,\"at\":\"2026-07-21T19:30:00\"}")
    private final Cursor afterCursor;
    @Schema(description = "다음 조회 커서", example = "{\"id\":8792,\"at\":\"2026-07-21T18:30:00\"}")
    private final Cursor nextCursor;
    @Schema(description = "다음 댓글 존재 여부", example = "true")
    private boolean hasNext;

    public static CommentListResponse from(List<CommentResponse> comments,
                                           Cursor afterCursor,
                                           Cursor nextCursor) {
        return CommentListResponse.builder()
                .comments(comments)
                .afterCursor(afterCursor)
                .nextCursor(nextCursor)
                .hasNext(nextCursor != null)
                .build();
    }
}
