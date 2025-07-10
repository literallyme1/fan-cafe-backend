package com.example.fan_cafe.promotion.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.example.fan_cafe.promotion.exception.PromotionErrorCode;
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

    public PromotionResponse create(PromotionRequest request,
                                                 MultipartFile image){
        String imageUrl = (image != null && !image.isEmpty())? s3Uploader.upload(image, "promotion") : null;
        Promotion promotion = this.create(request, imageUrl);
        return PromotionResponse.from(promotion);
    }

    @Transactional
    public Promotion create(PromotionRequest request, String imageUrl) {
        Promotion promotion = Promotion.of(request, imageUrl);
        return promotionRepository.save(promotion);
    }

    public PromotionListResponse get(int page, int size) {
        Pageable pageable = PageUtils.createPageRequest(page, size, "createdAt", "DESC");
        Slice<Promotion> promotions = promotionRepository.findSliceBy(pageable);
        List<PromotionResponse> promotionDtos = promotions.stream()
                .map(PromotionResponse::from)
                .toList();
        return PromotionListResponse.of(promotionDtos, promotions.hasNext());
    }

    public PromotionResponse update(Long id, PromotionRequest request, MultipartFile image){

        Promotion promotion = findByIdOrThrow(id);
        String newImageUrl = resolveImageUrl(promotion, request, image);
        Promotion newPromotion = update(id, request, newImageUrl);

        return PromotionResponse.from(newPromotion);
    }

    @Transactional
    public Promotion update(Long id, PromotionRequest request, String newImageUrl){
        Promotion promotion = findByIdOrThrow(id);
        promotion.update(request, newImageUrl);
        return promotion;
    }

    @Transactional
    public void delete(Long id){
        Promotion promotion = findByIdOrThrow(id);
        promotion.delete();
    }

    private String resolveImageUrl(Promotion oldPromotion, PromotionRequest request, MultipartFile image){
        String imageUrl = request.getImageUrl();

        if(request.isDeleteImage() && oldPromotion.getImageUrl() != null){
            s3Uploader.delete(s3Uploader.extractFileKey(oldPromotion.getImageUrl()));
        }

        if(image != null && !image.isEmpty()){
            imageUrl = s3Uploader.upload(image, "promotion");
        }
        return imageUrl;
    }


    private Promotion findByIdOrThrow(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new CustomException(PromotionErrorCode.PROMOTION_NOT_FOUND));
    }
}
