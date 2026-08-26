package com.example.fan_cafe.campaign.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CampaignErrorCode implements BaseErrorCode {
    INVALID_AMOUNT("C001", HttpStatus.BAD_REQUEST, "금액은 1,000원 이상이며 1,000원 단위여야 합니다."),
    INVALID_PERIOD("C002", HttpStatus.BAD_REQUEST, "Campaign 시작 시각은 마감 시각보다 빨라야 합니다."),
    CAMPAIGN_NOT_OPEN("C003", HttpStatus.CONFLICT, "현재 Campaign에는 참여할 수 없습니다."),
    TARGET_AMOUNT_EXCEEDED("C004", HttpStatus.CONFLICT, "Campaign 목표 금액을 초과할 수 없습니다."),
    INVALID_CAMPAIGN_STATE("C005", HttpStatus.CONFLICT, "허용되지 않은 Campaign 상태 전이입니다."),
    INVALID_CONTRIBUTION_STATE("C006", HttpStatus.CONFLICT, "허용되지 않은 Contribution 상태 전이입니다."),
    CAMPAIGN_NOT_FOUND("C007", HttpStatus.NOT_FOUND, "Campaign을 찾을 수 없습니다."),
    CONTRIBUTION_NOT_FOUND("C008", HttpStatus.NOT_FOUND, "Contribution을 찾을 수 없습니다."),
    CONTRIBUTION_NOT_REFUNDABLE("C009", HttpStatus.CONFLICT, "현재 상태에서는 참여를 환불할 수 없습니다."),
    APPROVAL_AFTER_DEADLINE("C010", HttpStatus.CONFLICT, "마감 이후 승인된 참여는 환불 대상입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    CampaignErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
