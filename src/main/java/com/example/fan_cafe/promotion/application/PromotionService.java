package com.example.fan_cafe.promotion.application;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.example.fan_cafe.promotion.infrastructure.PromotionRepository;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionListResponse;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionRequest;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final S3Uploader s3Uploader;

    public ApiResponse<PromotionResponse> create(PromotionRequest request,
                                                 MultipartFile image){
        String imageUrl = image.isEmpty()? null : s3Uploader.upload(image, "promotion");
        Promotion promotion = this.create(request, imageUrl);
        return ApiResponse.success(ApiResponseStatus.CREATED, PromotionResponse.from(promotion));
    }

    @Transactional
    public Promotion create(PromotionRequest request, String imageUrl) {
        Promotion promotion = Promotion.of(request, imageUrl);
        return promotionRepository.save(promotion);
    }

    public ApiResponse<PromotionListResponse> get(int page, int size) {
        Pageable pageable = PageUtils.createPageRequest(page, size, "createdAt", "DESC");
        Slice<Promotion> promotions = promotionRepository.findSliceAll(pageable);
        List<PromotionResponse> promotionDtos = promotions.stream()
                .map(PromotionResponse::from)
                .toList();
        return ApiResponse.success(ApiResponseStatus.SUCCESS, PromotionListResponse.of(promotionDtos, promotions.hasNext()));
    }
}
