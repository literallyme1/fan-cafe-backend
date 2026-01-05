package com.example.fan_cafe.comment.application;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.events.messaging.CommentCreatedEvent;
import com.example.fan_cafe.global.event.eventIdGenerator;
import com.example.fan_cafe.comment.events.messaging.CommentEventPublisher;
import com.example.fan_cafe.comment.exception.CommentErrorCode;
import com.example.fan_cafe.comment.infrastructure.CommentRepository;
import com.example.fan_cafe.comment.interfaces.dto.CommentRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentListResponse;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.CursorResolver;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.redis.RedisKeyUtil;
import com.example.fan_cafe.global.redis.RedisService;
import com.example.fan_cafe.global.util.CursorUtils;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.notification.domain.NotificationType;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeService likeService;
    private final RedisService redisService;
    private final CommentEventPublisher eventPublisher;


    public CommentResponse create(User user, CommentRequest request) {
        Post post = getPostOrThrow(request.getPostId());
        CommentResponse response = createComment(post, user, request);

        //cache update
        updateCommentCountCache(post.getId());

        publishCommentCreatedEvent(post, response);
        return response;
    }

    private void publishCommentCreatedEvent(Post post, CommentResponse response) {
        //producer event Id  > DTO 생성 > Queue
        String eventId = eventIdGenerator.generate();
        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .eventId(eventId)
                .notificationType(NotificationType.COMMENT)
                .postId(post.getId())
                .postAuthorId(post.getUser().getId())
                .commentId(response.getId())
                .commentAuthorId(response.getAuthorId())
                .createdAt(response.getCreatedAt())
                .build();

        eventPublisher.publish(event);
    }

    private void updateCommentCountCache(Long postId) {
        //redis INCR
        String key = RedisKeyUtil.getCommentCountKey(postId);
        redisService.increaseCount(key);
        //변경된 postId 기록
        redisService.recordCommentCountChangedPost(postId);
    }

    @Transactional
    public CommentResponse createComment(Post post, User user, CommentRequest request) {

        Comment comment = (request.getParentId() == null)
                ? Comment.of(post, user, request.getContent())
                : createReply(post, user, request.getParentId(), request.getContent());

        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    //부모 댓글만 가져오는 함수
    public CommentListResponse getComments(Long postId, Long userId, Cursor cursor, int size) {
        //request 해석 후 DB 요청
        validatePostExists(postId);
        Cursor resolvedCursor = getCommentsResolvedCursor(cursor);
        List<CommentResponse> comments = commentRepository.findCommentsByPostId(postId, userId, resolvedCursor, size);

        //반환을 위한 cursor 생성
        PageSlice paging = computePageSlice(comments, cursor, size);

        return CommentListResponse.from(paging.comments, paging.afterCursor, paging.nextCursor);
    }

    //대댓글만 가져오는 함수
    public CommentListResponse getReplies(Long commentId, Long userId, Cursor cursor, int size) {
        validateCommentExists(commentId);
        Cursor resolvedCursor = getRepliesResolvedCursor(cursor, commentId);
        List<CommentResponse> replies = commentRepository.findRepliesByParentId(commentId, userId, resolvedCursor, size);
        PageSlice paging = computePageSlice(replies, cursor, size);

        return CommentListResponse.from(paging.comments, paging.afterCursor, paging.nextCursor);

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

    private Cursor getCommentsResolvedCursor(Cursor cursor){
        return (cursor != null) ?
                CursorResolver.resolve(cursor.id(), cursor.at(), commentRepository::findLatestParentComment) :
                CursorResolver.resolve(null, null, commentRepository::findLatestParentComment);
    }

    private Cursor getRepliesResolvedCursor(Cursor cursor, Long parentId){
        return (cursor != null) ?
                CursorResolver.resolve(cursor.id(), cursor.at(), () -> commentRepository.findLatestRepliesByParentId(parentId)) :
                CursorResolver.resolve(null, null, () -> commentRepository.findLatestRepliesByParentId(parentId));
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

    @Transactional
    public void toggleLike(User user, Long id) {
        Comment comment = findByIdOrThrow(id);
        boolean isLiked = likeService.toggleLike(user, id, LikeTargetType.POST);
        if (isLiked) {
            comment.increaseLikeCount();
        } else {
            comment.decreaseLikeCount();
        }
    }

//    private CommentResponse connectChildren(CommentResponse root, Map<Long, List<CommentResponse>> childMap) {
//        List<CommentResponse> children = childMap.getOrDefault(root.getId(), List.of());
//        root.getChildren().addAll(children);
//        return root;
//    }

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
        if (!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
            throw new CustomException(CommentErrorCode.POST_NOT_FOUND);
        }
    }

    private void validateCommentExists(Long commentId) {
        if(!commentRepository.existsByParentIdAndDeletedAtIsNull(commentId)){
            throw new CustomException(CommentErrorCode.COMMENT_NOT_FOUND);
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

