package com.example.fan_cafe.order.payment.client;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PaymentClientConfig {
    @Bean
    RestClient paymentRestClient(PaymentClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String traceId = MDC.get("traceId");
                    if (traceId != null) request.getHeaders().set("X-Trace-Id", traceId);
                    return execution.execute(request, body);
                })
                .build();
    }
}
