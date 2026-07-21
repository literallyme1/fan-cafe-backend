package com.example.fan_cafe.bookmark.interfaces.dto;

import com.example.fan_cafe.post.domain.Post;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BookmarkListItemResponse {

    @Schema(description = "게시글 식별자", example = "3201")
    private Long postId;
    @Schema(description = "대표 이미지 URL", example = "https://cdn.fancafe.kr/posts/3201/thumb.jpg")
    private String thumbnailUrl;

    @QueryProjection
    public BookmarkListItemResponse(Long postId, String thumbnailUrl) {
        this.postId = postId;
        this.thumbnailUrl = thumbnailUrl;
    }
    public static BookmarkListItemResponse from(Post post) {
        return BookmarkListItemResponse.builder()
                .postId(post.getId())
                .thumbnailUrl(post.getThumbnailUrl())
                .build();
    }
}
