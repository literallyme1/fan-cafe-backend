package com.example.fan_cafe.bookmark.infrastructure;

import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkListItemResponse;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface BookmarkRepositoryCustom {

    Slice<BookmarkListItemResponse> findBookmarkResponsesByUser(User user, Pageable pageable);
    boolean existsByUserAndPost(User user, Post post);
}
