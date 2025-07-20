package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.domain.QComment;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.util.dsl.CommentDslUtil;
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


@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    QComment comment = QComment.comment;
    QUser user = QUser.user;

    @Override
    public Slice<CommentResponse> findAllByPostId(Long postId, Pageable pageable){

        List<OrderSpecifier<?>> orderSpecifiers = CommentDslUtil.toOrderSpecifiers(
                pageable,
                new PathBuilder<Comment>(Comment.class, "comment") //인자 (엔티티 명시, qComment 별칭)
        );

        List<CommentResponse> results = queryFactory
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
                .leftJoin(comment.parent)
                .where(comment.post.id.eq(postId))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0])) // 정렬 조건
                .offset(pageable.getOffset()) // 페이지 시작 위치
                .limit(pageable.getPageSize() + 1) // 페이지 크기
                .fetch();

        return PageUtils.toSlice(results, pageable);
    }


}
