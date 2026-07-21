package com.example.fan_cafe.bookmark.interfaces.dto;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Builder
@Getter
public class BookmarkResponse {
    @Schema(description = "게시글 식별자", example = "3201")
    private Long postId;
    @Schema(description = "북마크 등록 여부", example = "true")
    private boolean marked;

    public static BookmarkResponse from(Long postId, boolean marked){
        return BookmarkResponse.builder()
                .postId(postId)
                .marked(marked)
                .build();
    }
}
