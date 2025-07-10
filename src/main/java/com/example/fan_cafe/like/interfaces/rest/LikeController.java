package com.example.fan_cafe.like.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.interfaces.dto.LikeListResponse;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{postId}")
    public ApiResponse<LikeResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                            @PathVariable Long postId){
        return ApiResponse.success(ApiResponseStatus.CREATED, likeService.create(user, postId));
    }

    @GetMapping
    public ApiResponse<LikeListResponse> get(@AuthenticationPrincipal(expression = "user")User user){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, likeService.get(user));
    }

}
