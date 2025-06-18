package com.example.fan_cafe.user.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.interfaces.dto.LoginRequest;
import com.example.fan_cafe.user.interfaces.dto.LoginResponse;
import com.example.fan_cafe.user.interfaces.dto.RegisterRequest;
import com.example.fan_cafe.user.interfaces.dto.UserInfoResponse;
import com.example.fan_cafe.user.application.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        return authService.register(reqeust, Role.USER);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

}
