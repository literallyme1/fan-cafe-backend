package com.example.fan_cafe.infrastructure.monitoring.detect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthDetectScheduler {

    //상태 직접 체크
    private final RedisHealthProbe redisProbe;
    private final RabbitConsumerHealthProbe rabbitProbe;
    private final HttpLatencyProbe httpLatencyProbe;

    //상태변화 감지
    private final HealthStatusChangeDetector detector;

    @Scheduled(fixedDelayString = "${monitoring.detect.interval}") //현재 1분으로 해놓음.
    public void detect() {
        log.warn("[SCHEDULER] detect 실행됨");
        detector.detect("REDIS", redisProbe.check());
        detector.detect("RABBIT_CONSUMER", rabbitProbe.check());
        detector.detect("HTTP_P95", httpLatencyProbe.check());
    }

}
