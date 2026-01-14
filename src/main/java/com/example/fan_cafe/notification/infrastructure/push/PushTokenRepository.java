package com.example.fan_cafe.notification.infrastructure.push;

import com.example.fan_cafe.notification.domain.push.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByUserIdAndToken(Long userId, String token);

    List<PushToken> findAllByUserIdAndActiveTrue(Long userId);

}
