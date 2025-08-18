package com.example.fan_cafe.notification.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements BaseErrorCode {

    NOTIFICATION_NOT_FOUND("N001", HttpStatus.NOT_FOUND, "알림 기록을 찾을 수 없습니다."),
    INVALID_USER("N002", HttpStatus.FORBIDDEN, "사용자는 알람에 접근할 수 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
