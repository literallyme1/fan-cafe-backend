package com.example.fan_cafe.outbox.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.outbox.application.OutboxAdminService;
import com.example.fan_cafe.outbox.interfaces.dto.OutboxEventAdminResponse;
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
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    @GetMapping("/manual-required")
    public ApiResponse<List<OutboxEventAdminResponse>> getManualRequiredEvents() {
        List<OutboxEventAdminResponse> data = outboxAdminService.getManualRequiredEvents().stream()
                .map(OutboxEventAdminResponse::from)
                .toList();
        return ApiResponse.success(ApiResponseStatus.SUCCESS, data);
    }

    @GetMapping("/{id}")
    public ApiResponse<OutboxEventAdminResponse> getOutboxEvent(@PathVariable Long id) {
        OutboxEventAdminResponse data = OutboxEventAdminResponse.from(outboxAdminService.getOutboxEvent(id));
        return ApiResponse.success(ApiResponseStatus.SUCCESS, data);
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        outboxAdminService.requestManualRetry(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
