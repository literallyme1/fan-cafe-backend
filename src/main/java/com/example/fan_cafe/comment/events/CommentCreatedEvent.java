package com.example.fan_cafe.comment.events;

import com.example.fan_cafe.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentCreatedEvent {

    private final String eventId;
    private final NotificationType notificationType;
    private final Long postId;
    private final Long postAuthorId;
    private final Long commentId;
    private final Long commentAuthorId; //누가 달았는 지
    private final LocalDateTime createdAt;
}