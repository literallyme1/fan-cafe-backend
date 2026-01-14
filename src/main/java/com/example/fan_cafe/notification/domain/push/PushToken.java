package com.example.fan_cafe.notification.domain.push;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "push_token",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "token"})
        }
)
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    //FCM 토큰
    @Column(nullable = false, length = 255)
    private String token;

    // 디바이스 플랫폼
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PushPlatform platform;

    // 유효 여부 (실패 시 false)
    @Column(nullable = false)
    private boolean active = true;

    // 마지막 사용 시점
    private LocalDateTime lastUsedAt;

    public PushToken(Long userId, String token, PushPlatform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }
}
