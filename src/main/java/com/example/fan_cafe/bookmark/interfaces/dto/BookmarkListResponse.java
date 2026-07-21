package com.example.fan_cafe.bookmark.interfaces.dto;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class BookmarkListResponse {
    @Schema(description = "북마크 게시글 목록", example = "[{\"postId\":3201,\"thumbnailUrl\":\"https://cdn.fancafe.kr/posts/3201/thumb.jpg\"}]")
    private List<BookmarkListItemResponse> bookmarks;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    public static BookmarkListResponse from(List<BookmarkListItemResponse> bookmarks, boolean hasNext) {
        return BookmarkListResponse.builder()
                .bookmarks(bookmarks)
                .hasNext(hasNext)
                .build();
    }
}
