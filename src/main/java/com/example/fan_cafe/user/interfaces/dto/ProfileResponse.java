package com.example.fan_cafe.user.interfaces.dto;

import com.example.fan_cafe.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProfileResponse {

    @Schema(description = "회원 식별자", example = "101")
    private final Long id;
    @Schema(description = "회원 이메일", example = "fan@example.com")
    private final String email;
    @Schema(description = "회원 닉네임", example = "별빛팬")
    private final String nickname;
    @Schema(description = "회원 권한", example = "USER")
    private final String role;
    @Schema(description = "프로필 소개", example = "콘서트와 신보 소식을 기록합니다.")
    private final String introduction;
    @Schema(description = "프로필 이미지", example = "https://cdn.fancafe.kr/users/101/avatar.jpg")
    private final String avatarUrl;
    @Schema(description = "팔로워 수", example = "128")
    private final int followerCount;
    @Schema(description = "팔로잉 수", example = "42")
    private final int followingCount;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().toString())
                .introduction(user.getIntroduction())
                .avatarUrl(user.getAvatarUrl())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .build();
    }
}
