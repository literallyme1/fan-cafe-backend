package com.example.fan_cafe.post.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class PostGetResponse {

    private final List<PostDto> data;
    private final Long nextCursorId;
    private final LocalDateTime nextCursorCreatedAt;
    private final boolean hasNext;

    public static PostGetResponse from(List<PostDto> data,
                                       Long nextCursorId,
                                       LocalDateTime nextCursorCreatedAt,
                                       boolean hasNext) {
        return PostGetResponse.builder()
                .data(data)
                .nextCursorId(nextCursorId)
                .nextCursorCreatedAt(nextCursorCreatedAt)
                .hasNext(hasNext)
                .build();
    }

}
