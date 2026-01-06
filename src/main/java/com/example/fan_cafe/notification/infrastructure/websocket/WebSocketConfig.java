package com.example.fan_cafe.notification.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    //메세지 브로커 설정 (메세지를 중간에서 받는 중계소)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 → 클라이언트 메시지 prefix(주소)
        registry.enableSimpleBroker("/topic", "/queue"); //topic : 다수, queue : 1:1
        // 클라이언트 → 서버 메시지 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    //"/ws/notifications" -> 클라이언트가 webSocket 연결 시 사용 URL
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("ws/notifications")
                .setAllowedOriginPatterns("*"); //모든 출처 허용 (TODO : 특정도메인만 가능하게 하는 게 좋음)
    }

}
