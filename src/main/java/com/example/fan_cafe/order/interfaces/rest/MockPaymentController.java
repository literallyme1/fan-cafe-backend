package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentApproveRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentFailRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Mock PG 결제 승인/실패 API.
 * 실제 PG 연동 없이 PAYMENT_PENDING → PAID / PAYMENT_FAILED 전이만 수행한다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class MockPaymentController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/mock-payment/approve")
    public ApiResponse<OrderQueryResponse> approve(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId,
            @RequestBody @Valid MockPaymentApproveRequest request
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.approveMockPayment(user, orderId, request));
    }

    @PostMapping("/{orderId}/mock-payment/fail")
    public ApiResponse<OrderQueryResponse> fail(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId,
            @RequestBody(required = false) MockPaymentFailRequest request
    ) {
        MockPaymentFailRequest body = request != null ? request : new MockPaymentFailRequest();
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.failMockPayment(user, orderId, body));
    }
}
