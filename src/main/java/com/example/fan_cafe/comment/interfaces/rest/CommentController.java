package com.example.fan_cafe.comment.interfaces.rest;

import com.example.fan_cafe.comment.application.CommentService;
import com.example.fan_cafe.comment.interfaces.dto.CommentCreateRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ApiResponse<CommentResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                               @RequestBody @Valid CommentCreateRequest request) {
        return commentService.create(user, request);
    }
}
