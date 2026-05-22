package com.example.fan_cafe.global.redis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("ci")
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate redis;

//    @Test
//    void redis_set_get_test() {
//        redis.opsForValue().set("test:key", "hello");
//        String value = redis.opsForValue().get("test:key");
//
//        assertEquals("hello", value);
//    }
}
