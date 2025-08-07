package com.example.fan_cafe.post;

import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.*;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostHelper postHelper;


    @InjectMocks
    private PostService postService;

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

    @Test
    void get_shouldGetPastPost_whenValidRequest(){
        //given
        Long cursorId = null;
        LocalDateTime cursorCreatedAt = null;
        Cursor mockCursor = new Cursor(100L, LocalDateTime.now());

        when(postHelper.resolveCursor(cursorId, cursorCreatedAt)).thenReturn(mockCursor);

        PostResponse dto1 = new PostResponse(1L, "title1", "content1", "nickname", 0, 0, LocalDateTime.now(), List.of(), false, false);
        PostResponse dto2 = new PostResponse(2L, "title2", "content2","nickname",0, 0, LocalDateTime.now(), List.of(), false, false);
        List<PostResponse> dtos = List.of(dto1, dto2);

        when(postRepository.findNextPage(mockCursor.createdAt(), mockCursor.id(), 2, mockUser.getId())).thenReturn(dtos);

        Cursor nextCursor = new Cursor(200L, LocalDateTime.now().plusSeconds(1));
        when(postHelper.resolveCursor(2L, dto2.getCreatedAt())).thenReturn(nextCursor);

        //when
        PostListResponse result = postService.get(cursorId, cursorCreatedAt, 2, mockUser.getId());

        //then
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getNextCursorId()).isEqualTo(nextCursor.id());
        assertThat(result.getNextCursorCreatedAt()).isEqualTo(nextCursor.createdAt());
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    void update_shouldUpdatePost_whenValidRequest(){
        //given

        Long postId = 1L;
        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("update")
                .content("update content")
                .imageUrls(List.of("old1.jpg"))
                .build();
        List<MultipartFile> images = List.of(mock(MultipartFile.class));
        List<String> uploadedUrls = List.of("new-uploaded.jpg");
        List<String> finalImageUrls = List.of("old1.jpg", "new-uploaded.jpg");

        when(postHelper.findByIdOrThrow(postId)).thenReturn(mockPost);
        doNothing().when(postHelper).validateOwner(mockUser, mockPost);
        when(postHelper.uploadImages(any())).thenReturn(uploadedUrls);
        when(postHelper.mergeImageUrls(request.getImageUrls(), uploadedUrls)).thenReturn(finalImageUrls);

        //when
        var response = postService.update(mockUser, postId, request, images);

        //then
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getImageUrls()).containsExactly("old1.jpg", "new-uploaded.jpg");

        assertThat(mockPost.getContent()).isEqualTo(request.getContent());
        assertThat(mockPost.getImageUrls()).containsExactly("old1.jpg", "new-uploaded.jpg");
    }

}
