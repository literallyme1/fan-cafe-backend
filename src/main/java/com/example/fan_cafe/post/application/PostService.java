package com.example.fan_cafe.post.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.post.exception.PostErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.*;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostHelper postHelper;



    public PostResponse create(User user, PostCreateRequest request, List<MultipartFile> images) {
        List<String> imageUrls = postHelper.uploadImagesAndGetUrls(images, "post");
        try{
            return createWithImages(user, request, imageUrls);
        } catch (Exception e) {
            postHelper.deleteUrls(imageUrls);
            throw e;
        }
    }

    @Transactional
    public PostResponse createWithImages(User user, PostCreateRequest request, List<String> imageUrls) {
        Post post = Post.of(user, request.getTitle(), request.getContent(), imageUrls);
        postRepository.save(post);
        return PostResponse.from(post);
    }

    public PostListResponse get(Long cursorId, LocalDateTime cursorCreatedAt, int size) {
        Cursor cursor = postHelper.resolveCursor(cursorId, cursorCreatedAt);
        List<PostResponse> postDtos = postRepository.findNextPage(cursor.createdAt(), cursor.id(), size);
        boolean hasNext = postDtos.size() == size;

        Cursor nextCursor = hasNext ? postHelper.resolveCursor(postDtos.getLast().getId(), postDtos.getLast().getCreatedAt())
                : postHelper.resolveCursor(null, null);

        return PostListResponse.from(
                postDtos, nextCursor.id(), nextCursor.createdAt(), hasNext
        );
    }

    public PostListResponse getNewPosts(Long cursorId, LocalDateTime cursorCreatedAt, int size) {
        Cursor cursor = postHelper.resolveCursor(cursorId, cursorCreatedAt);
        List<PostResponse> postDtos = postRepository.findNewPosts(cursor.createdAt(), cursor.id(), size);

        boolean hasNext = postDtos.size() == size;
        Cursor nextCursor = hasNext ? postHelper.resolveCursor(postDtos.getLast().getId(), postDtos.getLast().getCreatedAt())
                : postHelper.resolveCursor(null, null);

        return PostListResponse.from(
                postDtos, nextCursor.id(), nextCursor.createdAt(), hasNext
        );
    }


    public PostUpdateResponse update(User user, Long postId, PostUpdateRequest request, List<MultipartFile> images) {

        Post post = postHelper.findByIdOrThrow(postId);
        postHelper.validateOwner(user, post);

        //새로운 이미지 업로드
        List<String> uploadedUrls = postHelper.uploadImages(images);
        List<String> finalImageUrls = postHelper.mergeImageUrls(request.getImageUrls(), uploadedUrls);

        try{
            List<String> removedUrl = update(post, request.getTitle(), request.getContent(), finalImageUrls);
            postHelper.deleteUrls(removedUrl);
            return PostUpdateResponse.from(finalImageUrls);
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


}
