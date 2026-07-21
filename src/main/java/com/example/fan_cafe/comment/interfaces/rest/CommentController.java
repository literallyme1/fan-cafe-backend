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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "댓글", description = "댓글과 답글 및 좋아요 관리")
public class CommentController {
//like base update
    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "댓글 작성", description = "게시글에 댓글 또는 답글을 작성함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 깊이 또는 입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ApiResponse<CommentResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                               @RequestBody @Valid CommentRequest request) {
        return ApiResponse.success(ApiResponseStatus.CREATED, commentService.create(user, request));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "댓글 목록 조회", description = "게시글의 최상위 댓글을 커서 기준으로 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ApiResponse<CommentListResponse> get(@PathVariable Long postId,
                                                @AuthenticationPrincipal(expression = "user") User user,
                                                @RequestParam(required = false) Cursor cursor,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, commentService.getComments(postId, user.getId(), cursor, size));
    }

    @GetMapping("{commentId}/replies")
    @Operation(summary = "답글 목록 조회", description = "댓글에 작성된 답글을 커서 기준으로 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ApiResponse<CommentListResponse> getReplies(@PathVariable Long commentId,
                                                       @AuthenticationPrincipal(expression = "user") User user,
                                                     @RequestParam(required = false) Cursor cursor,
                                                     @RequestParam(defaultValue = "10") int size){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, commentService.getReplies(commentId, user.getId(), cursor, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "댓글 수정", description = "작성자가 댓글 내용을 수정함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ApiResponse<CommentResponse> update(@AuthenticationPrincipal(expression = "user")User user,
                                               @PathVariable Long id,
                                               @RequestBody @Valid CommentRequest request){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, commentService.update(user, id, request));
    }

    @DeleteMapping("{id}")
    @Operation(summary = "댓글 삭제", description = "작성자가 댓글을 삭제 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user,
                                    @PathVariable Long id) {
        commentService.delete(user, id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    //like
    @PostMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요", description = "댓글 좋아요 상태를 추가하거나 취소함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ApiResponse<Void> toggleLike(@AuthenticationPrincipal(expression = "user") User user,
                                        @PathVariable Long id){
        commentService.toggleLike(user, id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }


}
