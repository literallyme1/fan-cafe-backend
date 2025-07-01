package com.example.fan_cafe.post.interfaces.dto;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PostUpdateResponse {

    private final List<String> imageUrls;

    public static PostUpdateResponse from(List<String> imageUrls) {
        return PostUpdateResponse.builder()
                .imageUrls(imageUrls)
                .build();
    }
}
