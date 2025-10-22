package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CommentListResponse {
    private List<CommentResponse> comments;
    private final Cursor afterCursor;
    private final Cursor nextCursor;
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
