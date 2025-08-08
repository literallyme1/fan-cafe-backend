package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.domain.QFollow;
import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class FollowQueryRepositoryImpl implements FollowQueryRepository{

    private final JPAQueryFactory queryFactory;
    QFollow follow = QFollow.follow;
    QUser user = QUser.user;

    @Override
    public List<FollowerItemResponse> findFollowers(
            Long targetId, Long viewerId, LocalDateTime cursorAt, Long cursorId, int size) {

        var existsExpr = JPAExpressions.selectOne().from(follow)
                .where(follow.id.followerId.eq(viewerId)
                        .and(follow.id.followingId.eq(user.id)))
                .exists();

        return queryFactory
                .select(Projections.constructor(FollowerItemResponse.class,
                        user.id,
                        user.nickname,
                        new CaseBuilder().when(existsExpr).then(true).otherwise(false),
                        follow.createdAt))
                .from(follow)
                .join(user).on(user.id.eq(follow.id.followerId))
                .where(
                        follow.id.followingId.eq(targetId),
                        cursorLt(cursorAt, cursorId)
                )
                .orderBy(follow.createdAt.desc(), follow.id.followerId.desc())
                .limit(size + 1)
                .fetch();
    }

    private com.querydsl.core.types.dsl.BooleanExpression cursorLt(LocalDateTime at, Long id) {
        if (at == null || id == null) return null;
        return follow.createdAt.lt(at)
                .or(follow.createdAt.eq(at).and(follow.id.followerId.lt(id)));
    }
}
