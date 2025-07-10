package com.example.fan_cafe.merchandise.interfaces.dto;


import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String name;


    private String description;

    @NotNull(message = "상품의 가격을 입력하세요.")
    private Long price;

    private Long salePrice;

    private int stock;

    @NotNull(message = "판매 상태를 알려주세요.")
    private Status status;

    @NotNull(message = "카테고리를 알려주세요.")
    private Category category;

    private String imageUrl;

    private boolean deleteImage;

}
