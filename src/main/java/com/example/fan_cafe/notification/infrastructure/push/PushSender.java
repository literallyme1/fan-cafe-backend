package com.example.fan_cafe.notification.infrastructure.push;

import com.example.fan_cafe.notification.domain.Notification;

public interface PushSender {

    void send(Long userId, Object payload);
}
