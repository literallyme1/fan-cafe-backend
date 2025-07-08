package com.example.fan_cafe.merchandise.application;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseListResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public ApiResponse<MerchandiseGroupedResponse> get(int page, int size, Category category) {
        Pageable pageable = PageUtils.createPageRequest(page, size, "createdAt", "DESC");
        Slice<Merchandise> merchandises = merchandiseRepository.searchMerchandise(Status.SALE, category, pageable);

        List<MerchandiseListResponse> groupedMerchandises;

        if (category != null) {
            List<MerchandiseResponse> responses = merchandises.stream()
                    .map(MerchandiseResponse::from)
                    .toList();
            groupedMerchandises = List.of(MerchandiseListResponse.of(category, responses));
        } else {
            Map<Category, List<MerchandiseResponse>> grouped = merchandises.stream()
                    .map(MerchandiseResponse::from)
                    .collect(Collectors.groupingBy(MerchandiseResponse::getCategory));

            groupedMerchandises = grouped.entrySet().stream()
                    .map(entry -> MerchandiseListResponse.of(entry.getKey(), entry.getValue()))
                    .toList();
        }

        return ApiResponse.success(ApiResponseStatus.SUCCESS,
                MerchandiseGroupedResponse.of(groupedMerchandises, merchandises.hasNext()));
    }
}
