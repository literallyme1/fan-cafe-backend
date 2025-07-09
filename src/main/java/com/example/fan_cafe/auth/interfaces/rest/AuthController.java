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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserInfoResponse> register(@RequestBody @Valid RegisterRequest reqeust) {
        UserInfoResponse response =  authService.register(reqeust, Role.USER);
        return ApiResponse.success(ApiResponseStatus.CREATED, response);

    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response =  authService.login(request);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal(expression = "user") User user){
        authService.logout(user.getId());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    @PostMapping("/refresh")
    public ApiResponse<JwtTokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request){
        JwtTokenResponse response = authService.reissueAccessToken(request.getRefreshToken());
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }
}
