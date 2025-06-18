package com.example.fan_cafe.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ApiResponseStatus {

    SUCCESS("S001", HttpStatus.OK, "요청에 성공했습니다."),
    CREATED("S002", HttpStatus.CREATED, "리소스가 성공적으로 생성되었습니다."),
    VALIDATION_ERROR("C001", HttpStatus.BAD_REQUEST, "유효성 검사 실패"),
    UNAUTHORIZED("A001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("A002", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("C004", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR("S999", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ApiResponseStatus(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
