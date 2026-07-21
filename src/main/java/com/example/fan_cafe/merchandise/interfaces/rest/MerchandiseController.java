package com.example.fan_cafe.merchandise.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.merchandise.application.MerchandiseService;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/merchandises")
@RequiredArgsConstructor
@Tag(name = "상품", description = "팬 상품 등록과 조회 및 재고 관리")
public class MerchandiseController {

    private final MerchandiseService merchandiseService;

    @PostMapping
    @Operation(summary = "상품 등록", description = "상품 정보와 대표 이미지를 등록함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "상품 정보 오류")
    })
    public ApiResponse<MerchandiseResponse> create(@RequestPart("merchandise") @Valid MerchandiseRequest request,
                                                   @RequestPart(value = "image", required = false)MultipartFile image) {
        MerchandiseResponse response =  merchandiseService.create(request, image);
        return  ApiResponse.success(ApiResponseStatus.CREATED, response);
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "카테고리별 상품 목록을 페이지 단위로 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지 또는 카테고리 오류")
    })
    public ApiResponse<MerchandiseGroupedResponse> get(@RequestParam(defaultValue = "0")int page,
                                                       @RequestParam(required = false)Category category,
                                                       @RequestParam(defaultValue = "10") int size) {
        MerchandiseGroupedResponse response = merchandiseService.get(page, size, category);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 상세 조회", description = "상품 식별자로 상세 정보를 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음")
    })
    public ApiResponse<MerchandiseResponse> get(@PathVariable Long id) {
        MerchandiseResponse response = merchandiseService.getDetail(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정", description = "상품 정보와 대표 이미지를 수정함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "상품 정보 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음")
    })
    public ApiResponse<MerchandiseResponse> update(@PathVariable Long id,
                                                   @RequestPart("merchandise") MerchandiseRequest request,
                                                   @RequestPart(value = "image", required = false) MultipartFile image){
        MerchandiseResponse response = merchandiseService.update(id, request, image);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "상품 재고 차감", description = "요청 수량만큼 상품 재고를 차감함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차감 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "수량 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "재고 부족")
    })
    public ApiResponse<MerchandiseResponse> decreaseStock(@PathVariable Long id,
                                                          @RequestParam int quantity){
        MerchandiseResponse response = merchandiseService.decreaseStock(id, quantity);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제", description = "상품을 판매 목록에서 삭제 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음")
    })
    public ApiResponse<Void> delete(@PathVariable Long id) {
        merchandiseService.delete(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
