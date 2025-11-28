package com.example.fan_cafe.post.application;


import com.example.fan_cafe.bookmark.infrastructure.BookmarkRepository;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.CursorResolver;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.global.redis.CacheTTL;
import com.example.fan_cafe.global.redis.RedisKeyUtil;
import com.example.fan_cafe.global.redis.RedisService;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.like.infrastructure.LikeRepository;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.*;
import com.example.fan_cafe.user.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostHelper postHelper;

    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LikeRepository likeRepository;


    private final LikeService likeService;
    private final RedisService redisService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    public PostResponse create(User user, PostCreateRequest request, List<MultipartFile> images) {
        List<String> imageUrls = postHelper.uploadImagesAndGetUrls(images, "post");
        try {
            return createWithImages(user, request, imageUrls);
        } catch (Exception e) {
            postHelper.deleteUrls(imageUrls);
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public PostResponse createWithImages(User user, PostCreateRequest request, List<String> imageUrls) {
        Post post = Post.of(user, request.getTitle(), request.getContent(), imageUrls);
        postRepository.save(post);
        return PostResponse.from(post, false, false);
    }

    public PostListResponse get(Cursor cursor, int size, Long userId) {

        // 1) 데이터 로딩
        List<PostResponse> posts = cursor == null
        ? getFirstPagePosts(size, userId)
                : postRepository.findNextPage(getResolvedCursor(cursor), size, userId);


        //2) 페이징 처리 & 반환
        PageSlice paging = computePageSlice(posts, size, cursor);
        return PostListResponse.fromCursors(
                paging.posts(), paging.nextCursor(), paging.afterCursor
        );
    }

    private List<PostResponse> getFirstPagePosts(int size, Long userId) {
        // 1. Raw Data 로딩 (Redis + DB Fallback)
        List<CachedPostItem> cachedPosts = loadCachedPosts(size);

        if (cachedPosts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. UserInfo 조회 (Parameter Object 생성)
        UserInfo userInfo = getUserInfo(userId, cachedPosts);

        // 3. 데이터 조립
        return enrichPostWithUserInfo(cachedPosts, userInfo);
    }

    private List<CachedPostItem> loadCachedPosts(int size) {
        String redisKey = RedisKeyUtil.getLatestPostListKey(size);

        // Redis 시도 -> 없으면 DB
        return getCachedPostItemsInRedis(redisKey)
                .orElseGet(() -> loadFromDbAndCache(size, redisKey));
    }

    private List<CachedPostItem> loadFromDbAndCache(int size, String redisKey) {
        log.info("[CACHE MISS] Latest posts → DB query");
        List<CachedPostItem> posts = postRepository.findLatestCachedPosts(size);

        // 비동기 처리나 별도 메소드로 분리하면 더 좋지만, 지금도 충분함
        try {
            String json = objectMapper.writeValueAsString(posts);
            redisService.set(redisKey, json, CacheTTL.POST_LIST_LATEST);
        } catch (Exception e) {
            log.error("[REDIS SAVE ERROR] key={}", redisKey, e);
        }
        return posts;
    }

    private UserInfo getUserInfo(Long userId, List<CachedPostItem> posts) {
        // posts가 empty일 때 처리는 호출부에서 했으므로 바로 stream 시작
        List<Long> postIds = posts.stream()
                .map(CachedPostItem::getId)
                .toList();

        Set<Long> likes = likeRepository.findLikedPostIds(userId, postIds);
        Set<Long> bookmarks = bookmarkRepository.findBookmarkedPostIds(userId, postIds);

        return new UserInfo(likes, bookmarks); // 변수 선언 없이 바로 리턴
    }

    private record UserInfo(Set<Long> likes, Set<Long> bookmarks) {
    }

    private static List<PostResponse> enrichPostWithUserInfo(List<CachedPostItem> posts, UserInfo userInfo) {
        return posts.stream()
                .map(p -> {
                    boolean liked = userInfo.likes.contains(p.getId());
                    boolean bookmarked = userInfo.bookmarks.contains(p.getId());
                    return PostResponse.from(p, liked, bookmarked);
                })
                .toList();
    }

    // 반환 타입을 Optional로 변경하여 호출자에게 "없을 수도 있음"을 알림
    private Optional<List<CachedPostItem>> getCachedPostItemsInRedis(String redisKey) {
        String cached = redisService.get(redisKey);
        if (cached == null) {
            return Optional.empty();
        }

        try {
            log.info("[CACHE HIT] key={}", redisKey);
            List<CachedPostItem> posts = objectMapper.readValue(cached, new TypeReference<>() {});
            return Optional.ofNullable(posts);
        } catch (Exception e) {
            log.warn("[CACHE PARSE ERROR] cache deleted. key={}", redisKey);
            redisTemplate.delete(redisKey);
            return Optional.empty();
        }
    }


    public PostListResponse getNewPosts(Cursor cursor, int size, Long userId) {
        //cursor 가 null 일 시
        if (cursor == null || cursor.id() == null || cursor.at() == null) {
            cursor = new Cursor(0L, LocalDateTime.MIN);
        }

        //1. count 확인
        Long newPostsCount = postRepository.countNewPosts(cursor);
        if (newPostsCount > 50) return get(cursor, size, userId);
        if (newPostsCount == 0) return PostListResponse.fromAfterCursor(List.of(), null);

        Cursor resolvedCursor = (newPostsCount == 1L) ? cursor : getResolvedCursor(cursor);
        List<PostResponse> postDtos = postRepository.findNewPosts(resolvedCursor, size, userId);
        Cursor afterCursor = CursorUtils.fromFirst(postDtos);
        return PostListResponse.fromAfterCursor(
                postDtos, afterCursor
        );
    }


    public PostResponse update(User user, Long postId, PostUpdateRequest request, List<MultipartFile> images) {

        Post post = postHelper.findByIdOrThrow(postId);
        postHelper.validateOwner(user, post);

        //새로운 이미지 업로드
        List<String> uploadedUrls = postHelper.uploadImages(images);
        List<String> finalImageUrls = postHelper.mergeImageUrls(request.getImageUrls(), uploadedUrls);

        try {
            List<String> removedUrl = update(post, request.getTitle(), request.getContent(), finalImageUrls);
            postHelper.deleteUrls(removedUrl);
            boolean isBookmarked = bookmarkRepository.existsByUserAndPost(user, post);
            return PostResponse.from(post, false, isBookmarked);
        } catch (Exception e) {
            postHelper.deleteUrls(uploadedUrls);
            throw e;
        }
    }


    @Transactional
    public List<String> update(Post post, String title, String content, List<String> imageUrls) {
        return post.update(title, content, imageUrls);
    }


    @Transactional
    public void delete(User user, Long id) {
        Post post = postHelper.findByIdOrThrow(id);
        postHelper.deleteUrls(post.getImageUrls());
        postHelper.validateOwner(user, post);
        post.delete();
    }

    @Transactional
    public void toggleLike(User user, Long id) {
        Post post = postHelper.findByIdOrThrow(id);
        boolean isLiked = likeService.toggleLike(user, id, LikeTargetType.POST);
        if (isLiked) {
            post.increaseLikeCount();
        } else {
            post.decreaseLikeCount();
        }
    }

    private Cursor getResolvedCursor(Cursor cursor) {
        return (cursor != null) ?
                CursorResolver.resolve(cursor.id(), cursor.at(), postRepository::findLatest)
                : CursorResolver.resolve(null, null, postRepository::findLatest);
    }

    //반환 할 beforeCursor 생성
    private PageSlice computePageSlice(List<PostResponse> posts, int size, Cursor cursor) {
        //첫 페이지 일 시 가장 최신글 커서 반환
        Cursor afterCursor = (cursor == null) ? CursorUtils.fromFirst(posts) : null;
        Cursor nextCursor = (posts.size() > size) ? CursorUtils.fromLast(posts) : null;
        //hasNext 확인 후 size 대로 cut
        if (nextCursor != null) posts = posts.subList(0, size);
        return new PageSlice(posts, nextCursor, afterCursor);

    }

    private record PageSlice(List<PostResponse> posts, Cursor nextCursor, Cursor afterCursor) {
    }


}
