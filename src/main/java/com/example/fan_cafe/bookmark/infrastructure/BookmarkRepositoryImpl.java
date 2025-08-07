package com.example.fan_cafe.bookmark.infrastructure;

import com.example.fan_cafe.bookmark.domain.Bookmark;
import com.example.fan_cafe.bookmark.domain.QBookmark;
import com.example.fan_cafe.bookmark.dsl.BookmarkDslUtil;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkListItemResponse;
import com.example.fan_cafe.global.common.SoftDeleteCondition;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.domain.QPost;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.User;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final PostRepository postRepository;
    QBookmark bookmark = QBookmark.bookmark;
    QPost post = QPost.post;

    @Override
    public Slice<BookmarkListItemResponse> findBookmarkResponsesByUser(User user, Pageable pageable){

        List<OrderSpecifier<?>> orderSpecifiers = BookmarkDslUtil.toOrderSpecifiers(
                pageable,
                new PathBuilder<Bookmark>(Bookmark.class, "bookmark") //q타입 내부 경로
        );

        List<Long> postIds = queryFactory
                .select(post.id)
                .from(bookmark)
                .join(bookmark.post, post)
                .where(
                        SoftDeleteCondition.isNotDeleted(bookmark.deletedAt),
                        bookmark.user.id.eq(user.getId())
                )
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        //post 를 가져옴.
        List<Post> posts = postRepository.findAllById(postIds);
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity())); //(postId, post)

        List<BookmarkListItemResponse> content = postIds.stream()
                .map(postId -> {
                    Post p = postMap.get(postId);
                    return BookmarkListItemResponse.from(p);
                })
                .toList();

        return PageUtils.toSlice(content, pageable);
    }
}
