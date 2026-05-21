package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.MockPgWebhookService;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Mock PG가 결제 결과를 푸시하는 웹훅 엔드포인트.
 * HMAC-SHA256 서명 검증 통과 후에만 주문 상태를 변경한다.
 */
@RestController
@RequestMapping("/api/mock-pg")
@RequiredArgsConstructor
public class MockPgWebhookController {

    public static final String HEADER_TIMESTAMP = "X-Mock-PG-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Mock-PG-Signature";

    private final MockPgWebhookService mockPgWebhookService;

    @PostMapping("/webhook")
    public ApiResponse<OrderQueryResponse> webhook(
            @RequestBody String rawBody,
            @RequestHeader(HEADER_TIMESTAMP) String timestamp,
            @RequestHeader(HEADER_SIGNATURE) String signature
    ) {
        OrderQueryResponse result = mockPgWebhookService.receive(rawBody, timestamp, signature);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, result);
    }
}
