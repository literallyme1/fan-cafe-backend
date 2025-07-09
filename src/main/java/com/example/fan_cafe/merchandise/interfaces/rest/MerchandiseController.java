package com.example.fan_cafe.merchandise.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.merchandise.application.MerchandiseService;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/merchandises")
@RequiredArgsConstructor
public class MerchandiseController {

    private final MerchandiseService merchandiseService;

    @PostMapping
    public ApiResponse<MerchandiseResponse> create(@RequestPart("merchandise") @Valid MerchandiseRequest request,
                                                   @RequestPart(value = "image", required = false)MultipartFile image) {
        MerchandiseResponse response =  merchandiseService.create(request, image);
        return  ApiResponse.success(ApiResponseStatus.CREATED, response);
    }

    @GetMapping
    public ApiResponse<MerchandiseGroupedResponse> get(@RequestParam(defaultValue = "0")int page,
                                                       @RequestParam(required = false)Category category,
                                                       @RequestParam(defaultValue = "10") int size) {
        MerchandiseGroupedResponse response = merchandiseService.get(page, size, category);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PutMapping("/{id}")
    public ApiResponse<MerchandiseResponse> update(@PathVariable Long id,
                                                   @RequestBody MerchandiseRequest request){
        MerchandiseResponse response = merchandiseService.update(id, request);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @PatchMapping("/{id}/stock")
    public ApiResponse<MerchandiseResponse> decreaseStock(@PathVariable Long id,
                                                          @RequestParam int quantity){
        MerchandiseResponse response = merchandiseService.decreaseStock(id, quantity);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        merchandiseService.delete(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
