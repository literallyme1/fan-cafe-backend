package com.example.fan_cafe.merchandise.application;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MerchandiseService {

    private final MerchandiseRepository merchandiseRepository;
    private final S3Uploader s3Uploader;

    public ApiResponse<MerchandiseResponse> create(MerchandiseRequest request, MultipartFile image){
        String imageUrl = image.isEmpty()? null : s3Uploader.upload(image, "merchandise");
        Merchandise merchandise = this.create(request, imageUrl);
        return ApiResponse.success(ApiResponseStatus.CREATED, MerchandiseResponse.from(merchandise));
    }

    //트랜잭션 분리
    @Transactional
    public Merchandise create(MerchandiseRequest request, String imageUrl) {
        Merchandise merchandise = Merchandise.of(request, imageUrl);
        return merchandiseRepository.save(merchandise);
    }
}
