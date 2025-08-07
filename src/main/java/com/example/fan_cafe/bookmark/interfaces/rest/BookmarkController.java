package com.example.fan_cafe.bookmark.interfaces.rest;

import com.example.fan_cafe.bookmark.application.BookmarkService;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkListResponse;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkResponse;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;

import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{postId}")
    public ApiResponse<BookmarkResponse> add(@AuthenticationPrincipal(expression = "user") User user,
                                             @PathVariable Long postId){
        return ApiResponse.success(ApiResponseStatus.CREATED, bookmarkService.add(user, postId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<BookmarkResponse> remove(@AuthenticationPrincipal(expression = "user")User user,
                                            @PathVariable Long postId){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, bookmarkService.remove(user, postId));
    }

    @GetMapping
    public ApiResponse<BookmarkListResponse> get(@RequestParam(defaultValue = "0")int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @AuthenticationPrincipal(expression = "user")User user) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, bookmarkService.get(page, size, user));
    }
}
