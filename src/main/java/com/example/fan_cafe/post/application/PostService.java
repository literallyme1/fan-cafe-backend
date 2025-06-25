package com.example.fan_cafe.post.application;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import com.example.fan_cafe.post.interfaces.dto.PostDto;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import com.example.fan_cafe.post.interfaces.dto.Cursor;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public ApiResponse<Void> create(User user, PostCreateRequest request) {
        Post post = request.toEntity(user);
        postRepository.save(post);
        return ApiResponse.success(ApiResponseStatus.CREATED);
    }

    public ApiResponse<PostResponse> get(Long cursorId, LocalDateTime cursorCreatedAt, int size) {
        Cursor cursor = resolveCursor(cursorId, cursorCreatedAt);
        Pageable pageable = PageUtils.createPageRequest(size);
        List<Post> posts = postRepository.findNextPage(cursorCreatedAt, cursorId, pageable);
        List<PostDto> postDtoList = posts.stream().map(PostDto::from).toList();

        boolean hasNext = postDtoList.size() == size;
        Long nextCursorId = null;
        LocalDateTime nextCursorCreatedAt = null;

        if(hasNext) {
            PostDto last = postDtoList.getLast();
            nextCursorId = last.getId();
            nextCursorCreatedAt = last.getCreatedAt();

        }
        PostResponse postResponse =PostResponse.from(
                postDtoList, nextCursorId, nextCursorCreatedAt, hasNext
        );
        return ApiResponse.success(ApiResponseStatus.SUCCESS, postResponse);
    }

//    public ApiResponse<Void> update() {
//    }

    public ApiResponse<Void> delete(Long id) {
        postRepository.deleteById(id);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
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
