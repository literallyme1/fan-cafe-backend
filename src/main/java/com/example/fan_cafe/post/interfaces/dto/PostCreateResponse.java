package com.example.fan_cafe.post.interfaces.dto;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PostCreateResponse {

    private final Long id;
    private final List<String> imageUrls;

    public static PostCreateResponse from (Long id,
                                           List<String> imageUrls) {
        return PostCreateResponse.builder()
                .id(id)
                .imageUrls(imageUrls)
                .build();
    }
}
