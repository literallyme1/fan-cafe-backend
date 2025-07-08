package com.example.fan_cafe.promotion.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PromotionErrorCode implements BaseErrorCode {

    PROMOTION_INVALID_TIME("P001", HttpStatus.BAD_REQUEST, "종료일은 시작일보다 앞설 수 없습니다."),
    PROMOTION_NOT_FOUND("P002", HttpStatus.NOT_FOUND, "프로모션을 찾을 수 없습니다."),
    NO_IMAGE_PROVIDED("P003", HttpStatus.BAD_REQUEST, "이미지를 1개 이상 게시해주세요.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    PromotionErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
