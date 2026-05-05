package com.example.fan_cafe.notification.infrastructure.websocket;

import com.example.fan_cafe.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtTokenProvider;
    private final Environment env;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        // 1. test 환경이면 그냥 통과
        if (Arrays.stream(env.getActiveProfiles())
                .anyMatch(p -> p.contains("test"))) {
            attributes.put("userId", 1L);
            return true;
        }

        //첫요청은 HTTP 이므로 ServletRequest 지 확인
        if(!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest httpRequest = servletServerHttpRequest.getServletRequest();
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false; // 인증 실패 → 연결 거부
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.isValid(token)) {
            return false;
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // WebSocket 세션에 userId 저장
        attributes.put("userId", userId);

        return true; // 연결 허용
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 사용 안 함
    }
}
