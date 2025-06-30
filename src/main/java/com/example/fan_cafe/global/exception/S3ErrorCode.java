package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum S3ErrorCode implements BaseErrorCode{

    FILE_UPLOAD_FAILED("U001", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED("U001", HttpStatus.INTERNAL_SERVER_ERROR, "파일 제거에 실패했습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    S3ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

