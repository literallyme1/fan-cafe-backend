package com.example.fan_cafe.like.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class LikeListResponse {

    @Schema(description = "좋아요 목록", example = "[{\"postId\":3201,\"liked\":true,\"likeCount\":215}]")
    private List<LikeResponse> likes;

    public static LikeListResponse from(List<LikeResponse> likes){
        return LikeListResponse.builder()
                .likes(likes)
                .build();
    }
}
