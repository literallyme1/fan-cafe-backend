//package com.example.fan_cafe.follow.infrastructure;
//
//import com.example.fan_cafe.follow.domain.QFollow;
//import com.example.fan_cafe.follow.interfaces.dto.FollowResponse;
//import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
//import com.example.fan_cafe.global.common.Cursor;
//import com.example.fan_cafe.global.util.CursorUtils;
//import com.example.fan_cafe.user.domain.QUser;
//import com.querydsl.core.types.Projections;
//import com.querydsl.core.types.dsl.CaseBuilder;
//import com.querydsl.jpa.JPAExpressions;
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import lombok.RequiredArgsConstructor;
//
//import java.util.List;
//
//@RequiredArgsConstructor
//public class FollowRepositoryImpl implements FollowRepositoryCustom{
//
//    private final JPAQueryFactory queryFactory;
//    QFollow follow = QFollow.follow;
//    QUser user = QUser.user;
//
//    @Override
//    public List<FollowerItemResponse> findFollowers(
//            Long targetId, Long viewerId, Cursor cursor, int size) {
//
//        /**
//         viewerId : 팔로워를 보는 사람
//         targetId : 보여지는 사람
//         existsExpr : 조건식 1. viewer(follower) 가 팔로우 한 게 있는 지 체크 2. viewer 가 팔로잉 한 사람과 같은 사람이 있는 지 체크 (팔로워 중)
//         */
//        var existsExpr = JPAExpressions.selectOne().from(follow)
//                .where(follow.id.followerId.eq(viewerId)
//                        .and(follow.id.followingId.eq(user.id)))
//                .exists();
//
//        return queryFactory
//                .select(Projections.constructor(FollowerItemResponse.class,
//                        user.id,
//                        user.nickname,
//                        new CaseBuilder().when(existsExpr).then(true).otherwise(false),
//                        follow.createdAt))
//                .from(follow)
//                .join(user).on(user.id.eq(follow.id.followerId))
//                .where(
//                        follow.id.followingId.eq(targetId), //팔로잉 하는 사람이 target
//                        CursorUtils.beforeDesc(follow.createdAt, follow.id.followerId, cursor)
//                )
//                .orderBy(follow.createdAt.desc(), follow.id.followerId.desc())
//                .limit(size + 1)
//                .fetch();
//    }
//
//
//    @Override
//    public List<FollowResponse> findNextPage(Cursor cursor, int size, Long userId){
//
//        return queryFactory.select(
//                Projections.constructor(FollowResponse.class,
//                        follow.id,
//                        follow.following.id,
//                        follow.following.nickname,
//                        follow.following.avatarUrl,
//                        follow.createdAt,
//                        JPAExpressions
//                                .selectOne()
//                                .from(follow)
//                                .where(userId.eq(follow.following.id))
//                                .and(follow.follower.id.eq(follow.following.id))
//                        .exists()
//        )
//                        .from(follow)
//                        .join(follow.following, user)
//                        .where(CursorUtils.beforeDesc(follow.createdAt, follow.id, cursor),
//                                follow.follower.eq(userId))
//                        .orderBy(follow.createdAt.desc(), follow.id.desc()))
//                        .limit(size + 1)
//                        .fetch();
//
//    }
//}
