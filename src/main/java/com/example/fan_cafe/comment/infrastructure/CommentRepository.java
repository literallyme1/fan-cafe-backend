package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    @NonNull
    @EntityGraph(attributePaths = {"user", "parent"})
    Optional<Comment> findById(Long id);

    boolean existsByParentIdAndDeletedAtIsNull(Long id);

}
