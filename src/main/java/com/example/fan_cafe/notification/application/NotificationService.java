package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.exception.NotificationErrorCode;
import com.example.fan_cafe.notification.infrastructure.JpaNotificationRepository;
import com.example.fan_cafe.notification.infrastructure.NotificationQueryRepositoryImpl;
import com.example.fan_cafe.notification.interfaces.dto.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JpaNotificationRepository repo;
    private final NotificationQueryRepositoryImpl queryRepo;

    @Transactional(readOnly = true)
    public NotificationListResponse list(Long userId, Cursor cursor, int size){
        List<Notification> list = queryRepo.findByReceiverCursor(
                userId, cursor.at(), cursor.id(), size);
        return NotificationListResponse.of(list, size);
    }

    //특정 알람 확인
    @Transactional
    public void markRead(Long userId, Long notificationId){
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        if (!n.getReceiverId().equals(userId)) throw new CustomException(NotificationErrorCode.INVALID_USER);
        n.markRead();
    }

    @Transactional
    public void markAllRead(Long userId){
        // update bulk (성능)
        repo.markAllRead(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId){ return repo.countUnread(userId); }
}
