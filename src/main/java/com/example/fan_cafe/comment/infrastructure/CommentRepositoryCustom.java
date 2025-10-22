package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;

public interface CommentRepositoryCustom {

    List<CommentResponse> findAllByPostId(Long postId, Cursor cursor, int size);
    Optional<Comment> findLatest();
}
