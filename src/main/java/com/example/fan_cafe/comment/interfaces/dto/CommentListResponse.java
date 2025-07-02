package com.example.fan_cafe.comment.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CommentListResponse {
    private List<CommentResponse> comments;
}
