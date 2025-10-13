package com.example.fan_cafe.post.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class PostListResponse {

    private final List<PostResponse> data;
    private final Cursor nextCursor;
    private final boolean hasNext;

    public static PostListResponse from(List<PostResponse> data,
                                        Cursor nextCursor,
                                        boolean hasNext) {
        return PostListResponse.builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

}
