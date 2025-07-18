package com.example.fan_cafe.merchandise.interfaces.dto;

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

    private Long id;
    private String name;
    private String description;
    private Long price;
    private Long salePrice;
    private int stock;
    private Status status;
    private Category category;
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
