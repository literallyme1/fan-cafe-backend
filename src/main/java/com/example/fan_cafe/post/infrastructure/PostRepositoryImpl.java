package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.bookmark.domain.QBookmark;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.SoftDeleteCondition;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.like.domain.QLike;
import com.example.fan_cafe.like.infrastructure.LikeRepositoryCustom;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.like.interfaces.dto.QLikeResponse;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.domain.QPost;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.post.interfaces.dto.QPostResponse;
import com.example.fan_cafe.user.domain.User;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QPost post = QPost.post;
    QLike like = QLike.like;
    QBookmark bookmark = QBookmark.bookmark;


    @Override
    public List<PostResponse> findNextPage(Cursor cursor, int size, Long userId) {

        List<Tuple> results = queryFactory
                .select(post, like.id, bookmark.id)
                .from(post)
                .leftJoin(like).on(like.post.eq(post), like.user.id.eq(userId))
                .leftJoin(bookmark).on(bookmark.post.eq(post), bookmark.user.id.eq(userId))
                .where(
                        SoftDeleteCondition.isNotDeleted(post.deletedAt),
                        CursorUtils.beforeDesc(post.createdAt, post.id, cursor)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(size)
                .fetch();

        return results.stream()
                .map(tuple -> {
                    Post p = tuple.get(post);
                    boolean isLiked = tuple.get(like.id) != null;
                    boolean isBookmarked = tuple.get(bookmark.id) != null;
                    return PostResponse.from(p, isLiked, isBookmarked);
                })
                .toList();
    }

    @Override
    public List<PostResponse> findNewPosts(Cursor cursor, int size, Long userId){
        List<Tuple> results = queryFactory
                .select(post, like.id, bookmark.id)
                .from(post)
                .leftJoin(like).on(like.post.eq(post), like.user.id.eq(userId))
                .leftJoin(bookmark).on(bookmark.post.eq(post), bookmark.user.id.eq(userId))
                .where(SoftDeleteCondition.isNotDeleted(post.deletedAt),
                        CursorUtils.beforeAsc(post.createdAt, post.id, cursor)
                )
                .orderBy(post.createdAt.desc(), post.id.asc())
                .limit(size)
                .fetch();

        return results.stream()
                .map(tuple -> {
                    Post p = tuple.get(post);
                    boolean isLiked = tuple.get(like.id) != null;
                    boolean isBookmarked = tuple.get(bookmark.id) != null;
                    return PostResponse.from(p, isLiked, isBookmarked);
                })
                .toList();


    }

    @Override
    public Optional<Post> findLatest() {
        Post result = queryFactory
                .selectFrom(post)
                .where(SoftDeleteCondition.isNotDeleted(post.deletedAt))
                .orderBy(post.createdAt.desc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }


}
