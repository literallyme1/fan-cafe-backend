package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.NotificationType;
import com.example.fan_cafe.notification.infrastructure.NotificationRepository;
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

    @Transactional
    public void saveNotification(Long receiverId,
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

            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("🔥 SAVE FAILED", e);
            throw e;
        }
}}
