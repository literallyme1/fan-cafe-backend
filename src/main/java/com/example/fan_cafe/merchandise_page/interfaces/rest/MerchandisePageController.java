package com.example.fan_cafe.merchandise_page.interfaces.rest;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.merchandise_page.application.MerchandisePageService;
import com.example.fan_cafe.merchandise_page.interfaces.dto.MerchandisePageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchandise-page")
@RequiredArgsConstructor
public class MerchandisePageController {

    private final MerchandisePageService merchandisePageService;

    @GetMapping
    public ApiResponse<MerchandisePageResponse> get(@RequestParam(defaultValue = "10") int size){
        MerchandisePageResponse response = merchandisePageService.get(size);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }
}
