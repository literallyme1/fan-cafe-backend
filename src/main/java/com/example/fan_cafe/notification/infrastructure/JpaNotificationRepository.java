package com.example.fan_cafe.notification.infrastructure;

import com.example.fan_cafe.notification.domain.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface JpaNotificationRepository extends JpaRepository<Notification, Long> {
    @Query("select count(n) from Notification n where n.receiverId=:uid and n.readAt is null")
    long countUnread(@Param("uid") Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.receiverId=:uid and n.readAt is null")
    int markAllRead(@Param("uid") Long userId, @Param("now") LocalDateTime now);
}