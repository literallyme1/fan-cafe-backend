package com.example.fan_cafe.user.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.interfaces.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<UserResponse> get(@AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, userService.get(user));
    }
    @DeleteMapping
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user){
        userService.delete(user.getId());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }




}
