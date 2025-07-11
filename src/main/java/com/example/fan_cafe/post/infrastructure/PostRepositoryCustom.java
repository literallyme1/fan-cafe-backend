package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.user.domain.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepositoryCustom {

    List<PostResponse> findNextPage(LocalDateTime createdAt, Long id, int size);
    List<PostResponse> findNewPosts(LocalDateTime createdAt, Long id, int size);
}
