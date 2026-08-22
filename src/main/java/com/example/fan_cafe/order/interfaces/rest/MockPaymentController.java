package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentApproveRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentCancelRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentFailRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Mock PG 결제 승인/실패 API.
 * 실제 PG 연동 없이 결제 승인/실패/취소(환불) Mock 전이를 수행한다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Mock 결제", description = "결제 승인과 실패 및 환불 시뮬레이션")
public class MockPaymentController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/mock-payment/approve")
    @Operation(summary = "결제 승인", description = "결제 금액과 멱등 키를 검증하고 주문을 결제 완료 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "금액 또는 멱등 키 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "결제 상태 충돌")
    })
    public ApiResponse<OrderQueryResponse> approve(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId,
            @RequestBody @Valid MockPaymentApproveRequest request
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.approveMockPayment(user, orderId, request));
    }

    @PostMapping("/{orderId}/mock-payment/fail")
    @Operation(summary = "결제 실패", description = "결제 대기 주문을 결제 실패 상태로 변경함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실패 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "변경 불가 상태")
    })
    public ApiResponse<OrderQueryResponse> fail(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId,
            @RequestBody(required = false) MockPaymentFailRequest request
    ) {
        MockPaymentFailRequest body = request != null ? request : new MockPaymentFailRequest();
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.failMockPayment(user, orderId, body));
    }

    /** PAID 주문 전체 취소/환불 (PAID → REFUNDED, Outbox PAYMENT_REFUNDED) */
    @PostMapping("/{orderId}/mock-payment/cancel")
    @Operation(summary = "결제 환불", description = "결제 완료 주문을 환불하고 재고와 Outbox 이벤트를 반영함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환불 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "sagaId 또는 환불 상태 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "환불 불가 상태")
    })
    public ApiResponse<OrderQueryResponse> cancel(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId,
            @RequestBody @Valid MockPaymentCancelRequest request
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.cancelMockPayment(user, orderId, request));
    }
}
