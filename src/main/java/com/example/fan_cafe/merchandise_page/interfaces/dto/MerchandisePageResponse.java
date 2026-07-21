package com.example.fan_cafe.merchandise_page.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionListResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MerchandisePageResponse {
    @Schema(description = "진행 중인 프로모션", example = "{\"promotions\":[],\"hasNext\":false}")
    private PromotionListResponse promotions;
    @Schema(description = "카테고리별 상품", example = "{\"merchandises\":[]}")
    private MerchandiseGroupedResponse merchandises;


    public static MerchandisePageResponse of(PromotionListResponse promotions,
                                             MerchandiseGroupedResponse merchandises){
        return MerchandisePageResponse.builder()
                .promotions(promotions)
                .merchandises(merchandises)
                .build();
    }

}
