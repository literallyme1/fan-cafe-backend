package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void saveNotification(Long receiverId,
                                 String eventId,
                                 String message) {
        Notification notification = Notification.builder()
                .receiverId(receiverId)
                .eventId(eventId)
                .message(message)
                .build();

        notificationRepository.save(notification);

    }
}
