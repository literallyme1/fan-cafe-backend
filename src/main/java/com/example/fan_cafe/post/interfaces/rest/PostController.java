package com.example.fan_cafe.post.interfaces.rest;


import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.post.exception.PostErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.interfaces.dto.*;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ApiResponse<PostResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                                  @RequestPart("post") @Valid PostCreateRequest request,
                                                  @RequestPart("images") List<MultipartFile> images) {
        if(images == null || images.isEmpty()){
            throw new CustomException(PostErrorCode.NO_IMAGE_PROVIDED);
        }

        PostResponse response = postService.create(user, request, images);
        return ApiResponse.success(ApiResponseStatus.CREATED, response);
    }

    //paged 10개씩
    @GetMapping
    public ApiResponse<PostListResponse> get(@RequestParam(required = false) Cursor cursor,
                                             @RequestParam(defaultValue = "10") int size,
                                             @AuthenticationPrincipal(expression = "user") User user)
    {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, postService.get(cursor, size, user.getId()));
    }

//    @GetMapping("/new")
//    public ApiResponse<PostListResponse> getNewPosts(@RequestParam(required = false) Cursor cursor,
//                                                     @RequestParam(defaultValue = "10") int size,
//                                                     @AuthenticationPrincipal(expression = "user") User user)
//    {
//        return ApiResponse.success(ApiResponseStatus.SUCCESS, postService.getNewPosts(cursor, size, user.getId()));
//    }

    @PutMapping("/{id}")
    public ApiResponse<PostResponse> update(@AuthenticationPrincipal(expression = "user") User user,
                                                  @PathVariable Long id,
                                                  @RequestPart("post") @Valid PostUpdateRequest request,
                                                  @RequestPart("images") List<MultipartFile> images)
    {
        PostResponse response = postService.update(user, id, request, images);
        return ApiResponse.success(ApiResponseStatus.SUCCESS,response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user,
                                    @PathVariable Long id)
    {
        postService.delete(user, id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    //like

}
