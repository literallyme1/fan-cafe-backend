package com.example.fan_cafe.user.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.interfaces.dto.ProfileRequest;
import com.example.fan_cafe.user.interfaces.dto.ProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<ProfileResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                               @Valid ProfileRequest profileRequest) {
        return ApiResponse.success(ApiResponseStatus.CREATED, userService.create(user.getId(), profileRequest));
    }
    @GetMapping
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, userService.get(user));
    }
    @DeleteMapping
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user){
        userService.delete(user.getId());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }




}
