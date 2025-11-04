package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.global.common.HasCreatedAt;
import com.querydsl.core.annotations.QueryProjection;
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

    private Long id;
    private LocalDateTime createdAt;
    private Long authorId;
    private String nickname;
    private String avatarUrl;
    private String content;
    private Long parentId;
    private List<CommentResponse> children;

    @QueryProjection
    public CommentResponse(Long id, Long authorId, String nickname, String avatarUrl, String content, Long parentId) {
        this.id = id;
        this.authorId = authorId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.content = content;
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
