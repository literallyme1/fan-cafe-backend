package com.example.fan_cafe.merchandise_page.interfaces.rest;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.merchandise_page.application.MerchandisePageService;
import com.example.fan_cafe.merchandise_page.interfaces.dto.MerchandisePageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchandise-page")
@RequiredArgsConstructor
@Tag(name = "상품 홈", description = "상품과 프로모션 통합 화면 조회")
public class MerchandisePageController {

    private final MerchandisePageService merchandisePageService;

    @GetMapping
    @Operation(summary = "상품 홈 조회", description = "카테고리별 상품과 프로모션을 한 번에 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 크기 오류")
    })
    public ApiResponse<MerchandisePageResponse> get(@RequestParam(defaultValue = "10") int size){
        MerchandisePageResponse response = merchandisePageService.get(size);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }
}
