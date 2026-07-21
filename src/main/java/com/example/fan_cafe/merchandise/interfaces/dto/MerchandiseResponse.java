package com.example.fan_cafe.merchandise.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
public class MerchandiseResponse {

    @Schema(description = "상품 식별자", example = "501")
    private Long id;
    @Schema(description = "상품명", example = "2026 월드투어 공식 티셔츠")
    private String name;
    @Schema(description = "상품 설명", example = "서울 공연 한정 공식 굿즈")
    private String description;
    @Schema(description = "정상 가격", example = "35000")
    private Long price;
    @Schema(description = "할인 가격", example = "29500")
    private Long salePrice;
    @Schema(description = "재고 수량", example = "498")
    private int stock;
    @Schema(description = "판매 상태", example = "SALE")
    private Status status;
    @Schema(description = "상품 카테고리", example = "FASHION")
    private Category category;
    @Schema(description = "상품 이미지 URL", example = "https://cdn.fancafe.kr/merch/501/main.jpg")
    private String imageUrl;

    @QueryProjection
    public MerchandiseResponse(Long id, String name,
                               String description,
                               Long price,
                               Long salePrice,
                               int stock,
                               Status status,
                               Category category,
                               String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.salePrice = salePrice;
        this.stock = stock;
        this.status = status;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public static MerchandiseResponse from(Merchandise merchandise) {
        return MerchandiseResponse.builder()
                .id(merchandise.getId())
                .name(merchandise.getName())
                .description(merchandise.getDescription())
                .price(merchandise.getPrice())
                .salePrice(merchandise.getSalePrice())
                .stock(merchandise.getStock())
                .status(merchandise.getStatus())
                .category(merchandise.getCategory())
                .imageUrl(merchandise.getImageUrl())
                .build();
    }
}
