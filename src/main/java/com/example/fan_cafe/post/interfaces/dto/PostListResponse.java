package com.example.fan_cafe.post.interfaces.dto;

import com.example.fan_cafe.global.common.Cursor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PostListResponse {

    private final List<PostResponse> data;
    private final Cursor nextCursor;
    private final Cursor afterCursor;
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
