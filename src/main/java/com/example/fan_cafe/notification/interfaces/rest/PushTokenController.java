package com.example.fan_cafe.notification.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.notification.application.PushTokenService;
import com.example.fan_cafe.notification.interfaces.dto.PushTokenRequest;
import com.example.fan_cafe.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "푸시 알림", description = "사용자 기기 푸시 토큰 관리")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @PostMapping
    @Operation(summary = "푸시 토큰 등록", description = "사용자 기기의 푸시 토큰과 플랫폼을 등록하거나 갱신함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "토큰 또는 플랫폼 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<Void> register(@AuthenticationPrincipal(expression = "user") User user,
                                         @RequestBody PushTokenRequest request) {
        pushTokenService.registerOrUpdate(user.getId(),
                request.token(),
                request.platform());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
