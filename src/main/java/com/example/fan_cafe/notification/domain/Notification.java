package com.example.fan_cafe.notification.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "notification",
        //같은 event_id 저장 X
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_event",
                        columnNames = "event_id"
                )
        },
        indexes = {
                //DB 가 받는 사람 기준으로 정렬해줌.
                @Index(
                        name = "idx_notification_receiver_created",
                        columnList = "receiver_id, created_at DESC"
                )
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    // 알림 받는 사람
    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    private Notification(NotificationType type,
                         String eventId,
                         Long receiverId,
                         String message) {
        this.type = type;
        this.eventId = eventId;
        this.receiverId = receiverId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
