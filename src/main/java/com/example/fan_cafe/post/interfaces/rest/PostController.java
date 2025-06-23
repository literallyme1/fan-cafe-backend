package com.example.fan_cafe.post.interfaces.rest;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/create")
    public void createPost(@RequestBody @Valid PostCreateRequest request) {
        return postService.createPost(request);
    }

}
