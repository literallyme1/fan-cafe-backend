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
        PageSlice paging = computePageSlice(postDtos, size);

        return PostListResponse.fromBeforeCursor(
                paging.posts(), paging.nextCursor()
        );
    }

//    public PostListResponse getNewPosts(Cursor cursor, int size, Long userId) {
//        List<PostResponse> postDtos = postRepository.findNewPosts(cursor, size, userId);
//
//        boolean hasNext = postDtos.size() == size;
//        Cursor nextCursor = hasNext ? postHelper.resolveCursor(postDtos.getLast().getId(), postDtos.getLast().getCreatedAt())
//                : postHelper.resolveCursor(null, null);
//
//        return PostListResponse.from(
//                postDtos, nextCursor, hasNext
//        );
//    }


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

    //반환 할 cursor 생성
    private PageSlice computePageSlice(List<PostResponse> posts, int size){
        if (posts.size() <= size) {
            return new PageSlice(posts, null);
        }
        posts = posts.subList(0, size);
        Cursor nextCursor = CursorUtils.fromLast(posts);
        return new PageSlice(posts, nextCursor);

    }

    private record PageSlice(List<PostResponse> posts, Cursor nextCursor){}


}
