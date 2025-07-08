package com.example.fan_cafe.merchandise.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.merchandise.application.MerchandiseService;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseListResponse;
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
                                                   @RequestPart("image")MultipartFile image) {
        return merchandiseService.create(request, image);
    }

    @GetMapping
    public ApiResponse<MerchandiseGroupedResponse> get(@RequestParam(required = false)int page,
                                                       @RequestParam(required = false)Category category,
                                                       @RequestParam(defaultValue = "10") int size) {
        return merchandiseService.get(page, size, category);
    }
}
