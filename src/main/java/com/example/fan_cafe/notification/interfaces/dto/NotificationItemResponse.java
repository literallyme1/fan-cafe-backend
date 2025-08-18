package com.example.fan_cafe.notification.interfaces.dto;

import com.example.fan_cafe.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationItemResponse(
        Long id, String type, String message,
        Long actorId, Long postId, Long commentId,
        boolean read, LocalDateTime createdAt) {

    public static NotificationItemResponse from(Notification n){
        return new NotificationItemResponse(
                n.getId(), n.getType().name(), n.getMessage(),
                n.getActorId(),
                n.getTarget()!=null ? n.getTarget().getPostId() : null,
                n.getTarget()!=null ? n.getTarget().getCommentId() : null,
                !n.isUnread(),
                n.getCreatedAt()
        );
    }
}
