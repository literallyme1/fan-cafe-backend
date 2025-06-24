package com.example.fan_cafe.post.application;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.util.SecurityUtil;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public ApiResponse<Void> create(User user, PostCreateRequest request) {
        Post post = request.toEntity(user);
        postRepository.save(post);
        return ApiResponse.success(ApiResponseStatus.CREATED);
    }

    public ApiResponse<List<PostResponse>> get() {

    }

    public ApiResponse<Void> update() {

    }

    public ApiResponse<Void> delete(Long id) {
        postRepository.deleteById(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
