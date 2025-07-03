package com.example.fan_cafe.comment.interfaces.dto;

import com.example.fan_cafe.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


@Builder
@Getter
public class CommentResponse {

    private Long id;
    private String writer;
    private String content;
    private Long parentId;
    private List<CommentResponse> children = new ArrayList<>();

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .writer(comment.getUser().getNickname())
                .content(comment.getContent())
                .parentId(comment.getParent().getId())
                .build();
    }
}
