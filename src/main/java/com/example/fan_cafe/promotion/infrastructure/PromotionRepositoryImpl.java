package com.example.fan_cafe.promotion.infrastructure;

import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.global.util.PromotionDslUtil;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.example.fan_cafe.promotion.domain.QPromotion;
import com.example.fan_cafe.promotion.interfaces.dto.PromotionResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

@RequiredArgsConstructor
public class PromotionRepositoryImpl implements PromotionRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    QPromotion promotion = QPromotion.promotion;

    @Override
    public Slice<PromotionResponse> findSliceBy(Pageable pageable){

        List<OrderSpecifier<?>> orderSpecifiers = PromotionDslUtil.toOrderSpecifiers(
                pageable,
                new PathBuilder<Promotion>(Promotion.class, "promotion")
        );

        List<PromotionResponse> results = queryFactory
                .select(Projections.constructor(PromotionResponse.class,
                                promotion.id,
                                promotion.title,
                                promotion.description,
                                promotion.imageUrl,
                                promotion.startAt,
                                promotion.endAt
                        ))
                .from(promotion)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset()) // 페이지 시작 위치
                .limit(pageable.getPageSize() + 1) // 페이지 크기
                .fetch();

        return PageUtils.toSlice(results, pageable);

    }
}
