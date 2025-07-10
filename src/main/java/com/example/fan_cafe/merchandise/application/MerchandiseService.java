package com.example.fan_cafe.merchandise.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.exception.MerchandiseErrorCode;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseGroupedResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseListResponse;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchandiseService {

    private final MerchandiseRepository merchandiseRepository;
    private final S3Uploader s3Uploader;

    public MerchandiseResponse create(MerchandiseRequest request, MultipartFile image){
        String imageUrl = image.isEmpty()? null : s3Uploader.upload(image, "merchandise");
        Merchandise merchandise = this.create(request, imageUrl);
        return MerchandiseResponse.from(merchandise);
    }

    //트랜잭션 분리
    @Transactional
    public Merchandise create(MerchandiseRequest request, String imageUrl) {
        Merchandise merchandise = Merchandise.of(request, imageUrl);
        return merchandiseRepository.save(merchandise);
    }

    public MerchandiseGroupedResponse get(int page, int size, Category category) {
        Pageable pageable = PageUtils.createPageRequest(page, size, "createdAt", "DESC");

        List<Category> categories = (category != null)
                ? List.of(category)
                : List.of(Category.values());

        List<MerchandiseListResponse> groupedResponses = categories.stream()
                .map(cat -> {
                    Slice<Merchandise> merchandises = merchandiseRepository.findTopByCategory(Status.SALE, cat, pageable);
                    List<MerchandiseResponse> responses = toResponseList(merchandises);
                    return MerchandiseListResponse.of(cat, responses, merchandises.hasNext());
                })
                .toList();

        return MerchandiseGroupedResponse.of(groupedResponses);
    }

    public MerchandiseResponse update(Long id, MerchandiseRequest request, MultipartFile image){

        Merchandise merchandise = findByIdOrThrow(id);
        String newImageUrl = resolveImageUrl(merchandise, request, image);
        Merchandise newMerchandise = update(merchandise, id, request, newImageUrl);
        return MerchandiseResponse.from(newMerchandise);
    }

    @Transactional
    public Merchandise update(Merchandise merchandise, Long id, MerchandiseRequest request, String imageUrl){
        merchandise.update(request, imageUrl);
        merchandise.markSoldOutIfNecessary();
        return merchandise;
    }

    @Transactional
    public MerchandiseResponse decreaseStock(Long id, int quantity){
        Merchandise merchandise = findByIdOrThrow(id);
        merchandise.decreaseStock(quantity);
        merchandise.markSoldOutIfNecessary();
        return MerchandiseResponse.from(merchandise);
    }

    @Transactional
    public void delete(Long id){
        Merchandise merchandise = findByIdOrThrow(id);
        merchandise.delete();
    }

    private String resolveImageUrl(Merchandise oldMerchandise, MerchandiseRequest request, MultipartFile image){
        String imageUrl = request.getImageUrl();

        if (request.isDeleteImage() && oldMerchandise.getImageUrl() != null) {
            s3Uploader.delete(s3Uploader.extractFileKey(oldMerchandise.getImageUrl()));
            imageUrl = null;
        }

        if(image != null && !image.isEmpty()){
            imageUrl = s3Uploader.upload(image, "merchandise");
        }
        return imageUrl;
    }

    private Merchandise findByIdOrThrow(Long id){
        return merchandiseRepository.findById(id)
                .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));
    }

    private List<MerchandiseResponse> toResponseList(Slice<Merchandise> slice) {
        return slice.stream()
                .map(MerchandiseResponse::from)
                .toList();
    }
}
