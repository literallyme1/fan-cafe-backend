package com.example.fan_cafe.like.infrastructure;

import com.example.fan_cafe.like.domain.QLike;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.like.interfaces.dto.QLikeResponse;
import com.example.fan_cafe.user.domain.User;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    QLike like = QLike.like;

    @Override
    public List<LikeResponse> findLikeResponsesByUser(User user) {

        return queryFactory
                .select(new QLikeResponse(like.post.id, Expressions.constant(true), like.post.likeCount))
                .from(like)
                .where(like.user.eq(user))
                .fetch();
    }

}
