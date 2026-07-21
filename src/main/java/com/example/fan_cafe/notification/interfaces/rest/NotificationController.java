package com.example.fan_cafe.notification.interfaces.rest;

import com.example.fan_cafe.notification.application.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "사용자 알림 기능 확장 엔드포인트")
public class NotificationController {

    private final NotificationService service;

}
