package com.example.fan_cafe.order.saga.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.order.saga.application.SagaAdminAction;
import com.example.fan_cafe.order.saga.application.SagaAdminService;
import com.example.fan_cafe.order.saga.interfaces.dto.SagaAdminResponse;
import com.example.fan_cafe.order.saga.interfaces.dto.SagaManualActionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sagas")
@RequiredArgsConstructor
@Tag(name = "Saga 운영", description = "수동 reconciliation 대상 조회와 상태 기반 운영 액션")
public class SagaAdminController {
    private final SagaAdminService sagaAdminService;

    @GetMapping("/reconciliation-required")
    @Operation(summary = "Saga 수동 reconciliation 목록")
    public ApiResponse<List<SagaAdminResponse>> getReconciliationRequiredSagas() {
        return ApiResponse.success(
                ApiResponseStatus.SUCCESS, sagaAdminService.getReconciliationRequiredSagas());
    }

    @GetMapping("/{sagaId}")
    @Operation(summary = "Saga 운영 상세")
    public ApiResponse<SagaAdminResponse> getSaga(@PathVariable UUID sagaId) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, sagaAdminService.getSaga(sagaId));
    }

    @PostMapping("/{sagaId}/actions/{action}")
    @Operation(summary = "Saga 상태 기반 수동 액션 요청")
    public ApiResponse<SagaManualActionResponse> requestAction(
            @PathVariable UUID sagaId,
            @PathVariable SagaAdminAction action
    ) {
        return ApiResponse.success(
                ApiResponseStatus.SUCCESS, sagaAdminService.requestAction(sagaId, action));
    }
}
