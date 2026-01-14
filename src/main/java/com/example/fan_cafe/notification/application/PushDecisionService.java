package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.notification.infrastructure.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PushDecisionService {


    private final WebSocketSessionRegistry sessionRegistry;

    public boolean isOnline(Long userId) {
        return sessionRegistry.isOnline(userId);
    }
}
