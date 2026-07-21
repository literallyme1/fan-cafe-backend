package com.example.fan_cafe.merchandise.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Status;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MerchandiseRequest {


    @NotBlank(message = "상품의 이름을 입력하세요.")
    @Size(max = 50, message = "상품명은 50자 이내여야 합니다.")
    @Schema(description = "상품명", example = "2026 월드투어 공식 티셔츠")
    private String name;


    @Schema(description = "상품 설명", example = "서울 공연 한정 공식 굿즈")
    private String description;

    @NotNull(message = "상품의 가격을 입력하세요.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    @Schema(description = "정상 가격", example = "35000")
    private Long price;

    @Min(value = 0, message = "세일 가격은 0원 이상이어야 합니다.")
    @Schema(description = "할인 가격", example = "29500")
    private Long salePrice;

    @Schema(description = "재고 수량", example = "500")
    private int stock;

    @NotNull(message = "판매 상태를 알려주세요.")
    @Schema(description = "판매 상태", example = "SALE")
    private Status status;

    @NotNull(message = "카테고리를 알려주세요.")
    @Schema(description = "상품 카테고리", example = "FASHION")
    private Category category;

    @Schema(description = "기존 이미지 URL", example = "https://cdn.fancafe.kr/merch/501/main.jpg")
    private String imageUrl;

    @Schema(description = "기존 이미지 삭제 여부", example = "false")
    private boolean deleteImage;

    @AssertTrue(message = "세일 가격은 정가보다 작아야 합니다.")
    public boolean isValidSalePrice() {
        return salePrice == null || salePrice < price;
    }

}
