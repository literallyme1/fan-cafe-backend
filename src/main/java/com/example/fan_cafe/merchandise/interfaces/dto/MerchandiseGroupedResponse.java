package com.example.fan_cafe.merchandise.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MerchandiseGroupedResponse {

    @Schema(description = "카테고리별 상품 목록", example = "[{\"category\":\"FASHION\",\"merchandises\":[],\"hasNext\":false}]")
    private List<MerchandiseListResponse> merchandises;

    public static MerchandiseGroupedResponse of(List<MerchandiseListResponse> merchandises){
        return MerchandiseGroupedResponse.builder()
                .merchandises(merchandises)
                .build();
    }
}
