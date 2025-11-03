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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, userService.get(user));
    }
    @DeleteMapping
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user){
        userService.delete(user.getId());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal(expression = "user") User user,
                                               @RequestPart("profile") @Valid ProfileRequest request,
                                               @RequestPart("image")MultipartFile image) {

        ProfileResponse response = userService.update(user.getId(), request, image);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }





}
