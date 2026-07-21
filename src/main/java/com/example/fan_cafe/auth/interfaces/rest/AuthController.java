package com.example.fan_cafe.auth.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.security.RefreshTokenRequest;
import com.example.fan_cafe.global.security.JwtTokenResponse;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.auth.interfaces.dto.LoginRequest;
import com.example.fan_cafe.auth.interfaces.dto.LoginResponse;
import com.example.fan_cafe.auth.interfaces.dto.RegisterRequest;
import com.example.fan_cafe.auth.interfaces.dto.UserInfoResponse;
import com.example.fan_cafe.auth.application.AuthService;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입과 로그인 및 토큰 관리")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "회원가입", description = "이메일과 사용자 정보를 등록해 일반 회원 계정을 생성함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    public ApiResponse<UserInfoResponse> register(@RequestBody @Valid RegisterRequest request) {
        UserInfoResponse response =  authService.register(request, Role.USER);
        return ApiResponse.success(ApiResponseStatus.CREATED, response);

    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 확인하고 인증 토큰을 발급함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response =  authService.login(request);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자의 리프레시 토큰을 폐기함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<Void> logout(@AuthenticationPrincipal(expression = "user") User user){
        authService.logout(user.getId());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "토큰 재발급", description = "유효한 리프레시 토큰으로 액세스 토큰을 재발급함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "토큰 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ApiResponse<JwtTokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request){
        JwtTokenResponse response = authService.reissueAccessToken(request.getRefreshToken());
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }
}
