package com.example.fan_cafe.follow.infrastructure;

import com.example.fan_cafe.follow.domain.QFollow;
import com.example.fan_cafe.follow.interfaces.dto.FollowResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QFollow follow = QFollow.follow;
    QUser user = QUser.user;


    //user(follower) 가 팔로잉한 목록
    @Override
    public List<FollowResponse> findNextFollowingPage(Cursor cursor, int size, Long userId) {

        //서브 사용해서 where 절로 인해 맞팔 여부 확인 X 문제를 해결
        QFollow followSub = new QFollow("followSub");

        return queryFactory.select(
                        Projections.constructor(FollowResponse.class,
                                follow.id,
                                follow.following.id,
                                follow.following.nickname,
                                follow.following.avatarUrl,
                                follow.createdAt,
                                JPAExpressions
                                        .selectOne()
                                        .from(followSub)
                                        .where(followSub.follower.id.eq(follow.following.id),
                                                followSub.following.id.eq(userId))
                                        .exists()
                        ))
                .from(follow)
                .join(follow.following, user)
                .where(CursorUtils.beforeDesc(follow.createdAt, follow.id, cursor),
                        follow.follower.id.eq(userId))
                .orderBy(follow.createdAt.desc(), follow.id.desc())
                .limit(size + 1)
                .fetch();

    }

    //user(follower) 가 팔로잉한 목록
    @Override
    public List<FollowResponse> findNextFollowerPage(Cursor cursor, int size, Long userId) {

        QFollow followSub = new QFollow("followSub");

        return queryFactory.select(
                        Projections.constructor(FollowResponse.class,
                                follow.id,
                                follow.follower.id,
                                follow.follower.nickname,
                                follow.follower.avatarUrl,
                                follow.createdAt,
                                JPAExpressions
                                        .selectOne()
                                        .from(followSub)
                                        .where(
                                                followSub.follower.id.eq(userId),
                                                followSub.following.id.eq(follow.follower.id)
                                        )
                                        .exists()
                        ))
                .from(follow)
                .join(follow.follower, user)
                .where(
                        CursorUtils.beforeDesc(follow.createdAt, follow.id, cursor),
                        follow.following.id.eq(userId)
                )
                .orderBy(follow.createdAt.desc(), follow.id.desc())
                .limit(size + 1)
                .fetch();
    }
}
