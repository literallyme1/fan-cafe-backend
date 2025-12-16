package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.interfaces.dto.CachedPostItem;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;

import java.util.List;
import java.util.Optional;

public interface PostRepositoryCustom {

    List<PostResponse> findNextPage(Cursor cursor, int size, Long userId);

    List<PostResponse> findNewPosts(Cursor cursor, int size, Long userId);

    Long countNewPosts(Cursor cursor);

    Optional<Post> findLatest();

    List<CachedPostItem> findLatestCachedPosts(int size);

    void increaseCommentCount(Long postId, int extraCount);
}