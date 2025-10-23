package com.example.fan_cafe.comment.infrastructure;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;

import java.util.List;
import java.util.Optional;

public interface CommentRepositoryCustom {

    List<CommentResponse> findCommentsByPostId(Long postId, Cursor cursor, int size);
    List<CommentResponse> findRepliesByParentId(Long parentId, Cursor cursor, int size);
    Optional<Comment> findLatestParentComment();
    Optional<Comment> findLatestRepliesByParentId(Long parentId);
}
