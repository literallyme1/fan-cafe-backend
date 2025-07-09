package com.example.fan_cafe.merchandise_page.interfaces.dto;

import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionListResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MerchandisePageResponse {
    private PromotionListResponse promotions;
    private MerchandiseGroupedResponse merchandises;


    public static MerchandisePageResponse of(PromotionListResponse promotions,
                                             MerchandiseGroupedResponse merchandises){
        return MerchandisePageResponse.builder()
                .promotions(promotions)
                .merchandises(merchandises)
                .build();
    }

}
