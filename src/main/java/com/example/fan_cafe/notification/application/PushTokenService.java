package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.notification.domain.push.PushPlatform;
import com.example.fan_cafe.notification.domain.push.PushToken;
import com.example.fan_cafe.notification.infrastructure.push.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    @Transactional
    public void registerOrUpdate(
            Long userId,
            String token,
            PushPlatform platform
    ) {
        pushTokenRepository.findByUserIdAndToken(userId, token)
                .ifPresentOrElse(
                        PushToken::markUsed,
                        () -> {
                            pushTokenRepository.save(
                                    new PushToken(userId, token, platform)
                            );
                        }
                );
    }


}
