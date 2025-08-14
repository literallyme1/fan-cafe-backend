package com.example.fan_cafe.follow;

import com.example.fan_cafe.follow.application.FollowService;
import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowId;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowerItemResponse;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    FollowService followService;

    Long followerId;
    Long targetId;

    @BeforeEach
    void setUp() {
        followerId = 1L;
        targetId = 2L;
    }

    @Test
    void follow_shouldSave_whenNotExists() {
        // given
        when(followRepository.exists(followerId, targetId)).thenReturn(false);

        // when
        followService.follow(followerId, targetId);

        // then
        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository, times(1)).save(captor.capture());
        Follow saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId().getFollowerId()).isEqualTo(followerId);
        assertThat(saved.getId().getFollowingId()).isEqualTo(targetId);
    }

    @Test
    void follow_shouldThrow_whenAlreadyExists() {
        // given
        when(followRepository.exists(followerId, targetId)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> followService.follow(followerId, targetId))
                .isInstanceOf(CustomException.class); // 예외 타입 다르면 수정
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void unfollow_shouldDeleteById() {
        // when
        followService.unfollow(followerId, targetId);

        // then
        ArgumentCaptor<FollowId> captor = ArgumentCaptor.forClass(FollowId.class);
        verify(followRepository, times(1)).deleteById(captor.capture());
        FollowId deletedId = captor.getValue();
        assertThat(deletedId.getFollowerId()).isEqualTo(followerId);
        assertThat(deletedId.getFollowingId()).isEqualTo(targetId);
    }

    @Test
    void getFollowers_shouldReturnNoHasNext_whenSizeNotExceeded() {
        // given
        Long viewerId = 99L;
        LocalDateTime cursorAt = LocalDateTime.now();
        Long cursorId = 100L;
        int size = 3;

        List<FollowerItemResponse> returned = new ArrayList<>();
        returned.add(new FollowerItemResponse(10L, "nick1", "example.jpg",true, cursorAt.minusSeconds(1)));
        returned.add(new FollowerItemResponse(11L, "nick2","example.jpg", false, cursorAt.minusSeconds(2)));
        returned.add(new FollowerItemResponse(12L, "nick3","example.jpg", false, cursorAt.minusSeconds(3)));
        when(followRepository.findFollowers(eq(targetId), eq(viewerId), any(Cursor.class), eq(size)))
                .thenReturn(returned);

        // when
        FollowerListResponse resp = followService.getFollowers(targetId, viewerId, cursorAt, cursorId, size);

        // then
        assertThat(resp.hasNext()).isFalse();
        assertThat(resp.items()).hasSize(3);
        assertThat(resp.items()).extracting("userId")
                .containsExactly(10L, 11L, 12L);
    }

    @Test
    void getFollowers_shouldTrimAndSetHasNext_whenSizeExceeded() {
        // given
        Long viewerId = 99L;
        LocalDateTime cursorAt = LocalDateTime.now();
        Long cursorId = 100L;
        int size = 2;

        List<FollowerItemResponse> returned = new ArrayList<>();
        returned.add(new FollowerItemResponse(10L, "nick1", "example.jpg",true, cursorAt.minusSeconds(1)));
        returned.add(new FollowerItemResponse(11L, "nick2","example.jpg", false, cursorAt.minusSeconds(2)));
        returned.add(new FollowerItemResponse(12L, "nick3","example.jpg", false, cursorAt.minusSeconds(3))); // size 초과분

        when(followRepository.findFollowers(eq(targetId), eq(viewerId), any(Cursor.class), eq(size)))
                .thenReturn(returned);

        // when
        FollowerListResponse resp = followService.getFollowers(targetId, viewerId, cursorAt, cursorId, size);

        // then
        assertThat(resp.hasNext()).isTrue();                 // 초과 → hasNext=true
        assertThat(resp.items()).hasSize(2);                 // 잘라서 반환
        assertThat(resp.items()).extracting("userId")
                .containsExactly(10L, 11L);
    }
}
