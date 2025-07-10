package com.example.fan_cafe.merchandise_page.application;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.merchandise.application.MerchandiseService;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.merchandise_page.interfaces.dto.MerchandisePageResponse;
import com.example.fan_cafe.merchandise_page.interfaces.dto.MerchandiseSummaryDto;
import com.example.fan_cafe.merchandise_page.interfaces.dto.PromotionSummaryDto;
import com.example.fan_cafe.promotion.application.PromotionService;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchandisePageService {

    private final MerchandiseService merchandiseService;
    private final PromotionService promotionService;

    public MerchandisePageResponse get(int size) {

        MerchandiseGroupedResponse merchandiseResponse = merchandiseService.get(0, size, null);
        PromotionListResponse promotionResponse = promotionService.get(0, size);

        return MerchandisePageResponse.of(promotionResponse, merchandiseResponse);
    }
}
