package com.example.fan_cafe.merchandise.interfaces.dto;


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
    private String name;


    private String description;

    @NotNull(message = "상품의 가격을 입력하세요.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Long price;

    @Min(value = 0, message = "세일 가격은 0원 이상이어야 합니다.")
    private Long salePrice;

    private int stock;

    @NotNull(message = "판매 상태를 알려주세요.")
    private Status status;

    @NotNull(message = "카테고리를 알려주세요.")
    private Category category;

    private String imageUrl;

    private boolean deleteImage;

    @AssertTrue(message = "세일 가격은 정가보다 작아야 합니다.")
    public boolean isValidSalePrice() {
        return salePrice == null || salePrice < price;
    }

}
