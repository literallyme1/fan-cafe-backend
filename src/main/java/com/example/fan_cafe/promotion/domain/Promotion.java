package com.example.fan_cafe.promotion.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.promotion.exception.PromotionErrorCode;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "promotions")
public class Promotion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String imageUrl;

    @Column
    private LocalDate startAt;

    @Column
    private LocalDate endAt;

    public static Promotion of(PromotionRequest dto, String imageUrl) {
        Promotion promotion = Promotion.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(imageUrl)
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .build();
        promotion.validateTime();
        return promotion;
    }

    public void update(PromotionRequest dto, String imageUrl){
        this.title = dto.getTitle();
        this.description = dto.getDescription();
        this.imageUrl = imageUrl;
        this.startAt = dto.getStartAt();
        this.endAt = dto.getEndAt();
        this.validateTime();
    }

    public void validateTime() {
        if(startAt!= null && endAt != null && endAt.isBefore(startAt)){
            throw new CustomException(PromotionErrorCode.PROMOTION_INVALID_TIME);
        }
    }

}
