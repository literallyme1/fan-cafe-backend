package com.example.fan_cafe.bookmark.infrastructure;

import com.example.fan_cafe.bookmark.domain.Bookmark;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long>, BookmarkRepositoryCustom {

    Optional<Bookmark> findByUserAndPost(User user, Post post);
}
