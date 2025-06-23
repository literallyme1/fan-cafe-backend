package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
