package com.example.fan_cafe.merchandise.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseRequest;
import com.example.fan_cafe.schedule.domain.Schedule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "merchandises")
public class Merchandise extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column
    private Long salePrice;

    @Column(nullable = false)
    private Long stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

//    public static Merchandise from(MerchandiseRequest dto) {
//        return Merchandise.builder()
//                .name(dto.getName())
//                .description(dto.getDescription())
//                .price(dto.getPrice())
//                .salePrice(dto.getSalePrice())
//                .status(dto.getStatus())
//                .imageUrl(dto.getImageUrl())
//                .category(dto.getCategory())
//                .build();
//    }
//
//    public void update(MerchandiseRequest dto) {
//        this.name = dto.getName();
//        this.description = dto.getDescription();
//        this.price = dto.getPrice();
//        this.salePrice = dto.getSalePrice();
//        this.status = dto.getStatus();
//        this.imageUrl = dto.getImageUrl();
//        this.category = dto.getCategory();
//    }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
        if(this.stock <= 0) {
            this.status = Status.SOLD_OUT;
        }
    }
}
