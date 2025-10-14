package com.example.fan_cafe.global;

import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.HasCreatedAt;
import com.example.fan_cafe.global.util.CursorUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class CursorUtilsTest {

    @Getter
    @AllArgsConstructor
    static class TestEntity implements HasCreatedAt {
        private final Long id;
        LocalDateTime createdAt;
    }

    @Test
    @DisplayName("리스트의 마지막 요소 기준으로 커서가 반환된다.")
    void givenList_whenFromLast_thenReturnsCursorFromLastElement(){

        //given
        List<TestEntity> list = List.of(
                new TestEntity(1L, LocalDateTime.of(2025, 10, 14, 13, 23, 0)),
                new TestEntity(2L, LocalDateTime.of(2025, 10, 14, 13, 24, 0))
        );

        //when
        Cursor result = CursorUtils.fromLast(list);

        //then
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.at()).isEqualTo(LocalDateTime.of(2025, 10, 14, 13, 24, 0));

    }

    @Test
    @DisplayName("리스트가 비어있으면 null 을 반호나한다.")
    void givenEmptyList_whenFromLast_thenReturnsNull(){
        //given
        List<TestEntity> empty = List.of();

        //when
        Cursor cursor = CursorUtils.fromLast(empty);

        //then
        assertThat(cursor).isNull();

    }
}
