package com.example.fan_cafe.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisService redisService;

    @GetMapping("/redis-test")
    public String test() {
        redisService.set("test", "success", Duration.ofMinutes(3));
        return redisService.get("test");
    }
}
