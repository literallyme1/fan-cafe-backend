package com.example.fan_cafe.merchandise.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MerchandiseGroupedResponse {

    private List<MerchandiseListResponse> merchandises;
    private boolean hasNext;

    public static MerchandiseGroupedResponse of(List<MerchandiseListResponse> merchandises, boolean hasNext){
        return MerchandiseGroupedResponse.builder()
                .merchandises(merchandises)
                .hasNext(hasNext)
                .build();
    }
}
