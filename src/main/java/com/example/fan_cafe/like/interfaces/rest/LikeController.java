package com.example.fan_cafe.like.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.interfaces.dto.LikeListResponse;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.user.domain.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Tag(name = "좋아요", description = "좋아요 기능 확장 엔드포인트")
public class LikeController {

    private final LikeService likeService;



}
