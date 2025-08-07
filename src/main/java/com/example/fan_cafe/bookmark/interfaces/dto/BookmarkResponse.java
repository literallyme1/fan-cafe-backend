package com.example.fan_cafe.bookmark.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BookmarkResponse {
    private Long postId;
    private boolean marked;

    public static BookmarkResponse from(Long postId, boolean marked){
        return BookmarkResponse.builder()
                .postId(postId)
                .marked(marked)
                .build();
    }
}
