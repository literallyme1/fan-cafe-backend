package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateResponse;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 로그인 사용자 기준으로 주문 생성 요청을 서비스에 위임한다.
    @PostMapping
    public ApiResponse<OrderCreateResponse> create(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        return ApiResponse.success(ApiResponseStatus.CREATED, orderService.create(user, request));
    }

    // 로그인 사용자의 주문 단건을 조회한다.
    @GetMapping("/{orderId}")
    public ApiResponse<OrderQueryResponse> get(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.get(user, orderId));
    }

    // 로그인 사용자의 주문 목록을 조회한다.
    @GetMapping
    public ApiResponse<List<OrderQueryResponse>> getMyOrders(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.getMyOrders(user));
    }

    // 로그인 사용자의 주문을 취소한다.
    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderQueryResponse> cancel(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.cancel(user, orderId));
    }
}

