package com.example.fan_cafe.follow;

import com.example.fan_cafe.follow.application.FollowService;
import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowingListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    FollowRepository followRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    FollowPolicy followPolicy;

    @InjectMocks
    FollowService followService;

    @Test
    @DisplayName("User 가 존재하고, follow 를 하지 않았을 때 follow 가 저장된다.")
    void givenValidateUsersAndNotFollow_whenFollow_thenSave() {

        Long followerId = 1L;
        Long followingId = 2L;

        User follower = new User();
        ReflectionTestUtils.setField(follower, "id", 1L);

        User following = new User();
        ReflectionTestUtils.setField(following, "id", 2L);

        // given
        when(userRepository.countByIdIn(List.of(followerId, followingId))).thenReturn(2L);
        when(userRepository.getReferenceById(1L)).thenReturn(follower);
        when(userRepository.getReferenceById(2L)).thenReturn(following);


        // when
        followService.follow(followerId, followingId);

        // then
        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository, times(1)).save(captor.capture());
        Follow saved = captor.getValue();
        assertThat(saved.getFollower().getId()).isEqualTo(followerId);
        assertThat(saved.getFollowing().getId()).isEqualTo(followingId);
    }

    @Test
    @DisplayName("User 가 존재하지 않을때  오류가 반환된다")
    void givenInvalidateUsersAndNotFollow_whenFollow_thenThrowsError() {

        Long followerId = 1L;
        Long followingId = 2L;

        // given
        when(userRepository.countByIdIn(List.of(followerId, followingId))).thenReturn(1L);


        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            followService.follow(followerId, followingId);
        });

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("User 가 존재하고, follow가 존재할 때 follow 를 삭제한다.")
    void givenFollowExists_whenUnfollow_thenDelete() {

        Long followerId = 1L;
        Long followingId = 2L;

        User follower = spy(new User());
        ReflectionTestUtils.setField(follower, "id", followerId);

        User following = spy(new User());
        ReflectionTestUtils.setField(following, "id", followingId);

        Follow follow = Follow.create(follower, following);
        // given
        when(followRepository.findByFollower_IdAndFollowing_Id(followerId, followingId)).thenReturn(Optional.of(follow));

        // when
        followService.unfollow(followerId, followingId);

        // then
        verify(follower, times(1)).decreaseFollowingCount();
        verify(following, times(1)).decreaseFollowerCount();
        verify(followRepository, times(1)).delete(any(Follow.class));
    }

    @Test
    @DisplayName("Follow 정보가 존재하지 않을때  오류가 반환된다")
    void givenExistingFollow_whenUnfollow_thenThrowsError() {

        Long followerId = 1L;
        Long followingId = 2L;

        // given
        when(followRepository.findByFollower_IdAndFollowing_Id(followerId, followingId))
                .thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            followService.unfollow(followerId, followingId);
        });

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("커서가 존재할 경우 nextCursor가 생성된다.")
    void givenValidCursor_whenGetFollowingList_thenNextCursorIsCreated() {
        //given
        Long userId = 1L;
        int size = 5;
        Cursor cursor = new Cursor(7L, LocalDateTime.of(2025, 10, 23, 8, 8, 8));

        List<FollowResponse> follows = new ArrayList<>();
        for (long i = 6; i > 0; i--) {
            follows.add(new FollowResponse(i, 1L, "user1", "avatar_url", LocalDateTime.of(2025, 10, 24, 8, 8, 8), false));
        }
        when(followRepository.findNextFollowingPage(any(Cursor.class), anyInt(), anyLong())).thenReturn(follows);

        //when
        FollowingListResponse result = followService.getFollowingList(userId, cursor, size);

        //then
        assertThat(result.nextCursor()).isNotNull();
        assertThat(result.following()).hasSize(5);
    }


}
