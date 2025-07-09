package com.example.fan_cafe.merchandise.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MerchandiseGroupedResponse {

    private List<MerchandiseListResponse> merchandises;

    public static MerchandiseGroupedResponse of(List<MerchandiseListResponse> merchandises){
        return MerchandiseGroupedResponse.builder()
                .merchandises(merchandises)
                .build();
    }
}
