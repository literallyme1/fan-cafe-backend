package com.example.fan_cafe.notification.infrastructure;

import com.example.fan_cafe.notification.domain.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {}