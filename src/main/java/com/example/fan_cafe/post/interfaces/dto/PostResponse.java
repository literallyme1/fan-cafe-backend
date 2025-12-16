package com.example.fan_cafe.post.interfaces.dto;

import com.example.fan_cafe.global.common.HasCreatedAt;
import com.example.fan_cafe.post.domain.Post;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class PostResponse implements HasCreatedAt {

    private final Long id;
    private final String title;
    private final String content;

    //user정보
    private final Long authorId;
    private final String nickname;
    private final String avatarUrl;
    private final int likeCount;
    private int commentCount;
    private final LocalDateTime createdAt;
    private final List<String> imageUrls;
    boolean isLiked;
    boolean isBookmarked;

    @QueryProjection
    public PostResponse(Long id,
                        String title,
                        String content,
                        Long authorId,
                        String nickname,
                        String avatarUrl,
                        int likeCount,
                        int commentCount,
                        LocalDateTime createdAt,
                        List<String> imageUrls,
                        boolean isLiked,
                        boolean isBookmarked) {
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
        this.isLiked = isLiked;
        this.isBookmarked = isBookmarked;
    }


    public static PostResponse from(Post post, boolean isLiked, boolean isBookmarked) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getUser().getId())
                .nickname(post.getUser().getNickname())
                .avatarUrl(post.getUser().getAvatarUrl())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .imageUrls(post.getImageUrls())
                .isLiked(isLiked)
                .isBookmarked(isBookmarked)
                .build();
    }

    public static PostResponse from(CachedPostItem item, boolean isLiked, boolean isBookmarked) {
        return PostResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .content(item.getContent())
                .authorId(item.getAuthorId())
                .nickname(item.getNickname())
                .avatarUrl(item.getAvatarUrl())
                .likeCount(item.getLikeCount())
                .commentCount(item.getCommentCount())
                .createdAt(item.getCreatedAt())
                .imageUrls(item.getImageUrls())
                .isLiked(isLiked)
                .isBookmarked(isBookmarked)
                .build();
    }

    public void setCommentCount(int commentCount){
        this.commentCount = commentCount;
    }
}
