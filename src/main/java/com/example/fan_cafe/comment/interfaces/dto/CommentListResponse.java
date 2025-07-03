package com.example.fan_cafe.comment.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CommentListResponse {
    private List<CommentResponse> comments;
    private boolean hasNext;

    public static CommentListResponse from(List<CommentResponse> comments, boolean hasNext) {
        return CommentListResponse.builder()
                .comments(comments)
                .hasNext(hasNext)
                .build();
    }
}
