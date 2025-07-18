package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements BaseErrorCode{

    INTERNAL_SERVER_ERROR("G001", HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 서버 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE("G002", HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 서비스를 이용할 수 없습니다."),

    // 요청 오류
    BAD_REQUEST("G010", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    METHOD_NOT_ALLOWED("G011", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE("G012", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),

    // 요청 제한
    TOO_MANY_REQUESTS("G030", HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");


    private final String code;
    private final HttpStatus status;
    private final String message;

    GlobalErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
