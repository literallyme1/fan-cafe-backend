package com.example.fan_cafe.post.interfaces.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class CachedPostItem {

    private final Long id;
    private final String title;
    private final String content;

    //작성자 정보
    private final Long authorId;
    private final String nickname;
    private final String avatarUrl;
    private final int likeCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final List<String> imageUrls;

    @QueryProjection
    public CachedPostItem(Long id,
                        String title,
                        String content,
                        Long authorId,
                        String nickname,
                        String avatarUrl,
                        int likeCount,
                        int commentCount,
                        LocalDateTime createdAt,
                        List<String> imageUrls) {
        this.id = id;
        this.title = title;
        this.content = content;
        this. authorId = authorId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.imageUrls = imageUrls;
    }
}
