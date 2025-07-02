package com.example.fan_cafe.comment.interfaces.dto;

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
}
