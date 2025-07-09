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
    private boolean hasNext;

    public static MerchandiseListResponse of(Category category, List<MerchandiseResponse> merchandises, boolean hasNext) {
        return MerchandiseListResponse.builder()
                .category(category)
                .merchandises(merchandises)
                .hasNext(hasNext)
                .build();
    }
}
