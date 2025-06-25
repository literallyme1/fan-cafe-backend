package com.example.fan_cafe.post.interfaces.rest;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import com.example.fan_cafe.post.interfaces.dto.PostDto;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
