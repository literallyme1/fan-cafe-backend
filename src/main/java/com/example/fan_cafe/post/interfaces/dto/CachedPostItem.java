package com.example.fan_cafe.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CachedPostItem {

    @Schema(description = "게시글 식별자", example = "3201")
    private final Long id;
    @Schema(description = "게시글 제목", example = "서울 콘서트 첫날 후기")
    private final String title;
    @Schema(description = "게시글 내용", example = "무대 연출과 앙코르가 정말 인상적이었어요.")
    private final String content;

    //작성자 정보
    @Schema(description = "작성자 식별자", example = "101")
    private final Long authorId;
    @Schema(description = "작성자 닉네임", example = "별빛팬")
    private final String nickname;
    @Schema(description = "작성자 프로필 이미지", example = "https://cdn.fancafe.kr/users/101/avatar.jpg")
    private final String avatarUrl;
    @Schema(description = "좋아요 수", example = "215")
    private final int likeCount;
    @Schema(description = "댓글 수", example = "48")
    private final int commentCount;
    @Schema(description = "작성 시각", example = "2026-07-21T22:10:00")
    private final LocalDateTime createdAt;
    @Schema(description = "게시글 이미지 URL 목록", example = "[\"https://cdn.fancafe.kr/posts/3201/01.jpg\"]")
    private List<String> imageUrls;

    @QueryProjection
    public CachedPostItem(Long id,
                        String title,
                        String content,
                        Long authorId,
                        String nickname,
                        String avatarUrl,
                        int likeCount,
                        int commentCount,
                        LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this. authorId = authorId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    public void setImageUrls(List<String> urls) {
        this.imageUrls = urls;
    }
}
