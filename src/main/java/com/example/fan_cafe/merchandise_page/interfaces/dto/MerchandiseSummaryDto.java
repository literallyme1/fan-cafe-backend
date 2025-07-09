package com.example.fan_cafe.merchandise_page.interfaces.dto;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MerchandiseSummaryDto {
    private Long id;
    private String name;
    private String description;
    private Long price;
    private Long salePrice;
    private int stock;
    private Status status;
    private Category category;
    private String imageUrl;

    public static MerchandiseSummaryDto from(Merchandise merchandise) {
        return MerchandiseSummaryDto.builder()
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
