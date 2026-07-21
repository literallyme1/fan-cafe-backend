package com.example.fan_cafe.order.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateResponse;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "주문", description = "상품 주문 생성과 조회 및 취소")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성 — 초기 상태 PAYMENT_PENDING (Outbox는 Mock 결제 승인 후 저장).
    @PostMapping
    @Operation(summary = "주문 생성", description = "상품 재고를 확보하고 결제 대기 상태의 주문을 생성함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "수량 또는 요청 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "재고 부족")
    })
    public ApiResponse<OrderCreateResponse> create(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        return ApiResponse.success(ApiResponseStatus.CREATED, orderService.create(user, request));
    }

    // 로그인 사용자의 주문 단건을 조회한다.
    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "로그인한 사용자의 주문과 주문 상품을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "조회 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음")
    })
    public ApiResponse<OrderQueryResponse> get(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.get(user, orderId));
    }

    // 로그인 사용자의 주문 목록을 조회한다.
    @GetMapping
    @Operation(summary = "내 주문 조회", description = "로그인한 사용자의 전체 주문 목록을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<List<OrderQueryResponse>> getMyOrders(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.getMyOrders(user));
    }

    // 로그인 사용자의 주문을 취소한다.
    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "결제 대기 주문을 취소하고 확보한 재고를 복구함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "취소 불가 상태")
    })
    public ApiResponse<OrderQueryResponse> cancel(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, orderService.cancel(user, orderId));
    }
}
