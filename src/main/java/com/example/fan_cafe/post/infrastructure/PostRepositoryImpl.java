package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.like.domain.QLike;
import com.example.fan_cafe.like.infrastructure.LikeRepositoryCustom;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.like.interfaces.dto.QLikeResponse;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.domain.QPost;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.post.interfaces.dto.QPostResponse;
import com.example.fan_cafe.user.domain.User;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QPost post = QPost.post;



    /**

     SELECT p FROM Post p
     WHERE (p.deletedAt IS NULL)
     AND ((p.createdAt < :createdAt)
     OR (p.createdAt = :createdAt AND p.id < :id))
     ORDER BY p.createdAt DESC, p.id DESC
     */

    @Override
    public List<PostResponse> findNextPage(LocalDateTime createdAt, Long id, int size) {

        List<Post> posts = queryFactory
                .selectFrom(post)
                .where( post.deletedAt.isNull(),
                        post.createdAt.lt(createdAt)
                                .or(post.createdAt.eq(createdAt).and(post.id.lt(id)))
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(size)
                .fetch();

        return posts.stream()
                .map(PostResponse::from)
                .toList();
    }

    @Override
    public List<PostResponse> findNewPosts(LocalDateTime createdAt, Long id, int size){
        List<Post> posts = queryFactory
                .selectFrom(post)
                .where( post.deletedAt.isNull(),
                        post.createdAt.gt(createdAt)
                                .or(post.createdAt.eq(createdAt).and(post.id.gt(id)))
                )
                .orderBy(post.createdAt.desc(), post.id.asc())
                .limit(size)
                .fetch();

        return posts.stream()
                .map(PostResponse::from)
                .toList();


    }


}
