package com.example.fan_cafe.global.test;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 테스트 환경에서만 노출되는 장애 주입 API.
 */
@RestController
@RequestMapping("/admin/simulate")
@Profile({"test", "ci", "awstest"})
@RequiredArgsConstructor
@Tag(name = "장애 주입", description = "테스트 환경 알림 장애 상태 제어")
public class FaultInjectionController {

    private final FaultStatus faultStatus;

    @PostMapping("/notification-block")
    @Operation(summary = "알림 장애 전환", description = "Outbox 알림 전송 차단 상태를 활성화하거나 해제함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "활성화 값 오류")
    })
    public ResponseEntity<Map<String, Object>> setNotificationBlock(@RequestParam boolean enable) {
        faultStatus.setNotificationBlocked(enable);
        return ResponseEntity.ok(Map.of(
                "notificationBlocked", faultStatus.isNotificationBlocked()
        ));
    }
}
