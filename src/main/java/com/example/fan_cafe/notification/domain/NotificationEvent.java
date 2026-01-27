package com.example.fan_cafe.notification.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class NotificationEvent {

    private NotificationOpsType type; //종류
    private NotificationLevel level;
    private String title; //요약 메세지
    private String description;
    private Map<String, Object> context; //key-value 로 수치, 상태를 담음.
    private LocalDateTime occurredAt;

    public static NotificationEvent of(
            NotificationOpsType type,
            NotificationLevel level,
            String title,
            String description,
            Map<String, Object> context
    ) {
        return NotificationEvent.builder()
                .type(type)
                .level(level)
                .title(title)
                .description(description)
                .context(context)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
