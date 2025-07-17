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
import com.example.fan_cafe.user.infrastructure.UserRepository;
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
    public CommentResponse create(User user, CommentCreateRequest request) {
        Post post = getPostOrThrow(request.getPostId());

        Comment comment = (request.getParentId() == null)
                ? Comment.of(post, user, request.getContent())
                : createReply(post, user, request.getParentId(), request.getContent());

        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    public CommentListResponse get(Long postId, int page) {
        validatePostExists(postId);
        Pageable pageable =  PageUtils.createPageRequest( page, 10,"createdAt", "DESC");


        Slice<CommentResponse> comments = commentRepository.findAllByPostId(postId, pageable);

        Map<Long, List<CommentResponse>> childMap = groupChildComments(comments);
        List<CommentResponse> rootResponses = buildCommentTree(comments, childMap);
        return CommentListResponse.from(rootResponses, comments.hasNext());
    }

    //자식 댓글 parent 기준 그룹핑
    private Map<Long, List<CommentResponse>> groupChildComments(Slice<CommentResponse> comments) {
        return comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommentResponse::getParentId));
    }

    //댓글 트리 구조 형성(리스트 연결)
    private List<CommentResponse> buildCommentTree(Slice<CommentResponse> comments, Map<Long, List<CommentResponse>> childMap) {
        return comments.stream()
                .filter(c -> c.getParentId() == null)
                .peek(root -> {
                    List<CommentResponse> children = childMap.getOrDefault(root.getId(), List.of());//root id key 를 가지고 옴.
                    root.getChildren().addAll(children);//자식 리스트 연결
                })
                .toList();
    }


    @Transactional
    public void delete(User principalUser, Long id) {
        Comment comment = findByIdOrThrow(id);

        if(!comment.getUser().getId().equals(principalUser.getId())) {
            throw new CustomException(CommentErrorCode.COMMENT_NOT_OWNER);
        }

        comment.delete();
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
        Comment parent = findByIdOrThrow(parentId);

        if (parent.getParent() != null) {
            throw new CustomException(CommentErrorCode.INVALID_COMMENT_DEPTH);
        }

        return Comment.of(post, user, parent, content);
    }
    private Comment findByIdOrThrow(Long id){
        return commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));
    }


}

