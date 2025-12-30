package com.example.fan_cafe.notification.application.retry;

public enum RetryTarget {
    RETRY_5S,     // 5초 대기 큐
    RETRY_30S,    // 30초 대기 큐
    DLQ           // 최종 실패 보관소
}
