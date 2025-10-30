package com.example.fan_cafe.like;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.domain.Like;
import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.like.infrastructure.LikeRepository;
import com.example.fan_cafe.like.interfaces.dto.LikeListResponse;
import com.example.fan_cafe.like.interfaces.dto.LikeResponse;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @InjectMocks
    private LikeService likeService;

    User mockUser;
    Post mockPost;

    @BeforeEach
    void setUp(){
        mockUser = User.of("test@test.com", "encode_pw", "nickname", Role.USER);
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        User author = User.of("author@test.com", "pw", "author", Role.USER);
        ReflectionTestUtils.setField(author, "id", 2L);

        mockPost = Post.builder()
                .id(1L)
                .user(author)
                .title("오늘의 송가인")
                .content("오늘도 아름답네요")
                .build();
    }

    @DisplayName("타켓 정보와 유저가 주어졌을 때 기존 liked 정보가 있을 시 삭제한다.")
    @Test
    void givenTargetAndUser_whenAlreadyLiked_thenDeleteLiked(){
        //given
        User user = mockUser;
        Long targetId = 1L;
        LikeTargetType targetType = LikeTargetType.POST;

        when(likeRepository.findByTargetIdAndTargetTypeAndUserId(targetId, targetType, user)).thenReturn(Optional.ofNullable(Like.of(user, targetType, targetId)));

        //when
        boolean isLiked = likeService.toggleLike(user, targetId, targetType);
        //then
        verify(likeRepository, times(1)).delete(any(Like.class));
        assertThat(isLiked).isFalse();
    }

    @DisplayName("타켓 정보와 유저가 주어졌을 때 기존 liked 정보가 없을 시 생성한다.")
    @Test
    void givenTargetAndUser_whenNotLiked_thenSaveLiked(){
        //given
        User user = mockUser;
        Long targetId = 1L;
        LikeTargetType targetType = LikeTargetType.POST;

        when(likeRepository.findByTargetIdAndTargetTypeAndUserId(targetId, targetType, user)).thenReturn(Optional.empty());

        //when
        boolean isLiked = likeService.toggleLike(user, targetId, targetType);

        //then
        verify(likeRepository, times(1)).save(any(Like.class));
        assertThat(isLiked).isTrue();
    }

    @DisplayName("동시에 좋아요 요청이 들어오면 이미 좋아요로 판단한다.")
    @Test
    void givenConcurrentRequest_whenSaveDuplicateLike_thenThrowCustomException() {
        when(likeRepository.findByTargetIdAndTargetTypeAndUserId(any(), any(), any()))
                .thenReturn(Optional.empty());
        doThrow(DataIntegrityViolationException.class).when(likeRepository).save(any(Like.class));

        // when & then
        assertThrows(CustomException.class,
                () -> likeService.toggleLike(mockUser, 1L, LikeTargetType.POST));

    }

}
