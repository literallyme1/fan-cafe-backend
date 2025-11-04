package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.domain.QComment;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.SoftDeleteCondition;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.like.domain.QLike;
import com.example.fan_cafe.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    QComment comment = QComment.comment;
    QUser user = QUser.user;
    QLike like = QLike.like;


    @Override
    public List<CommentResponse> findCommentsByPostId(Long postId, Long userId, Cursor cursor, int size) {

        return queryFactory.select(
                        Projections.constructor(CommentResponse.class,
                                comment.id,
                                comment.content,
                                comment.parent.id,
                                comment.user.id,
                                comment.user.nickname,
                                comment.user.avatarUrl,
                                comment.createdAt,
                                comment.likeCount,
                                JPAExpressions
                                        .selectOne()
                                        .from(like)
                                        .where(like.user.id.eq(userId)
                                                .and(like.targetId.eq(comment.id))
                                                .and(like.targetType.eq(LikeTargetType.COMMENT)))
                                        .exists()
                        )
                )
                .from(comment)
                .join(comment.user, user)
                .leftJoin(comment.parent) //parent 를 가져오는 이유 : isNUll 인지 여부를 가져오기 위해서
                .where(CursorUtils.beforeDesc(comment.createdAt, comment.id, cursor),
                        comment.post.id.eq(postId),
                        comment.parent.isNull(),
                        SoftDeleteCondition.isNotDeleted(comment.deletedAt))
                .orderBy(comment.createdAt.desc(), comment.id.desc()) // 정렬 조건
                .limit(size + 1)
                .fetch();

    }

    @Override
    public List<CommentResponse> findRepliesByParentId(Long parentId, Long userId, Cursor cursor, int size) {

        return queryFactory
                .select(Projections.constructor(CommentResponse.class,
                        comment.id,
                        comment.content,
                        comment.parent.id,
                        comment.user.id,
                        comment.user.nickname,
                        comment.user.avatarUrl,
                        comment.createdAt,
                        comment.likeCount,
                        JPAExpressions
                                .selectOne()
                                .from(like)
                                .where(like.user.id.eq(userId)
                                        .and(like.targetId.eq(comment.id))
                                        .and(like.targetType.eq(LikeTargetType.COMMENT)))
                                .exists()
                ))
                .from(comment)
                .join(comment.user, user)
                .leftJoin(comment.parent)
                .where(CursorUtils.beforeDesc(comment.createdAt, comment.id, cursor),
                        comment.parent.id.eq(parentId),
                        SoftDeleteCondition.isNotDeleted(comment.deletedAt))
                .orderBy(comment.createdAt.desc(), comment.id.desc()) // 정렬 조건
                .limit(size + 1)
                .fetch();
    }

    @Override
    public Optional<Comment> findLatestParentComment() {
        Comment result = queryFactory
                .select(comment)
                .from(comment)
                .where(SoftDeleteCondition.isNotDeleted(comment.deletedAt))
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .limit(1)
                .fetchFirst();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Comment> findLatestRepliesByParentId(Long parentId) {
        Comment result = queryFactory
                .select(comment)
                .from(comment)
                .where(SoftDeleteCondition.isNotDeleted(comment.deletedAt),
                        comment.parent.id.eq(parentId))
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .limit(1)
                .fetchFirst();
        return Optional.ofNullable(result);
    }


}
