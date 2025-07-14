package com.example.fan_cafe.post;


import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PostHelperTest {

    @Mock
    private S3Uploader s3Uploader;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostHelper postHelper;


}
