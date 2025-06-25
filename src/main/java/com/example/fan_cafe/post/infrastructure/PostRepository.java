package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.post.domain.Post;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(""" 
        SELECT p FROM Post p
        WHERE (p.createdAt < :createdAt)
           OR (p.createdAt = :createdAt AND p.id < :id)
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findNextPage(@Param("createdAt")LocalDateTime createdAt,
                            @Param("id") Long id,
                            Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE (p.createdAt > :createdAt)
           OR (p.createdAt = :createdAt AND p.id > :id)
        ORDER BY p.createdAt ASC, p.id ASC
    """)
    List<Post> findNewPosts(@Param("createdAt") LocalDateTime createdAt,
                            @Param("id") Long id,
                            Pageable pageable);

    Post findTopByOrderByCreatedAtDesc();
}
