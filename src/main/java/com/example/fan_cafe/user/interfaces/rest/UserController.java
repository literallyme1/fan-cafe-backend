package com.example.fan_cafe.user.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.interfaces.dto.ProfileRequest;
import com.example.fan_cafe.user.interfaces.dto.ProfileResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "회원", description = "회원 프로필 조회와 수정 및 탈퇴")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, userService.get(user));
    }
    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자 계정을 탈퇴 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResponse<Void> delete(@AuthenticationPrincipal(expression = "user") User user){
        userService.delete(user.getId());
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    @PutMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "닉네임과 소개 및 프로필 이미지를 수정함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "닉네임 중복")
    })
    public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal(expression = "user") User user,
                                               @RequestPart("profile") @Valid ProfileRequest request,
                                               @RequestPart(value = "image", required = false)MultipartFile image) {

        ProfileResponse response = userService.update(user.getId(), request, image);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "회원 프로필 조회", description = "회원 식별자로 공개 프로필을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ApiResponse<ProfileResponse> get(@PathVariable Long userId){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, userService.getProfile(userId));
    }





}
