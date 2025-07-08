package com.example.fan_cafe.merchandise.interfaces.dto;

import com.example.fan_cafe.merchandise.domain.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MerchandiseListResponse {
    private Category category;
    private List<MerchandiseResponse> merchandises;

    public static MerchandiseListResponse of(Category category, List<MerchandiseResponse> merchandises) {
        return MerchandiseListResponse.builder()
                .category(category)
                .merchandises(merchandises)
                .build();
    }
}
