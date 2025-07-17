package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.comment.domain.Comment;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    private Long id;
    private String writer;
    private String content;
    private Long parentId;
    private List<CommentResponse> children;

    @QueryProjection
    public CommentResponse(Long id, String writer, String content, Long parentId) {
        this.id = id;
        this.writer = writer;
        this.content = content;
        this.parentId = parentId;
        this.children = new ArrayList<>();
    }

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .writer(comment.getUser().getNickname())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .children(new ArrayList<>())
                .build();
    }
}
