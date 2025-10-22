package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.domain.QComment;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.comment.dsl.CommentDslUtil;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.SoftDeleteCondition;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.user.domain.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    QComment comment = QComment.comment;
    QUser user = QUser.user;

    @Override
    public List<CommentResponse> findAllByPostId(Long postId, Cursor cursor, int size){

        return queryFactory
                .select(Projections.constructor(CommentResponse.class,
                        comment.id,
                        comment.content,
                        comment.parent.id,
                        comment.user.id,
                        comment.user.nickname,
                        comment.createdAt
                ))
                .from(comment)
                .join(comment.user, user)
                .leftJoin(comment.parent) //parent 를 가져오는 이유가 뭔가?
                .where(CursorUtils.beforeDesc(comment.createdAt, comment.id, cursor),
                        comment.post.id.eq(postId),
                        SoftDeleteCondition.isNotDeleted(comment.deletedAt))
                .orderBy(comment.createdAt.desc(), comment.id.desc()) // 정렬 조건
                .limit(size + 1)
                .fetch();
    }

    @Override
    public Optional<Comment> findLatest(){
        Comment result = queryFactory
                .select(comment)
                .from(comment)
                .where(SoftDeleteCondition.isNotDeleted(comment.deletedAt))
                .orderBy(comment.createdAt.desc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(result);
    }


}
