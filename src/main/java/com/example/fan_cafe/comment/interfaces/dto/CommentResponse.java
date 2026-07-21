package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.global.common.HasCreatedAt;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse implements HasCreatedAt {

    @Schema(description = "댓글 식별자", example = "8802")
    private Long id;
    @Schema(description = "작성 시각", example = "2026-07-21T19:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "작성자 식별자", example = "101")
    private Long authorId;
    @Schema(description = "작성자 닉네임", example = "별빛팬")
    private String nickname;
    @Schema(description = "작성자 프로필 이미지", example = "https://cdn.fancafe.kr/users/101/avatar.jpg")
    private String avatarUrl;
    @Schema(description = "좋아요 수", example = "27")
    private int likeCount;
    @Schema(description = "내 좋아요 여부", example = "true")
    private boolean liked;
    @Schema(description = "댓글 내용", example = "무대 영상도 기다리고 있어요.")
    private String content;
    @Schema(description = "부모 댓글 식별자", example = "8801", nullable = true)
    private Long parentId;
    @Schema(description = "답글 목록", example = "[{\"id\":8803,\"content\":\"저도 기대돼요!\"}]")
    private List<CommentResponse> children;

    @QueryProjection
    public CommentResponse(Long id,
                           Long authorId,
                           String nickname,
                           String avatarUrl,
                           String content,
                           int likeCount,
                           boolean liked,
                           Long parentId) {
        this.id = id;
        this.authorId = authorId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.content = content;
        this.likeCount = likeCount;
        this.liked = liked;
        this.parentId = parentId;
        this.children = new ArrayList<>();
    }

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorId(comment.getUser().getId())
                .nickname(comment.getUser().getNickname())
                .avatarUrl(comment.getUser().getAvatarUrl())
                .content(comment.getDeletedAt() != null ? "[삭제된 댓글입니다]" : comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .children(new ArrayList<>())
                .build();
    }
}
