package com.example.fan_cafe.comment;


import com.example.fan_cafe.comment.application.CommentService;
import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.comment.exception.CommentErrorCode;
import com.example.fan_cafe.comment.infrastructure.CommentRepository;
import com.example.fan_cafe.comment.interfaces.dto.CommentListResponse;
import com.example.fan_cafe.comment.interfaces.dto.CommentRequest;
import com.example.fan_cafe.comment.interfaces.dto.CommentResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

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

        mockUser = User.of("test@test.com", "encode_pw", "nickname", Role.USER);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
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
    void create_shouldCreate_whenRequestIsValid() {
        //given
        CommentRequest request = new CommentRequest(1L, "첫번째 댓글입니다.", null);
        when(postRepository.findByIdAndDeletedAtIsNull(request.getPostId())).thenReturn(Optional.of(mockPost));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        //when
        CommentResponse  response = commentService.create(mockUser, request);

        //then

        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo(request.getContent());

    }

    @Test
    void create_shouldCreateReply_whenRequestIsValid() {
        //given
        CommentRequest request = new CommentRequest(2L, "대댓글입니다.", 1L);
        when(postRepository.findByIdAndDeletedAtIsNull(request.getPostId())).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(request.getParentId())).thenReturn(Optional.of(mockRootComment));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        //when
        var response = commentService.create(mockUser, request);

        //then
        verify(commentRepository, times(1)).save(captor.capture());
        Comment saved = captor.getValue();

        assertThat(saved.getContent()).isEqualTo(request.getContent());
        assertThat(saved.getPost()).isEqualTo(mockPost);
        assertThat(saved.getUser()).isEqualTo(mockUser);
        assertThat(saved.getParent()).isEqualTo(mockRootComment);

        assertThat(response.getContent()).isEqualTo(request.getContent());

    }

    @Test
    void create_shouldThrowException_whenDepthIsMoreThan2() {
        //given
        CommentRequest request = new CommentRequest(3L, "대대댓글입니다.", 2L);
        when(postRepository.findByIdAndDeletedAtIsNull(request.getPostId())).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(request.getParentId())).thenReturn(Optional.of(mockReply));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            commentService.create(mockUser, request);
        });

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }



    @Test
    void update_shouldUpdate_whenRequestIsValid() {
        //given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockRootComment));
        CommentRequest request = CommentRequest.builder()
                .content("업데이트된 댓글")
                .build();
        //when
        var response = commentService.update(mockUser, 1L, request);

        //then
        assertThat(response.getContent()).isEqualTo(request.getContent());
        assertThat(mockRootComment.getContent()).isEqualTo(request.getContent());
    }

    @Test
    void delete_ShouldDelete_whenRequestIsValid() {
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(mockRootComment));

        commentService.delete(mockUser, 1L);

        //then
        assertThat(mockRootComment.getDeletedAt()).isNotNull();

    }

    @Test
    @DisplayName("없는 글 id 를 줄 경우, 예외가 발생한다.")
    void givenInvalidPostId_whenGetComments_thenThrowsException(){
        //given
        Long postId = 999L;
        when(postRepository.existsByIdAndDeletedAtIsNull(postId)).thenReturn(false);

        //when & then
        assertThatThrownBy(() -> commentService.getComments(postId, null, 3))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CommentErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("커서가 null 일 경우 afterCursor 가 생성된다.")
    void givenNullCursor_whenGetComments_thenAfterCursorIsCreated(){
        //given
        Long postId = 1L;
        int size = 3;
        when(postRepository.existsByIdAndDeletedAtIsNull(postId)).thenReturn(true);
        when(commentRepository.findAllByPostId(anyLong(), any(Cursor.class), anyInt())).thenReturn(
                List.of(new CommentResponse(3L,  "user1", "content", null))
        );

        //when
        CommentListResponse result = commentService.getComments(postId, null, size);

        //then
        assertThat(result.getAfterCursor()).isNotNull();
        assertThat(result.getNextCursor()).isNull();
        assertThat(result.getComments()).hasSize(1);
    }

    @Test
    @DisplayName("커서가 존재할 경우 nextCursor가 생성된다.")
    void givenValidCursor_whenGetComments_thenNextCursorIsCreated(){
        //given
        Long postId = 1L;
        int size = 5;
        Cursor cursor = new Cursor(7L, LocalDateTime.of(2025, 10, 23, 8,8,8));
        when(postRepository.existsByIdAndDeletedAtIsNull(postId)).thenReturn(true);
        List<CommentResponse> comments = new ArrayList<>();
        for (long i= 6; i>0; i--) {
            comments.add(new CommentResponse(i,  "user1", "content", null));
        }
        when(commentRepository.findAllByPostId(anyLong(), any(Cursor.class), anyInt())).thenReturn(comments);

        //when
        CommentListResponse result = commentService.getComments(postId, cursor, size);

        //then
        assertThat(result.getAfterCursor()).isNull();
        assertThat(result.getNextCursor()).isNotNull();
        assertThat(result.getComments()).hasSize(5);
    }
}
