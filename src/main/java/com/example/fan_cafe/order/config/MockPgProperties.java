package com.example.fan_cafe.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mock PG 웹훅 설정. {@code mock.pg.webhook-secret} 로 HMAC 서명 검증에 사용한다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mock.pg")
public class MockPgProperties {

    private String webhookSecret;
}
