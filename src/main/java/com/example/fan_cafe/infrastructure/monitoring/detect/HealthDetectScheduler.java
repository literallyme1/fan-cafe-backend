package com.example.fan_cafe.infrastructure.monitoring.detect;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HealthDetectScheduler {

    //상태 직접 체크
    private final RedisHealthProbe redisProbe;
    private final RabbitConsumerHealthProbe rabbitProbe;

    //상태변화 감지
    private final HealthStatusChangeDetector detector;

    @Scheduled(fixedDelayString = "${monitoring.detect.interval}") //30초
    public void detect() {
        detector.detect("REDIS", redisProbe.check());
        detector.detect("RABBIT_CONSUMER", rabbitProbe.check());
    }

}
