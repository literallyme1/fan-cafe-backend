package com.example.fan_cafe.merchandise.infrastructure;

import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.global.util.PromotionDslUtil;
import com.example.fan_cafe.merchandise.domain.Category;
import com.example.fan_cafe.merchandise.domain.QMerchandise;
import com.example.fan_cafe.merchandise.domain.Status;
import com.example.fan_cafe.merchandise.interfaces.dto.MerchandiseResponse;
import com.example.fan_cafe.promotion.domain.Promotion;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

@RequiredArgsConstructor
public class MerchandiseRepositoryImpl implements MerchandiseRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QMerchandise merchandise = QMerchandise.merchandise;


    @Override
    public Slice<MerchandiseResponse> findTopByCategory(Status status, Category category, Pageable pageable){

        List<OrderSpecifier<?>> orderSpecifiers = PromotionDslUtil.toOrderSpecifiers(
                pageable,
                new PathBuilder<Promotion>(Promotion.class, "promotion")
        );

        List<MerchandiseResponse> results = queryFactory
                .select(Projections.constructor(MerchandiseResponse.class,
                        merchandise.id,
                        merchandise.name,
                        merchandise.description,
                        merchandise.price,
                        merchandise.salePrice,
                        merchandise.stock,
                        merchandise.status,
                        merchandise.category,
                        merchandise.imageUrl

                        ))
                .from(merchandise)
                .where(
                        merchandise.deletedAt.isNull(), // 삭제되지 않은 것
                        merchandise.status.eq(status), // 상태 일치
                        merchandise.category.eq(category) // 카테고리 일치
                )
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset()) // 페이지 시작 위치
                .limit(pageable.getPageSize() + 1) // 페이지 크기
                .fetch();

        return PageUtils.toSlice(results, pageable);

    }
}
