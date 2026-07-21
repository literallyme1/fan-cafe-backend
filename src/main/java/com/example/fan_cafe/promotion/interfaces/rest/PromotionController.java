package com.example.fan_cafe.promotion.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.promotion.application.PromotionService;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionListResponse;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionRequest;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
@Tag(name = "프로모션", description = "상품 프로모션 등록과 조회")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @Operation(summary = "프로모션 등록", description = "프로모션 정보와 이미지를 등록함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "프로모션 정보 오류")
    })
    public ApiResponse<PromotionResponse> create(@RequestPart("promotion") @Valid PromotionRequest request,
                                                 @RequestPart(value = "image", required = false)MultipartFile image) {
        PromotionResponse response = promotionService.create(request, image);
        return ApiResponse.success(ApiResponseStatus.CREATED, response);
    }

    @GetMapping
    @Operation(summary = "프로모션 조회", description = "진행 중인 프로모션 목록을 페이지 단위로 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지 값 오류")
    })
    public ApiResponse<PromotionListResponse> get(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size){
        PromotionListResponse response = promotionService.get(page, size);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "프로모션 수정", description = "프로모션 정보와 이미지를 수정함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "프로모션 정보 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로모션 없음")
    })
    public ApiResponse<PromotionResponse> update(@PathVariable Long id,
                                                 @RequestPart("promotion") @Valid PromotionRequest request,
                                                 @RequestPart(value = "image", required = false) MultipartFile image){

        PromotionResponse response = promotionService.update(id, request, image);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "프로모션 삭제", description = "프로모션을 삭제 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로모션 없음")
    })
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
