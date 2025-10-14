package com.example.fan_cafe.global;

import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.CursorResolver;
import com.example.fan_cafe.global.common.HasCreatedAt;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThat;

public class CursorResolverTest {

    @Getter
    @AllArgsConstructor
    static class TestEntity implements HasCreatedAt {
        private final Long id;
        LocalDateTime createdAt;
    }

    @Test
    @DisplayName("커서 2개가 다 잘 주어진 경우, 그대로 반환")
    void givenCursorValues_whenResolve_thenReturnsSameCursor(){

        //given
        Long cursorId = 1L;
        LocalDateTime cursorCreatedAt = LocalDateTime.of(2025, 10, 14, 13, 23, 0);

        //when
        Cursor result = CursorResolver.resolve(cursorId, cursorCreatedAt, Optional::empty);

        //then
        assertThat(result.id()).isEqualTo(cursorId);
        assertThat(result.at()).isEqualTo(cursorCreatedAt);

    }

    @Test
    @DisplayName("커서는 없지만 latest 가 존재할 때 latest 기준 커서가 반환된다.")
    void givenNoCursorButLatestExists_whenResolve_thenReturnsLatestCursor(){

        //given
        TestEntity latest = new TestEntity(1L,LocalDateTime.of(2025, 10, 14, 13, 23, 0));
        Supplier<Optional<? extends HasCreatedAt>> latestSupplier = () -> Optional.of(latest);

        //when
        Cursor result = CursorResolver.resolve(null, null, latestSupplier);

        //then
        assertThat(result.id()).isEqualTo(latest.getId() + 1);
        assertThat(result.at()).isEqualTo(latest.getCreatedAt().plusNanos(1));
    }

    @Test
    @DisplayName("커서와, latest 가 없을 시 Max Value 커서가 반환된다.")
    void givenNoCursorAndNoLatest_whenResolve_thenReturnsMaxCursor(){

        //when
        Cursor result = CursorResolver.resolve(null, null, Optional::empty);

        //then
        assertThat(result.id()).isEqualTo(Long.MAX_VALUE);
        assertThat(result.at()).isNotNull();
    }



}
