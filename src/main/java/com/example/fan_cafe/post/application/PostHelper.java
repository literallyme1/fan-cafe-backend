package com.example.fan_cafe.post.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.exception.PostErrorCode;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.Cursor;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostHelper {

    private final S3Uploader s3Uploader;
    private final PostRepository postRepository;

    public void deleteUrls(List<String> removedUrl) {
        for(String url : removedUrl){
            s3Uploader.delete(s3Uploader.extractFileKey(url));
        }
    }

    public Post findByIdOrThrow(Long id){
        return  postRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
    }

    public List<String> uploadImagesAndGetUrls(List<MultipartFile> images, String dir) {
        List<String> urls = new ArrayList<>();
        for(MultipartFile image : images) {
            urls.add(s3Uploader.upload(image, dir));
        }
        return urls;
    }
    public Cursor resolveCursor(Long cursorId, LocalDateTime cursorCreatedAt) {
        if (cursorId != null && cursorCreatedAt != null) {
            return new Cursor(cursorId, cursorCreatedAt);
        }
        Optional<Post> latest = postRepository.findLatest();
        return latest
                .map(post -> new Cursor(post.getId() + 1, post.getCreatedAt().plusNanos(1)))
                .orElseGet(() -> new Cursor(Long.MAX_VALUE, LocalDateTime.now().plusNanos(1)));
    }

    public void validateOwner(User user, Post post) {
        if (!user.equals(post.getUser())) {
            throw new CustomException(PostErrorCode.POST_NOT_OWNER);
        }
    }

    public List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return List.of();
        return uploadImagesAndGetUrls(images, "post");
    }

    public List<String> mergeImageUrls(List<String> existing, List<String> uploaded) {
        List<String> merged = new ArrayList<>();
        if (existing != null) merged.addAll(existing);
        merged.addAll(uploaded);
        return merged;
    }
}
