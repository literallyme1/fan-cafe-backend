package com.example.fan_cafe.comment.application;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.exception.CommentErrorCode;
import com.example.fan_cafe.comment.infrastructure.CommentRepository;
import com.example.fan_cafe.comment.interfaces.dto.CommentRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentListResponse;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.CursorResolver;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;


    @Transactional
    public CommentResponse create(User user, CommentRequest request) {
        Post post = getPostOrThrow(request.getPostId());

        Comment comment = (request.getParentId() == null)
                ? Comment.of(post, user, request.getContent())
                : createReply(post, user, request.getParentId(), request.getContent());

        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    //부모 댓글만 가져오는 함수
    public CommentListResponse get(Long postId, Cursor cursor, int size) {
        //request 해석 후 DB 요청
        validatePostExists(postId);
        Cursor resolvedCursor = getResolvedCursor(cursor);
        List<CommentResponse> comments = commentRepository.findAllByPostId(postId, resolvedCursor, size);

        //대댓글 구조 조립
//        Map<Long, List<CommentResponse>> childMap = groupChildComments(comments);
//        List<CommentResponse> rootResponses = buildCommentTree(comments, childMap);

        //반환을 위한 cursor 생성
        PageSlice paging = computePageSlice(comments, cursor, size);

        return CommentListResponse.from(paging.comments(), paging.afterCursor, paging.nextCursor);
    }

    private PageSlice computePageSlice(List<CommentResponse> comments, Cursor cursor, int size){
        //get 요청이 처음일때만 반환
        Cursor afterCursor = (cursor == null)? CursorUtils.fromFirst(comments) : null;
        //다음 페이지가 있을 때만 반환
        Cursor nextCursor = (comments.size() > size)? CursorUtils.fromLast(comments) : null;
        if(nextCursor != null) comments = comments.subList(0, size);
        return new PageSlice(comments, afterCursor, nextCursor);
    }
    private record PageSlice(List<CommentResponse> comments, Cursor afterCursor, Cursor nextCursor){}

    private Cursor getResolvedCursor(Cursor cursor){
        return (cursor != null) ?
                CursorResolver.resolve(cursor.id(), cursor.at(), commentRepository::findLatest) :
                CursorResolver.resolve(null, null, commentRepository::findLatest);
    }

    @Transactional
    public CommentResponse update(User user, Long id, CommentRequest request){
        Comment comment = findByIdOrThrow(id);
        validateWriter(user, comment);
        comment.updateContent(request.getContent());

        return CommentResponse.from(comment);
    }


    @Transactional
    public void delete(User user, Long id) {
        Comment comment = findByIdOrThrow(id);
        validateWriter(user, comment);
        comment.delete();
    }

    //자식 댓글 parent 기준 그룹핑
    private Map<Long, List<CommentResponse>> groupChildComments(List<CommentResponse> comments) {
        return comments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommentResponse::getParentId));
    }

    //댓글 트리 구조 형성(리스트 연결)
    private List<CommentResponse> buildCommentTree(List<CommentResponse> comments, Map<Long, List<CommentResponse>> childMap) {
        return comments.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> connectChildren(root, childMap))
                .toList();
    }

    private CommentResponse connectChildren(CommentResponse root, Map<Long, List<CommentResponse>> childMap) {
        List<CommentResponse> children = childMap.getOrDefault(root.getId(), List.of());
        root.getChildren().addAll(children);
        return root;
    }

    private static void validateWriter(User principalUser, Comment comment) {
        if(!comment.getUser().getId().equals(principalUser.getId())) {
            throw new CustomException(CommentErrorCode.COMMENT_NOT_OWNER);
        }
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.POST_NOT_FOUND));
    }

    private void validatePostExists(Long postId) {
        if(!postRepository.existsByIdAndDeletedAtIsNull(postId)){
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

