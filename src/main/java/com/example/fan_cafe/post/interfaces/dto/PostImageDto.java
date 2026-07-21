package com.example.fan_cafe.post.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostImageDto(
        @Schema(description = "게시글 식별자", example = "3201") Long postId,
        @Schema(description = "이미지 URL", example = "https://cdn.fancafe.kr/posts/3201/01.jpg") String url
) {}
