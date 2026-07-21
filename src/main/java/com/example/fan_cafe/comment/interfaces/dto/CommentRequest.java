package com.example.fan_cafe.comment.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {

    @NotNull(message = "게시글이 불분명합니다.")
    @Schema(description = "게시글 식별자", example = "3201")
    private Long postId;
    @NotBlank(message = "댓글을 입력해주세요.")
    @Schema(description = "댓글 내용", example = "이번 앨범 정말 기대돼요!")
    private String content;
    @Schema(description = "부모 댓글 식별자", example = "8801", nullable = true)
    private Long parentId;
}
