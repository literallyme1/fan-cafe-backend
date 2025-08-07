package com.example.fan_cafe.like;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.domain.Like;
import com.example.fan_cafe.like.infrastructure.LikeRepository;
import com.example.fan_cafe.like.interfaces.dto.LikeListResponse;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.domain.Post;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;


    @Mock
    private PostHelper postHelper;
    @InjectMocks
    private LikeService likeService;

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
    void create_shouldLikePost_whenNotAlreadyLiked(){
        //given
        Long postId = 1L;
        when(postHelper.findByIdOrThrow(any())).thenReturn(mockPost);

        //when
        LikeResponse response = likeService.like(mockUser, postId);

        //then
        verify(likeRepository, times(1)).save(any(Like.class));
        assertThat(mockPost.getLikeCount()).isEqualTo(1);
        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(1);
    }

    @Test
    void create_shouldThrowLikeError_whenAlreadyLiked(){
        //given
        Long postId = 1L;
        when(postHelper.findByIdOrThrow(any())).thenReturn(mockPost);
        when(likeRepository.save(any())).thenThrow(new DataIntegrityViolationException("중복"));
        //when
        assertThrows(CustomException.class, () -> likeService.like(mockUser, 1L));
    }

    @Test
    void delete_shouldDeleteLike_when_likeExists(){
        //given
        Long postId = 1L;
        Like like = Like.of(mockUser, mockPost);
        ReflectionTestUtils.setField(mockPost, "likeCount", 1);
        when(likeRepository.findByUserAndPost(mockUser, mockPost)).thenReturn(Optional.of(like));
        when(postHelper.findByIdOrThrow(any())).thenReturn(mockPost);

        //when
        var response = likeService.unlike(mockUser, postId);

        //then
        verify(likeRepository, times(1)).delete(any(Like.class));
        assertThat(mockPost.getLikeCount()).isEqualTo(0);
        assertThat(response.getLikeCount()).isEqualTo(0);
    }

    @Test
    void should_returnLikeList_when_userIsGiven() {
        // given
        LikeResponse like1 = new LikeResponse(1L, true, 10);
        LikeResponse like2 = new LikeResponse(2L, true, 5);
        List<LikeResponse> mockLikes = List.of(like1, like2);

        when(likeRepository.findLikeResponsesByUser(mockUser)).thenReturn(mockLikes);

        // when
        LikeListResponse response = likeService.get(mockUser);

        // then
        assertThat(response.getLikes()).hasSize(2);
        assertThat(response.getLikes())
                .extracting("postId")
                .containsExactly(1L, 2L);
    }

}
