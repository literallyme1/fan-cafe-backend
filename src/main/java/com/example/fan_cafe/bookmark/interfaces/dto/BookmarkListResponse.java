package com.example.fan_cafe.bookmark.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class BookmarkListResponse {
    private List<BookmarkListItemResponse> bookmarks;
    private boolean hasNext;

    public static BookmarkListResponse from(List<BookmarkListItemResponse> bookmarks, boolean hasNext) {
        return BookmarkListResponse.builder()
                .bookmarks(bookmarks)
                .hasNext(hasNext)
                .build();
    }
}
