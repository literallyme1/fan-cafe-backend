package com.example.fan_cafe.merchandise_page.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MerchandisePageResponse {
    private List<PromotionSummaryDto> promotions;
    private List<MerchandiseSummaryDto> merchandises;
    private boolean promotionHasNext;
    private boolean merchandiseHasNext;

    public static MerchandisePageResponse of(List<PromotionSummaryDto> promotions,
                                             List<MerchandiseSummaryDto> merchandises,
                                             boolean promotionHasNext,
                                             boolean merchandiseHasNext){
        return MerchandisePageResponse.builder()
                .promotions(promotions)
                .merchandises(merchandises)
                .promotionHasNext(promotionHasNext)
                .merchandiseHasNext(merchandiseHasNext)
                .build();
    }

}
