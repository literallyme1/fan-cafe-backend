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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "게시글", description = "게시글 작성과 조회 및 반응 관리")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "게시글 작성", description = "게시글 내용과 이미지를 등록함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 또는 이미지 오류")
    })
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
    @Operation(summary = "게시글 목록 조회", description = "커서 기준으로 게시글 목록을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "커서 또는 크기 오류")
    })
    public ApiResponse<PostListResponse> get(@RequestParam(required = false) Cursor cursor,
                                             @RequestParam(defaultValue = "10") int size,
                                             @AuthenticationPrincipal(expression = "user") User user)
    {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, postService.get(cursor, size, user.getId()));
    }

    @GetMapping("/new")
    @Operation(summary = "새 게시글 조회", description = "기준 시점 이후 등록된 게시글을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "커서 또는 크기 오류")
    })
    public ApiResponse<PostListResponse> getNewPosts(@RequestParam(required = false) Cursor cursor,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @AuthenticationPrincipal(expression = "user") User user)
    {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, postService.getNewPosts(cursor, size, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "게시글 수정", description = "작성자가 게시글 내용과 이미지를 수정함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ApiResponse<PostResponse> update(@AuthenticationPrincipal(expression = "user") User user,
                                                  @PathVariable Long id,
                                                  @RequestPart("post") @Valid PostUpdateRequest request,
                                                  @RequestPart("images") List<MultipartFile> images)
    {
        PostResponse response = postService.update(user, id, request, images);
        return ApiResponse.success(ApiResponseStatus.SUCCESS,response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제", description = "작성자가 게시글을 삭제 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user,
                                    @PathVariable Long id)
    {
        postService.delete(user, id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    //like
    @PostMapping("/{postId}/like")
    @Operation(summary = "게시글 좋아요", description = "게시글 좋아요 상태를 추가하거나 취소함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ApiResponse<Void> toggleLike(@AuthenticationPrincipal(expression = "user") User user,
                                        @PathVariable("postId") Long postId){
        postService.toggleLike(user, postId);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

}
