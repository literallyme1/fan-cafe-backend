package com.example.fan_cafe.post.application;


import com.example.fan_cafe.bookmark.infrastructure.BookmarkRepository;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.CursorResolver;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.*;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostHelper postHelper;



    public PostResponse create(User user, PostCreateRequest request, List<MultipartFile> images) {
        List<String> imageUrls = postHelper.uploadImagesAndGetUrls(images, "post");
        try{
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

        Cursor resolvedCursor = getResolvedCursor(cursor);
        List<PostResponse> postDtos = postRepository.findNextPage(resolvedCursor, size, userId);
        PageSlice paging = computePageSlice(postDtos, size, cursor);

        return PostListResponse.fromCursors(
                paging.posts(), paging.nextCursor(), paging.afterCursor
        );
    }


    public PostListResponse getNewPosts(Cursor cursor, int size, Long userId) {
        //cursor 가 null 일 시
        if (cursor == null || cursor.id() == null || cursor.at() == null) {
            cursor = new Cursor(0L, LocalDateTime.MIN);
        }

        //1. count 확인
        Long newPostsCount = postRepository.countNewPosts(cursor);
        if(newPostsCount > 50) return get(cursor, size, userId);
        if (newPostsCount == 0) return PostListResponse.fromAfterCursor(List.of(), null);

        Cursor resolvedCursor = (newPostsCount == 1L)? cursor : getResolvedCursor(cursor);
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

        try{
            List<String> removedUrl = update(post, request.getTitle(), request.getContent(), finalImageUrls);
            postHelper.deleteUrls(removedUrl);
            boolean isBookmarked = bookmarkRepository.existsByUserAndPost(user, post);
            return PostResponse.from(post, false, isBookmarked);
        }catch (Exception e){
            postHelper.deleteUrls(uploadedUrls);
            throw e;
        }
    }


    @Transactional
    public List<String> update(Post post, String title, String content, List<String> imageUrls){
        return post.update(title, content, imageUrls);
    }


    @Transactional
    public void delete(User user, Long id) {
        Post post = postHelper.findByIdOrThrow(id);
        postHelper.deleteUrls(post.getImageUrls());
        postHelper.validateOwner(user, post);
        post.delete();
    }

    private Cursor getResolvedCursor(Cursor cursor){
         return (cursor != null) ?
                CursorResolver.resolve(cursor.id(), cursor.at(), postRepository::findLatest)
                : CursorResolver.resolve(null, null, postRepository::findLatest);
    }

    //반환 할 beforeCursor 생성
    private PageSlice computePageSlice(List<PostResponse> posts, int size, Cursor cursor){
        //첫 페이지 일 시 가장 최신글 커서 반환
        Cursor afterCursor = (cursor == null)? CursorUtils.fromFirst(posts) : null;
        Cursor nextCursor = (posts.size() > size)? CursorUtils.fromLast(posts) : null;
        //hasNext 확인 후 size 대로 cut
        if(nextCursor != null)  posts = posts.subList(0, size);
        return new PageSlice(posts, nextCursor, afterCursor);

    }

    private record PageSlice(List<PostResponse> posts, Cursor nextCursor, Cursor afterCursor){}


}
