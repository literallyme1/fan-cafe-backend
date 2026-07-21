package com.example.fan_cafe.outbox.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.outbox.application.OutboxAdminService;
import com.example.fan_cafe.outbox.interfaces.dto.OutboxEventAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/outbox-events")
@RequiredArgsConstructor
@Tag(name = "Outbox 운영", description = "실패 Outbox 이벤트 조회와 수동 재처리")
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    @GetMapping("/manual-required")
    @Operation(summary = "수동 처리 목록", description = "재시도 한도를 초과한 Outbox 이벤트를 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    public ApiResponse<List<OutboxEventAdminResponse>> getManualRequiredEvents() {
        List<OutboxEventAdminResponse> data = outboxAdminService.getManualRequiredEvents().stream()
                .map(OutboxEventAdminResponse::from)
                .toList();
        return ApiResponse.success(ApiResponseStatus.SUCCESS, data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Outbox 상세", description = "Outbox 이벤트의 상태와 오류 및 재시도 정보를 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이벤트 없음")
    })
    public ApiResponse<OutboxEventAdminResponse> getOutboxEvent(@PathVariable Long id) {
        OutboxEventAdminResponse data = OutboxEventAdminResponse.from(outboxAdminService.getOutboxEvent(id));
        return ApiResponse.success(ApiResponseStatus.SUCCESS, data);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Outbox 재처리", description = "수동 처리 대상 이벤트를 재발행 대기 상태로 변경함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재처리 요청 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "재처리 불가 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이벤트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 재처리 중")
    })
    public ApiResponse<Void> retry(@PathVariable Long id) {
        outboxAdminService.requestManualRetry(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
