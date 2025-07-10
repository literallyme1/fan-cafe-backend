package com.example.fan_cafe.promotion.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.promotion.application.PromotionService;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionListResponse;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionRequest;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ApiResponse<PromotionResponse> create(@RequestPart("promotion") @Valid PromotionRequest request,
                                                 @RequestPart(value = "image", required = false)MultipartFile image) {
        PromotionResponse response = promotionService.create(request, image);
        return ApiResponse.success(ApiResponseStatus.CREATED, response);
    }

    @GetMapping
    public ApiResponse<PromotionListResponse> get(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size){
        PromotionListResponse response = promotionService.get(page, size);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id,
                                                 @RequestPart("promotion") @Valid PromotionRequest request,
                                                 @RequestPart(value = "image", required = false) MultipartFile image){

        PromotionResponse response = promotionService.update(id, request, image);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
