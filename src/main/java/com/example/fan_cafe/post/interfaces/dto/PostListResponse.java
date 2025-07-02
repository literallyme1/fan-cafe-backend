package com.example.fan_cafe.post.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class PostListResponse {

    private final List<PostResponse> data;
    private final Long nextCursorId;
    private final LocalDateTime nextCursorCreatedAt;
    private final boolean hasNext;

    public static PostListResponse from(List<PostResponse> data,
                                        Long nextCursorId,
                                        LocalDateTime nextCursorCreatedAt,
                                        boolean hasNext) {
        return PostListResponse.builder()
                .data(data)
                .nextCursorId(nextCursorId)
                .nextCursorCreatedAt(nextCursorCreatedAt)
                .hasNext(hasNext)
                .build();
    }

}
