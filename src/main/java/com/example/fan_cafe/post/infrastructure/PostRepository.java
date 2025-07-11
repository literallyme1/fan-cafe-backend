package com.example.fan_cafe.post.infrastructure;

import com.example.fan_cafe.post.domain.Post;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    @NonNull
    Optional<Post> findById(Long id);

    boolean existsById(Long id);

    Post findTopByOrderByCreatedAtDesc();
}
