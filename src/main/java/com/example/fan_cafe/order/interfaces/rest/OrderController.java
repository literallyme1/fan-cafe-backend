package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}

