package com.example.fan_cafe.post.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;
    private final int likeCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
}
