package com.example.fan_cafe.notification.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
//@EnableWebSocketMessageBroker
@EnableWebSocket
@RequiredArgsConstructor
//WebSocketConfigurer 브로커시
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
//TODO:채팅

//    //메세지 브로커 설정 (메세지를 중간에서 받는 중계소)
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        // 서버 → 클라이언트 메시지 prefix(주소)
//        registry.enableSimpleBroker("/topic", "/queue"); //topic : 다수, queue : 1:1
//        // 클라이언트 → 서버 메시지 prefix
//        registry.setApplicationDestinationPrefixes("/app");
//    }

//    //"/ws/notifications" -> 클라이언트가 webSocket 연결 시 사용 URL
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws/notifications")
//                .addInterceptors(webSocketAuthInterceptor)
//                .setAllowedOriginPatterns("*"); //모든 출처 허용 (TODO : 특정도메인만 가능하게 하는 게 좋음)
//    }
//
//    //WebSocket으로 한 번에 보낼 수 있는 메시지 최대 크기를 제한
//    @Override
//    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
//        registry.setMessageSizeLimit(64 * 1024);
//    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/notifications")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}

