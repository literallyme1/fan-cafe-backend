package com.example.fan_cafe.comment.application;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.exception.CommentErrorCode;
import com.example.fan_cafe.comment.infrastructure.CommentRepository;
import com.example.fan_cafe.comment.interfaces.dto.CommentCreateRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentListResponse;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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

    public ApiResponse<CommentListResponse> get(Long postId, int page) {
        validatePostExists(postId);
        Pageable pageable =  PageUtils.createPageRequest( page, 10,"createdAt", "DESC");
        Slice<Comment> roots = commentRepository.findByPostIdAndParentIsNull(postId, pageable);

        List<Long> rootIds = roots.getContent().stream()
                .map(Comment::getId)
                .toList();

        List<Comment> children = commentRepository.findByParentIdIn(rootIds);
        Map<Long, List<CommentResponse>> childrenMap = mapChildResponses(children);


        List<CommentResponse> rootResponses = roots.getContent().stream()
                .map(root -> buildCommentResponseWithChildren(root, childrenMap))
                .toList();
        return ApiResponse.success(ApiResponseStatus.SUCCESS, CommentListResponse.from(rootResponses, roots.hasNext()));
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.POST_NOT_FOUND));
    }

    private void validatePostExists(Long postId) {
        if(!postRepository.existsById(postId)){
            throw new CustomException(CommentErrorCode.POST_NOT_FOUND);
        }
    }

    private Comment createReply(Post post, User user, Long parentId, String content) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (parent.getParent() != null) {
            throw new CustomException(CommentErrorCode.INVALID_COMMENT_DEPTH);
        }

        return Comment.of(post, user, parent, content);
    }

    private Map<Long, List<CommentResponse>> mapChildResponses(List<Comment> children) {
        return children.stream()
                .map(CommentResponse::from)
                .collect(Collectors.groupingBy(CommentResponse::getParentId));
    }

    private CommentResponse buildCommentResponseWithChildren(Comment root, Map<Long, List<CommentResponse>> childMap) {
        CommentResponse response = CommentResponse.from(root);
        List<CommentResponse> childResponses = childMap.getOrDefault(root.getId(), List.of());
        response.getChildren().addAll(childResponses);
        return response;
    }
}

