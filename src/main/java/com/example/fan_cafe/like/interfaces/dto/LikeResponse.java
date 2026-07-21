package com.example.fan_cafe.like.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LikeResponse {

    @Schema(description = "게시글 식별자", example = "3201")
    private Long postId;
    @Schema(description = "내 좋아요 여부", example = "true")
    private boolean liked;
    @Schema(description = "좋아요 수", example = "215")
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
