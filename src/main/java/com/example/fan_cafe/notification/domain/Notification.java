package com.example.fan_cafe.notification.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification",
        indexes = {
                @Index(name="ix_notif_receiver_created", columnList="receiverId, createdAt DESC, id DESC"),
                @Index(name="ix_notif_unread_receiver", columnList="receiverId, readAt")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private NotificationType type;

    // 알림 받는 사람
    @Column(nullable=false)
    private Long receiverId;

    // 행동 주체 (보낸이)
    @Column(nullable=false)
    private Long actorId;

    @Embedded
    private NotificationTarget target;

    @Column(nullable=false, length=200)
    private String message;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    private Notification(NotificationType type, Long receiverId, Long actorId,
                         NotificationTarget target, String message) {
        this.type = type;
        this.receiverId = receiverId;
        this.actorId = actorId;
        this.target = target;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public static Notification follow(Long receiverId, Long actorId, String actorName){
        return new Notification(NotificationType.FOLLOW, receiverId, actorId,
                NotificationTarget.none(), actorName + "님이 당신을 팔로우했습니다.");
    }
    public static Notification comment(Long receiverId, Long actorId, Long postId, Long commentId, String preview){
        String msg = "댓글: \"" + preview + "\"";
        return new Notification(NotificationType.COMMENT, receiverId, actorId,
                NotificationTarget.forComment(postId, commentId), msg);
    }
    public static Notification like(Long receiverId, Long actorId, Long postId){
        return new Notification(NotificationType.LIKE, receiverId, actorId,
                NotificationTarget.forPost(postId), "게시글을 좋아합니다.");
    }

    public boolean isUnread(){ return readAt == null; }
    public void markRead(){ if (readAt == null) this.readAt = LocalDateTime.now(); }
}
