package com.example.fan_cafe.global.test;

import lombok.RequiredArgsConstructor;
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
public class FaultInjectionController {

    private final FaultStatus faultStatus;

    @PostMapping("/notification-block")
    public ResponseEntity<Map<String, Object>> setNotificationBlock(@RequestParam boolean enable) {
        faultStatus.setNotificationBlocked(enable);
        return ResponseEntity.ok(Map.of(
                "notificationBlocked", faultStatus.isNotificationBlocked()
        ));
    }
}
