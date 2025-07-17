package com.example.fan_cafe.comment;


import com.example.fan_cafe.comment.application.CommentService;
import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.infrastructure.CommentRepository;
import com.example.fan_cafe.comment.interfaces.dto.CommentRequest;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private User mockUser;
    private Post mockPost;
    private Comment mockRootComment;
    private Comment mockReply;

    @BeforeEach
    void setUp() {

        mockUser = new User(1L,"test@test.com", "encode_pw", "nickname", Role.USER);
        mockPost = Post.builder()
                .id(1L)
                .user(mockUser)
                .title("오늘의 송가인")
                .content("오늘도 아름답네요")
                .build();
        mockRootComment = Comment.builder()
                .id(1L)
                .post(mockPost)
                .user(mockUser)
                .content("첫번째 댓글")
                .build();

        mockReply = Comment.builder()
                .id(2L)
                .post(mockPost)
                .user(mockUser)
                .content("첫번째 댓글의 댓글")
                .parent(mockRootComment)
                .build();
    }

    @Test
    void create_root_comment_success() {
        //given
        CommentRequest request = new CommentRequest(1L, "첫번째 댓글입니다.", null);
        when(postRepository.findById(request.getPostId())).thenReturn(Optional.of(mockPost));

        //when
        var response = commentService.create(mockUser, request);

        //then
        verify(commentRepository, times(1)).save(any(Comment.class));

    }

    @Test
    void create_reply_success() {
        //given
        CommentRequest request = new CommentRequest(2L, "대댓글입니다.", 1L);
        when(postRepository.findById(request.getPostId())).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(request.getParentId())).thenReturn(Optional.of(mockRootComment));
//        when(commentRepository.findById(request.getParentId())).thenReturn(Optional.empty());


        //when
        var response = commentService.create(mockUser, request);

        //then
        verify(commentRepository, times(1)).save(any(Comment.class));

    }

    @Test
    void create_reply_depth_2_failed() {
        //given
        CommentRequest request = new CommentRequest(3L, "대대댓글입니다.", 2L);
        when(postRepository.findById(request.getPostId())).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(request.getParentId())).thenReturn(Optional.of(mockReply));

        //when & then
        assertThrows(CustomException.class, () -> { commentService.create(mockUser, request); });
    }

    @Test
    void delete_success() {
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(mockRootComment));

        commentService.delete(mockUser, 1L);

        //then
        assertNotNull(mockRootComment.getDeletedAt());

    }

}
