package com.example.fan_cafe.comment.application;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.exception.CommentErrorCode;
import com.example.fan_cafe.comment.infrastructure.CommentRepository;
import com.example.fan_cafe.comment.interfaces.dto.CommentCreateRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;


    @Transactional
    public ApiResponse<CommentResponse> create(User user, CommentCreateRequest request) {
        Post post = getPostOrThrow(request.getPostId());

        Comment comment = (request.getParentId() == null)
                ? Comment.of(post, user, request.getContent())
                : createReply(post, user, request.getParentId(), request.getContent());

        commentRepository.save(comment);

        return ApiResponse.success(ApiResponseStatus.CREATED, CommentResponse.from(comment));
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.POST_NOT_FOUND));
    }

    private Comment createReply(Post post, User user, Long parentId, String content) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (parent.getParent() != null) {
            throw new CustomException(CommentErrorCode.INVALID_COMMENT_DEPTH);
        }

        return Comment.of(post, user, parent, content);
    }
}
