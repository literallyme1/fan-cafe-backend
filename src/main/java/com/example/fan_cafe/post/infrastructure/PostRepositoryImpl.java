package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.bookmark.domain.QBookmark;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.SoftDeleteCondition;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.like.domain.QLike;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.domain.QPost;
import com.example.fan_cafe.post.domain.QPostImage;
import com.example.fan_cafe.post.interfaces.dto.CachedPostItem;
import com.example.fan_cafe.post.interfaces.dto.PostImageDto;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QPost post = QPost.post;
    QLike like = QLike.like;
    QBookmark bookmark = QBookmark.bookmark;
    QPostImage postImage = QPostImage.postImage;


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

    public List<Long> findLatestPostIds(int size) {
        return queryFactory.select(post.id)
                .from(post)
                .where(SoftDeleteCondition.isNotDeleted(post.deletedAt))
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(size)
                .fetch();
    }

    private List<PostImageDto> findImagesByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) return List.of();

        return queryFactory
                .select(Projections.constructor(
                        PostImageDto.class,
                        postImage.post.id,
                        postImage.imageUrl
                ))
                .from(postImage)
                .where(postImage.post.id.in(postIds))
                .orderBy(postImage.id.asc())
                .fetch();
    }

    private Map<Long, List<String>> groupImages(List<PostImageDto> imageDtos) {
        return imageDtos.stream()
                .collect(Collectors.groupingBy(
                        PostImageDto::postId,
                        Collectors.mapping(PostImageDto::url, Collectors.toList())
                ));
    }

    @Override
    public List<CachedPostItem> findLatestCachedPosts(int size) {

        // 1. 최신 PostId 리스트
        List<Long> postIds = findLatestPostIds(size + 1);
        if (postIds.isEmpty()) return List.of();

        // 2. 이미지 DTO 가져오기
        List<PostImageDto> imageDtos = findImagesByPostIds(postIds);

        // 3. Map<PostId, List<String>> 만들기
        Map<Long, List<String>> imageMap = groupImages(imageDtos);

        // 4. Post 본문 + 작성자 기본 정보 + like/commentCount 가져오기
        List<CachedPostItem> items = queryFactory
                .select(Projections.constructor(
                        CachedPostItem.class,
                        post.id,
                        post.title,
                        post.content,
                        post.user.id,
                        post.user.nickname,
                        post.user.avatarUrl,
                        post.likeCount,
                        post.commentCount,
                        post.createdAt
                ))
                .from(post)
                .where(post.id.in(postIds))
                .orderBy(post.createdAt.desc(), post.id.desc())
                .fetch();

        // 5. 각 CachedPostItem 에 이미지 붙이기
        items.forEach(item -> {
            List<String> urls = imageMap.getOrDefault(item.getId(), List.of());
            item.setImageUrls(urls);  // setter 필요 (혹은 builder)
        });

        return items;
    }

    @Override
    public List<PostResponse> findNewPosts(Cursor cursor, int size, Long userId) {
        List<Tuple> results = queryFactory
                .select(post, like.id, bookmark.id)
                .from(post)
                .leftJoin(like).on(like.post.eq(post), like.user.id.eq(userId))
                .leftJoin(bookmark).on(bookmark.post.eq(post), bookmark.user.id.eq(userId))
                .where(SoftDeleteCondition.isNotDeleted(post.deletedAt),
                        CursorUtils.afterDesc(post.createdAt, post.id, cursor)
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
    public Long countNewPosts(Cursor cursor) {
        return queryFactory
                .select(post.count())
                .from(post)
                .where(SoftDeleteCondition.isNotDeleted(post.deletedAt),
                        CursorUtils.afterDesc(post.createdAt, post.id, cursor)
                )
                .fetchOne();
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

    @Override
    public void increaseCommentCount(Long postId, int extraCount) {
        queryFactory
                .update(post)
                .set(
                        post.commentCount,
                        post.commentCount.add(extraCount)
                )
                .where(SoftDeleteCondition.isNotDeleted(post.deletedAt),
                        post.id.eq(postId))
                .execute();
    }
}
