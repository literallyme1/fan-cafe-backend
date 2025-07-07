package com.example.fan_cafe.merchandise.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MerchandiseListResponse {

    private List<MerchandiseResponse> merchandises;

    public static MerchandiseListResponse of(List<MerchandiseResponse> merchandises) {
        return MerchandiseListResponse.builder()
                .merchandises(merchandises)
                .build();
    }
}
