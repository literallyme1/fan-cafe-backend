package com.example.fan_cafe.notification.application.retry;
import com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames;
import org.eclipse.angus.mail.iap.ConnectionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;

@Component
public class RetryPolicy {

    public boolean isRetryable(Throwable e) {
        return e instanceof TransientDataAccessException //DB 잠깐 문제
            || e instanceof SocketTimeoutException //네트워크 타임 아웃
            || e instanceof ConnectionException; //일시 연결 실패
    }

    //실패횟수에 맞는 목적지 설정
    public RetryTarget decideRetryTarget(int retryCount) {
        if (retryCount == 0) return RetryTarget.RETRY_5S;
        if (retryCount == 1) return RetryTarget.RETRY_30S;
        return RetryTarget.DLQ;
    }
}
