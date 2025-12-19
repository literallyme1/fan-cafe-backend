package com.example.fan_cafe.comment.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentCreatedEvent {

    private final String eventId;
    private final Long postId;
    private final Long postAuthorId;
    private final Long commentId;
    private final Long commentAuthorId;
    private final LocalDateTime createdAt;
}