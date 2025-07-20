package com.example.fan_cafe.global.util.dsl;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.exception.MerchandiseErrorCode;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MerchandiseDslUtil {

    public static List<OrderSpecifier<?>> toOrderSpecifiers(Pageable pageable, PathBuilder<Merchandise> pathBuilder) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        //정렬꺼내서 정렬 조건과, 필드 확인
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty();

            switch (property) {
                case "id" -> orders.add(
                        new OrderSpecifier<>(direction, pathBuilder.getNumber("id", Long.class))
                );
                case "createdAt" -> orders.add(
                        new OrderSpecifier<>(direction, pathBuilder.getDateTime("createdAt", LocalDateTime.class))
                );
                default -> throw new CustomException(MerchandiseErrorCode.INVALID_MERCHANDISE_PROPERTY);
            }
        }

        return orders;
    }
}
