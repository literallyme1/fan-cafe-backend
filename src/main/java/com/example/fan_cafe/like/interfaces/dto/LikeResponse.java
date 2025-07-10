package com.example.fan_cafe.like.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LikeResponse {

    private Long postId;
    private boolean liked;
    private int likeCount;

    public static LikeResponse from(Long postId, boolean liked, int likeCount){
        return LikeResponse.builder()
                .postId(postId)
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
