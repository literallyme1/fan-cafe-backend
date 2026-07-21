package com.example.fan_cafe.follow.interfaces.rest;


import com.example.fan_cafe.follow.application.FollowService;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowingListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "팔로우", description = "회원 팔로우와 관계 목록 조회")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{targetId}/follow")
    @Operation(summary = "회원 팔로우", description = "대상 회원을 팔로우함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "팔로우 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 팔로우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 팔로우함")
    })
    public void follow(@PathVariable Long targetId,
                       @AuthenticationPrincipal(expression = "user") User user) {
        followService.follow(user.getId(), targetId);
    }

    @DeleteMapping("/{targetId}/follow")
    @Operation(summary = "팔로우 취소", description = "대상 회원과의 팔로우 관계를 삭제함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팔로우 관계 없음")
    })
    public void unfollow(@PathVariable Long targetId,
                         @AuthenticationPrincipal(expression = "user") User user) {
        followService.unfollow(user.getId(), targetId);
    }


    @GetMapping("/{userId}/following")
    @Operation(summary = "팔로잉 목록 조회", description = "회원이 팔로우하는 사용자 목록을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<FollowingListResponse> getFollowing(@PathVariable("userId") Long userId,
                                    @RequestParam(required = false)Cursor cursor,
                                    @RequestParam(defaultValue = "20") int size){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, followService.getFollowingList(userId, cursor, size));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "팔로워 목록 조회", description = "회원을 팔로우하는 사용자 목록을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<FollowerListResponse> getFollowers(@PathVariable("userId") Long userId,
                                              @RequestParam(required = false)Cursor cursor,
                                              @RequestParam(defaultValue = "20") int size){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, followService.getFollowerList(userId, cursor, size));
    }
}
