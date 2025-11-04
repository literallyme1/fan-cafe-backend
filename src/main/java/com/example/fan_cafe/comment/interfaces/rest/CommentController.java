package com.example.fan_cafe.comment.interfaces.rest;

import com.example.fan_cafe.comment.application.CommentService;
import com.example.fan_cafe.comment.interfaces.dto.CommentRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentListResponse;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
//like base update
    private final CommentService commentService;

    @PostMapping
    public ApiResponse<CommentResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                               @RequestBody @Valid CommentRequest request) {
        return ApiResponse.success(ApiResponseStatus.CREATED, commentService.create(user, request));
    }

    @GetMapping("/{postId}")
    public ApiResponse<CommentListResponse> get(@PathVariable Long postId,
                                                @AuthenticationPrincipal(expression = "user") User user,
                                                @RequestParam(required = false) Cursor cursor,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, commentService.getComments(postId, user.getId(), cursor, size));
    }

    @GetMapping("{commentId}/replies")
    public ApiResponse<CommentListResponse> getReplies(@PathVariable Long commentId,
                                                       @AuthenticationPrincipal(expression = "user") User user,
                                                     @RequestParam(required = false) Cursor cursor,
                                                     @RequestParam(defaultValue = "10") int size){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, commentService.getReplies(commentId, user.getId(), cursor, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<CommentResponse> update(@AuthenticationPrincipal(expression = "user")User user,
                                               @PathVariable Long id,
                                               @RequestBody @Valid CommentRequest request){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, commentService.update(user, id, request));
    }

    @DeleteMapping("{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user,
                                    @PathVariable Long id) {
        commentService.delete(user, id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    //like
    @PostMapping("/{commentId}/like")
    public ApiResponse<Void> toggleLike(@AuthenticationPrincipal(expression = "user") User user,
                                        @PathVariable Long id){
        commentService.toggleLike(user, id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }


}
