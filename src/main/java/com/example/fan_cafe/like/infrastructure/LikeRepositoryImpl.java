package com.example.fan_cafe.like.infrastructure;

import com.example.fan_cafe.like.domain.QLike;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.like.interfaces.dto.QLikeResponse;
import com.example.fan_cafe.user.domain.User;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    QLike like = QLike.like;

    @Override
    public Set<Long> findLikedInPostIds(Long userId, List<Long> postIds) {
        return new HashSet<>(queryFactory.select(like.post.id)
                .from(like)
                .where(like.user.id.eq(userId),
                        like.post.id.in(postIds))
                .fetch());
    }
}
