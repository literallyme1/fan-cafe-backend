package com.example.fan_cafe.post.interfaces.rest;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.PostErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import com.example.fan_cafe.post.interfaces.dto.PostDto;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
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
    public ApiResponse<Void> create(@AuthenticationPrincipal User user,
                                    @RequestPart("post") @Valid PostCreateRequest request,
                                    @RequestPart("images") List<MultipartFile> images) {
        if(images == null || images.isEmpty()){
            throw new CustomException(PostErrorCode.NO_IMAGE_PROVIDED);
        }
            return postService.create(user, request, images);
    }

    //paged 10개씩
    @GetMapping
    public ApiResponse<PostResponse> get(@RequestParam(required = false) Long cursorId,
                                         @RequestParam(required = false) @DateTimeFormat (iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
                                         @RequestParam(defaultValue = "10") int size)
    {
        return postService.get(cursorId, cursorCreatedAt, size);
    }

//    @PutMapping("/{id}")
//    public ApiResponse<Void> update(@PathVariable Long id) {
//        return postService.update();
//    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return postService.delete(id);
    }


}
