package com.example.fan_cafe.infrastructure.monitoring.detect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpLatencyProbe {

    private final MeterRegistry meterRegistry;

    public ComponentHealthStatus check() {
        try {
            Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();

            if (timers.isEmpty()) {
                return ComponentHealthStatus.UP;
            }

            // 🔥 전체 요청 중 가장 느린 서버 처리 시간 (핵심)
            double maxMillis = timers.stream()
                    .filter(timer -> timer.count() > 0)
                    .mapToDouble(timer -> timer.max(TimeUnit.MILLISECONDS))
                    .max()
                    .orElse(Double.NaN);

            log.warn("[METRIC] HTTP MAX latency = {} ms", maxMillis);

            if (Double.isNaN(maxMillis)) {
                return ComponentHealthStatus.UP;
            }

            return decideStatus(maxMillis);

        } catch (Exception e) {
            log.error("[METRIC] HttpLatencyProbe error", e);
            return ComponentHealthStatus.DOWN;
        }
    }

    private ComponentHealthStatus decideStatus(double millis) {
        if (millis >= 1000) {
            return ComponentHealthStatus.DOWN;
        }
        if (millis >= 800) {
            return ComponentHealthStatus.DEGRADED;
        }
        return ComponentHealthStatus.UP;
    }
}