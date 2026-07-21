package com.example.fan_cafe.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "게시글 식별자", example = "3201")
    private final Long id;
    @Schema(description = "게시글 제목", example = "서울 콘서트 첫날 후기")
    private final String title;
    @Schema(description = "게시글 내용", example = "무대 연출과 앙코르가 정말 인상적이었어요.")
    private final String content;

    //user정보
    @Schema(description = "작성자 식별자", example = "101")
    private final Long authorId;
    @Schema(description = "작성자 닉네임", example = "별빛팬")
    private final String nickname;
    @Schema(description = "작성자 프로필 이미지", example = "https://cdn.fancafe.kr/users/101/avatar.jpg")
    private final String avatarUrl;
    @Schema(description = "좋아요 수", example = "215")
    private final int likeCount;
    @Schema(description = "댓글 수", example = "48")
    private int commentCount;
    @Schema(description = "작성 시각", example = "2026-07-21T22:10:00")
    private final LocalDateTime createdAt;
    @Schema(description = "게시글 이미지 URL 목록", example = "[\"https://cdn.fancafe.kr/posts/3201/01.jpg\"]")
    private final List<String> imageUrls;
    @Schema(description = "내 좋아요 여부", example = "true")
    boolean isLiked;
    @Schema(description = "내 북마크 여부", example = "false")
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
