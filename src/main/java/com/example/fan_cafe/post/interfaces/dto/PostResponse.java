package com.example.fan_cafe.post.interfaces.dto;

import com.example.fan_cafe.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class PostResponse {

    private final List<PostDto> data;
    private final Long nextCursorId;
    private final LocalDateTime nextCursorCreatedAt;
    private final boolean hasNext;

    public static PostResponse from(List<PostDto> data,
                                       Long nextCursorId,
                                       LocalDateTime nextCursorCreatedAt,
                                       boolean hasNext) {
        return PostResponse.builder()
                .data(data)
                .nextCursorId(nextCursorId)
                .nextCursorCreatedAt(nextCursorCreatedAt)
                .hasNext(hasNext)
                .build();
    }

}
