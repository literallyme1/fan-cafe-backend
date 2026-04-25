package com.example.fan_cafe.notification.infrastructure.push;

public interface MessageSender {

    void send(Long userId, Object payload);
}
