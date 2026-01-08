package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.comment.events.messaging.CommentCreatedEvent;
import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.NotificationType;
import com.example.fan_cafe.notification.infrastructure.NotificationRepository;
import com.example.fan_cafe.notification.infrastructure.websocket.realtime.WebSocketNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationSender websocketSender;

    @Transactional
    public Notification saveNotification(Long receiverId,
                                 String eventId,
                                 String message) {
        try {
            Notification notification = Notification.builder()
                    .receiverId(receiverId)
                    .eventId(eventId)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .type(NotificationType.COMMENT)

                    .build();

            return notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("🔥 SAVE FAILED", e);
            throw e;
        }
}

    @Transactional
    public void createAndDispatchNotification(CommentCreatedEvent event){
        //1. 알림 저장
        Notification notification = saveNotification(
                event.getPostAuthorId(),
                event.getEventId(),
                "내 게시글에 댓글이 달렸습니다."
        );

        //2. 온라인 일 시 실시간 전송
        websocketSender.sendIfOnline(
                event.getPostAuthorId(),
                notification.toSimplePayload());

    }
}


