package com.example.fan_cafe.like.interfaces.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LikeResponse {

    private Long postId;
    private boolean liked;
    private int likeCount;

    @QueryProjection
    public LikeResponse(Long postId, boolean liked, int likeCount) {
        this.postId = postId;
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public static LikeResponse from(Long postId, boolean liked, int likeCount){
        return LikeResponse.builder()
                .postId(postId)
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    public static LikeResponse from(int likeCount){
        return LikeResponse.builder()
                .liked(false)
                .likeCount(likeCount)
                .build();
    }
}
