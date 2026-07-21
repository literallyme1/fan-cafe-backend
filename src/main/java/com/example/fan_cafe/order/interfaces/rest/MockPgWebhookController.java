package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.MockPgWebhookService;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Mock PG가 결제 결과를 푸시하는 웹훅 엔드포인트.
 * HMAC-SHA256 서명 검증 통과 후에만 주문 상태를 변경한다.
 */
@RestController
@RequestMapping("/api/mock-pg")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "PG 웹훅", description = "서명 기반 Mock PG 결제 결과 수신")
public class MockPgWebhookController {

    public static final String HEADER_TIMESTAMP = "X-Mock-PG-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Mock-PG-Signature";

    private final MockPgWebhookService mockPgWebhookService;

    @PostMapping("/webhook")
    @Operation(summary = "결제 웹훅 수신", description = "HMAC 서명을 검증하고 결제 승인 또는 실패 상태를 반영함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본문 또는 헤더 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "서명 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "결제 상태 충돌")
    })
    public ApiResponse<OrderQueryResponse> webhook(
            @RequestBody String rawBody,
            @RequestHeader(HEADER_TIMESTAMP) String timestamp,
            @RequestHeader(HEADER_SIGNATURE) String signature
    ) {
        OrderQueryResponse result = mockPgWebhookService.receive(rawBody, timestamp, signature);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, result);
    }
}
