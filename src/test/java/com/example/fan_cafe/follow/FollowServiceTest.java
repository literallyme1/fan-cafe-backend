package com.example.fan_cafe.follow;

import com.example.fan_cafe.follow.application.FollowService;
import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
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
    void givenValidateUserAndNotFollow_whenFollow_thenSave() {

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
        assertThat(saved.getId().getFollowerId()).isEqualTo(followerId);
        assertThat(saved.getId().getFollowingId()).isEqualTo(followingId);
    }

//    @Test
//    void follow_shouldThrow_whenAlreadyExists() {
//        // given
//        when(followRepository.exists(followerId, targetId)).thenReturn(true);
//
//        // when / then
//        assertThatThrownBy(() -> followService.follow(followerId, targetId))
//                .isInstanceOf(CustomException.class); // 예외 타입 다르면 수정
//        verify(followRepository, never()).save(any(Follow.class));
//    }
//
//    @Test
//    void unfollow_shouldDeleteById() {
//        // when
//        followService.unfollow(followerId, targetId);
//
//        // then
//        ArgumentCaptor<FollowId> captor = ArgumentCaptor.forClass(FollowId.class);
//        verify(followRepository, times(1)).deleteById(captor.capture());
//        FollowId deletedId = captor.getValue();
//        assertThat(deletedId.getFollowerId()).isEqualTo(followerId);
//        assertThat(deletedId.getFollowingId()).isEqualTo(targetId);
//    }
//
//    @Test
//    void getFollowers_shouldReturnNoHasNext_whenSizeNotExceeded() {
//        // given
//        Long viewerId = 99L;
//        LocalDateTime cursorAt = LocalDateTime.now();
//        Long cursorId = 100L;
//        int size = 3;
//
//        List<FollowerItemResponse> returned = new ArrayList<>();
//        returned.add(new FollowerItemResponse(10L, "nick1", "example.jpg",true, cursorAt.minusSeconds(1)));
//        returned.add(new FollowerItemResponse(11L, "nick2","example.jpg", false, cursorAt.minusSeconds(2)));
//        returned.add(new FollowerItemResponse(12L, "nick3","example.jpg", false, cursorAt.minusSeconds(3)));
//        when(followRepository.findFollowers(eq(targetId), eq(viewerId), any(Cursor.class), eq(size)))
//                .thenReturn(returned);
//
//        // when
//        FollowerListResponse resp = followService.getFollowers(targetId, viewerId, cursorAt, cursorId, size);
//
//        // then
//        assertThat(resp.hasNext()).isFalse();
//        assertThat(resp.items()).hasSize(3);
//        assertThat(resp.items()).extracting("userId")
//                .containsExactly(10L, 11L, 12L);
//    }
//
//    @Test
//    void getFollowers_shouldTrimAndSetHasNext_whenSizeExceeded() {
//        // given
//        Long viewerId = 99L;
//        LocalDateTime cursorAt = LocalDateTime.now();
//        Long cursorId = 100L;
//        int size = 2;
//
//        List<FollowerItemResponse> returned = new ArrayList<>();
//        returned.add(new FollowerItemResponse(10L, "nick1", "example.jpg",true, cursorAt.minusSeconds(1)));
//        returned.add(new FollowerItemResponse(11L, "nick2","example.jpg", false, cursorAt.minusSeconds(2)));
//        returned.add(new FollowerItemResponse(12L, "nick3","example.jpg", false, cursorAt.minusSeconds(3))); // size 초과분
//
//        when(followRepository.findFollowers(eq(targetId), eq(viewerId), any(Cursor.class), eq(size)))
//                .thenReturn(returned);
//
//        // when
//        FollowerListResponse resp = followService.getFollowers(targetId, viewerId, cursorAt, cursorId, size);
//
//        // then
//        assertThat(resp.hasNext()).isTrue();                 // 초과 → hasNext=true
//        assertThat(resp.items()).hasSize(2);                 // 잘라서 반환
//        assertThat(resp.items()).extracting("userId")
//                .containsExactly(10L, 11L);
//    }
}
