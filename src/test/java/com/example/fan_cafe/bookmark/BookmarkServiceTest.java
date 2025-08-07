package com.example.fan_cafe.bookmark;

import com.example.fan_cafe.auth.application.AuthService;
import com.example.fan_cafe.bookmark.application.BookmarkService;
import com.example.fan_cafe.bookmark.domain.Bookmark;
import com.example.fan_cafe.bookmark.infrastructure.BookmarkRepository;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkResponse;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostHelper postHelper;

    @InjectMocks
    private BookmarkService bookmarkService;

    private User mockUser;
    private Post mockPost;

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
    }

    @Test
    void add_shouldCreate_whenNotAlreadyMarked(){

        Long postId = 1L;
        //given
        when(postHelper.findByIdOrThrow(postId)).thenReturn(mockPost);

        //when
        BookmarkResponse response = bookmarkService.add(mockUser, postId);

        //then
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
        assertThat(response.isMarked()).isTrue();
    }

    @Test
    void add_shouldThrowError_whenAlreadyMarked(){
        Long postId = 1L;
        //given
        when(postHelper.findByIdOrThrow(postId)).thenReturn(mockPost);
        when(bookmarkRepository.save(any())).thenThrow(new DataIntegrityViolationException("중복"));

        //when
        assertThrows(CustomException.class, () -> bookmarkService.add(mockUser, postId));
    }

    @Test
    void remove_shouldRemove_when_bookmarkExists(){
        Long postId = 1L;
        Bookmark mockBookmark = Bookmark.of(mockUser, mockPost);
        //given
        when(postHelper.findByIdOrThrow(postId)).thenReturn(mockPost);
        when(bookmarkRepository.findByUserAndPost(mockUser, mockPost)).thenReturn(Optional.of(mockBookmark));

        //when
        BookmarkResponse response = bookmarkService.remove(mockUser, postId);

        //then
        verify(bookmarkRepository, times(1)).delete(mockBookmark);
        assertThat(response.isMarked()).isFalse();
    }
}
