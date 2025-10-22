package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;

public interface CommentRepositoryCustom {

    Slice<CommentResponse> findAllByPostId(Long postId, Pageable pageable);
    Optional<Comment> findLatest();
}
