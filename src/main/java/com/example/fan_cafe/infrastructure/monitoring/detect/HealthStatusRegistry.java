package com.example.fan_cafe.infrastructure.monitoring.detect;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//이전 Health 상태 저장
@Component
public class HealthStatusRegistry {

    private final Map<String, ComponentHealthStatus> store = new ConcurrentHashMap<>();

    public ComponentHealthStatus get(String component) {
        return store.get(component);
    }

    public void update(String component, ComponentHealthStatus status){
        store.put(component, status);
    }
}
