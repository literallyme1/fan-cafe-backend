package com.example.fan_cafe.post.interfaces.dto;

import com.example.fan_cafe.global.common.HasCreatedAt;
import com.example.fan_cafe.post.domain.Post;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.dialect.function.PostgreSQLTruncRoundFunction;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class PostResponse implements HasCreatedAt {

    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;
    private final int likeCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final List<String> imageUrls;
    boolean isLiked;
    boolean isBookmarked;

    @QueryProjection
    public PostResponse(Long id,
                        String title,
                        String content,
                        String nickname,
                        int likeCount,
                        int commentCount,
                        LocalDateTime createdAt,
                        List<String> imageUrls,
                        boolean isLiked,
                        boolean isBookmarked) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.nickname = nickname;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.imageUrls = imageUrls;
        this.isLiked = isLiked;
        this.isBookmarked = isBookmarked;
    }


    public static PostResponse from(Post post, boolean isLiked, boolean isBookmarked) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .nickname(post.getUser().getNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .imageUrls(post.getImageUrls())
                .isLiked(isLiked)
                .isBookmarked(isBookmarked)
                .build();
    }
}
