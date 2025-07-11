package com.example.fan_cafe.post;

import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.like.domain.Like;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.PostCreateRequest;
import com.example.fan_cafe.post.interfaces.dto.PostCreateResponse;
import com.example.fan_cafe.post.interfaces.dto.PostResponse;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostHelper postHelper;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private PostService postService;

    User mockUser;
    Post mockPost;

    @BeforeEach
    void setUp(){
        mockUser = new User(1L,"test@test.com", "encode_pw", "nickname", Role.USER);
        mockPost = Post.builder()
                .id(1L)
                .user(mockUser)
                .title("오늘의 송가인")
                .content("오늘도 아름답네요")
                .build();
    }

    @Test
    void create_shouldPost_whenValidRequest(){
        //given

        PostCreateRequest request = PostCreateRequest.builder()
                .title("제목입니다.")
                .content("내용입니다.")
                .build();

        MultipartFile image = mock(MultipartFile.class);
        List<MultipartFile> imageList = List.of(image);

        List<String> uploadedUrls = List.of("new-url.jpg");
        when(postHelper.uploadImagesAndGetUrls(imageList, "post")).thenReturn(uploadedUrls);

        //when
        PostResponse response = postService.create(mockUser, request, imageList);

        //then
        verify(postRepository, times(1)).save(any(Post.class));
        assertThat(response.getTitle()).isEqualTo("제목입니다.");
    }


}
