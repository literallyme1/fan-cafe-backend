package com.example.fan_cafe.like.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class LikeListResponse {

    private List<LikeResponse> likes;

    public static LikeListResponse from(List<LikeResponse> likes){
        return LikeListResponse.builder()
                .likes(likes)
                .build();
    }
}
