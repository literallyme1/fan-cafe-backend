package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface CommentRepositoryCustom {

    Slice<CommentResponse> findAllByPostId(Long postId, Pageable pageable);
}
