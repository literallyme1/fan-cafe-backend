package com.example.fan_cafe.promotion.dsl;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.example.fan_cafe.promotion.exception.PromotionErrorCode;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PromotionDslUtil {

    public static List<OrderSpecifier<?>> toOrderSpecifiers(Pageable pageable, PathBuilder<Promotion> pathBuilder) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        //정렬꺼내서 정렬 조건과, 필드 확인
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty();

            switch (property) {
                case "id" -> orders.add(
                        new OrderSpecifier<>(direction, pathBuilder.getNumber("id", Long.class))
                );
                case "at" -> orders.add(
                        new OrderSpecifier<>(direction, pathBuilder.getDateTime("at", LocalDateTime.class))
                );
                default -> throw new CustomException(PromotionErrorCode.INVALID_PROMOTION_PROPERTY);
            }
        }

        return orders;
    }
}
