package com.example.fan_cafe.post.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostCommentUpdatedEvent {
    private Long postId;
}
