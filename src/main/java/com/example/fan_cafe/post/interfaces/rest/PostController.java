package com.example.fan_cafe.post.interfaces.rest;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ApiResponse<Void> create(@AuthenticationPrincipal User user,
            @RequestBody @Valid PostCreateRequest request) {
        return postService.create(user, request);
    }

    //paged 10개씩
    @GetMapping
    public ApiResponse<List<PostResponse>> get(){
        return postService.get();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id) {
        return postService.update();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return postService.delete(id);
    }


}
