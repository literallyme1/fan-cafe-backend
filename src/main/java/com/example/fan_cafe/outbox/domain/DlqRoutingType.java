package com.example.fan_cafe.outbox.domain;

/**
 * DLQ로 보낼 때 실패 원인을 구분한다(Slack·DB 공통).
 */
public enum DlqRoutingType {

    /** 재시도 한도(x-retry-count 기준)를 넘긴 경우. */
    RETRY_EXCEEDED,

    /** 재시도 의미가 없는 영구 오류로 즉시 보존하는 경우. */
    NON_RETRYABLE
}
