package com.example.fan_cafe.bookmark.interfaces.dto;

import com.example.fan_cafe.post.domain.Post;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BookmarkListItemResponse {

    private Long postId;
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
