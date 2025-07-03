package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @NonNull
    Optional<Comment> findById(Long id);

    Slice<Comment> findByPostIdAndParentIsNull(Long postId,Pageable pageable);

    List<Comment> findByParentIdIn(List<Long> parentIds);
}
