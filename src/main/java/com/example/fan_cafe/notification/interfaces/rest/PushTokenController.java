package com.example.fan_cafe.notification.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.notification.application.PushTokenService;
import com.example.fan_cafe.notification.interfaces.dto.PushTokenRequest;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/push-tokens")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @PostMapping
    public ApiResponse<Void> register(@AuthenticationPrincipal(expression = "user") User user,
                                         @RequestBody PushTokenRequest request) {
        pushTokenService.registerOrUpdate(user.getId(),
                request.token(),
                request.platform());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
