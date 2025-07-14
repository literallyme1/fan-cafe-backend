package com.example.fan_cafe.post;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.exception.PostErrorCode;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    void resolveCursor_shouldReturnGivenCursor_whenCursorParamsProvided(){

    }

}
