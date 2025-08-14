package com.example.fan_cafe.post;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.exception.PostErrorCode;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostHelperTest {

    @Mock
    private S3Uploader s3Uploader;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostHelper postHelper;

    User mockUser;
    Post mockPost;

    @BeforeEach
    void setUp(){
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
    void findByIdOrThrow_shouldThrowException_whenPostNotFound(){
        //given
        Long postId = 999L;

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            postHelper.findByIdOrThrow(postId);
        });

        //then
        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void uploadImagesAndGetUrls_shouldReturnUrls_whenImagesGiven(){

        //given
        MultipartFile image1 = mock(MultipartFile.class);
        MultipartFile image2 = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(image1, image2);
        when(s3Uploader.upload(image1, "post")).thenReturn("new-uploaded1.jpg");
        when(s3Uploader.upload(image2, "post")).thenReturn("new-uploaded2.jpg");

        //when
        var response = postHelper.uploadImagesAndGetUrls(images, "post");

        //then
        assertThat(response).hasSize(2);
        assertThat(response).containsExactly("new-uploaded1.jpg", "new-uploaded2.jpg");
    }

    @Test
    void resolveCursor_shouldReturnGivenCursor_whenCursorParamsAreNull(){

        //given
        Long cursorId = null;
        LocalDateTime cursorCreatedAt = null;
        when(postRepository.findLatest()).thenReturn(Optional.of(mockPost));
        ReflectionTestUtils.setField(mockPost, "createdAt", LocalDateTime.of(2025, 1, 1, 0, 0));
        //when
        var response = postHelper.resolveCursor(cursorId, cursorCreatedAt);

        //then
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.at()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0).plusNanos(1));
    }
}
