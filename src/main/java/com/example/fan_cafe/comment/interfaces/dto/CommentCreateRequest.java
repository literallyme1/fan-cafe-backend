package com.example.fan_cafe.comment.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "게시글이 불분명합니다.")
    private Long postId;
    @NotBlank(message = "댓글을 입력해주세요.")
    private String content;
    private Long parentId;
}
