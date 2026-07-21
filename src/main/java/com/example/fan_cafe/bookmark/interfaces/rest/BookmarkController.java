package com.example.fan_cafe.bookmark.interfaces.rest;

import com.example.fan_cafe.bookmark.application.BookmarkService;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkListResponse;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkResponse;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;

import com.example.fan_cafe.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
@Tag(name = "북마크", description = "게시글 북마크 등록과 조회")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{postId}")
    @Operation(summary = "북마크 등록", description = "게시글을 내 북마크에 등록함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 북마크")
    })
    public ApiResponse<BookmarkResponse> add(@AuthenticationPrincipal(expression = "user") User user,
                                             @PathVariable Long postId){
        return ApiResponse.success(ApiResponseStatus.CREATED, bookmarkService.add(user, postId));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "북마크 삭제", description = "게시글을 내 북마크에서 삭제함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "북마크 없음")
    })
    public ApiResponse<BookmarkResponse> remove(@AuthenticationPrincipal(expression = "user")User user,
                                            @PathVariable Long postId){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, bookmarkService.remove(user, postId));
    }

    @GetMapping
    @Operation(summary = "북마크 목록 조회", description = "내 북마크 목록을 페이지 단위로 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지 값 오류")
    })
    public ApiResponse<BookmarkListResponse> get(@RequestParam(defaultValue = "0")int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @AuthenticationPrincipal(expression = "user")User user) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, bookmarkService.get(page, size, user));
    }
}
