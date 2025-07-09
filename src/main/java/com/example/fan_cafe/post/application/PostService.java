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
    private final S3Uploader s3Uploader;

    public PostCreateResponse create(User user, PostCreateRequest request, List<MultipartFile> images) {
        List<String> imageUrls = uploadImagesAndGetUrls(images, "post");

        return createWithImages(user, request, imageUrls);
    }

    @Transactional
    public PostCreateResponse createWithImages(User user, PostCreateRequest request, List<String> imageUrls) {
        Post post = Post.of(user, request.getTitle(), request.getContent(), imageUrls);
        postRepository.save(post);
        return PostCreateResponse.from(post.getId(), imageUrls);
    }

    public PostListResponse get(Long cursorId, LocalDateTime cursorCreatedAt, int size) {
        Cursor cursor = resolveCursor(cursorId, cursorCreatedAt);
        Pageable pageable = PageUtils.createPageRequest(size);
        List<Post> posts = postRepository.findNextPage(cursor.createdAt(), cursor.id(), pageable);
        List<PostResponse> postDtoList = posts.stream().map(PostResponse::from).toList();

        boolean hasNext = postDtoList.size() == size;
        Long nextCursorId = null;
        LocalDateTime nextCursorCreatedAt = null;

        if(hasNext) {
            PostResponse last = postDtoList.getLast();
            nextCursorId = last.getId();
            nextCursorCreatedAt = last.getCreatedAt();

        }
        return PostListResponse.from(
                postDtoList, nextCursorId, nextCursorCreatedAt, hasNext
        );
    }

    public PostListResponse getNewPosts(Long cursorId, LocalDateTime cursorCreatedAt, int size) {
        Cursor cursor = resolveCursor(cursorId, cursorCreatedAt);
        Pageable pageable = PageUtils.createPageRequest(size);
        List<Post> posts = postRepository.findNewPosts(cursor.createdAt(), cursor.id(), pageable);
        List<PostResponse> postDtoList = posts.stream().map(PostResponse::from).toList();

        boolean hasNext = postDtoList.size() == size;
        Long nextCursorId = null;
        LocalDateTime nextCursorCreatedAt = null;

        if(hasNext) {
            PostResponse last = postDtoList.getLast();
            nextCursorId = last.getId();
            nextCursorCreatedAt = last.getCreatedAt();

        }
        PostListResponse postResponse = PostListResponse.from(
                postDtoList, nextCursorId, nextCursorCreatedAt, hasNext
        );
        return postResponse;
    }

    @Transactional
    public PostUpdateResponse update(User user, Long postId, PostUpdateRequest request, List<MultipartFile> images) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
        if(!user.equals(post.getUser())) {throw new CustomException(PostErrorCode.POST_NOT_OWNER);}

        //새로운 이미지 업로드
        List<String> uploadedUrls = (images != null && !images.isEmpty())
                ? uploadImagesAndGetUrls(images, "post")
                : List.of();

        //update 된 url + 새로운 이미지 url
        List<String> finalImageUrls = new ArrayList<>();
        finalImageUrls.addAll(request.getImageUrls());
        finalImageUrls.addAll(uploadedUrls);

        post.update(request.getTitle(), request.getContent(), finalImageUrls);

        return PostUpdateResponse.from(finalImageUrls);
    }

    @Transactional
    public void delete(User user, Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
        if(!user.equals(post.getUser())) throw new CustomException(PostErrorCode.POST_NOT_OWNER);
        post.delete();
    }

    private List<String> uploadImagesAndGetUrls(List<MultipartFile> images, String dir) {
        List<String> urls = new ArrayList<>();
        for(MultipartFile image : images) {
            urls.add(s3Uploader.upload(image, dir));
        }
        return urls;
    }
    private Cursor resolveCursor(Long cursorId, LocalDateTime cursorCreatedAt) {
        if (cursorId != null && cursorCreatedAt != null) {
            return new Cursor(cursorId, cursorCreatedAt);
        }
        Post latest = postRepository.findTopByOrderByCreatedAtDesc();
        if (latest == null) {
            return new Cursor(Long.MAX_VALUE, LocalDateTime.now().plusNanos(1));
        }
        return new Cursor(latest.getId() + 1, latest.getCreatedAt().plusNanos(1));
    }
}
