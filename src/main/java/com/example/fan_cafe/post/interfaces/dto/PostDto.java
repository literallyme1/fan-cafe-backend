package com.example.fan_cafe.post.interfaces.dto;

import com.example.fan_cafe.post.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class PostDto {

    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;
    private final int likeCount;
    private final int commentCount;
    private final LocalDateTime createdAt;

    public static PostDto from(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .nickname(post.getUser().getNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
