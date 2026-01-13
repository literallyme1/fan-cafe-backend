package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.notification.domain.push.PushToken;
import com.example.fan_cafe.notification.infrastructure.push.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushTokenQueryService {

    private final PushTokenRepository pushTokenRepository;

    // 활성 토큰만 조회
    public List<PushToken> findActiveTokens(Long userId) {
        return pushTokenRepository.findAllByUserIdAndActiveTrue(userId);
    }
}
