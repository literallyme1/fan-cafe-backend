package com.example.fan_cafe.global.util;

import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.HasCreatedAt;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
public class CursorUtils {

    //조회한 글 중 가장 최신 글을 찾아 AfterCursor 를 생성
    public static <T extends HasCreatedAt> Cursor fromFirst(List<T> list){
        if(list == null ||list.isEmpty()){
            return null;
        }
        T first = list.getFirst();
        return new Cursor(first.getId(), first.getCreatedAt());
    }
    //조회한 글 중 가장 오래된 글을 통해 BeforeCursor 를 생성
    public static <T extends HasCreatedAt> Cursor fromLast(List<T> list){
        if(list == null ||list.isEmpty()){
            return null;
        }
        T last = list.getLast();
        return new Cursor(last.getId(), last.getCreatedAt());
    }
    //cursor 기준 최신 정보 가져오는 where 절
    public static BooleanExpression beforeDesc(
            DateTimePath<LocalDateTime> createdAtPath,
            NumberPath<Long> idPath,
            Cursor cursor
    ){
        if (cursor == null || cursor.at() == null || cursor.id() == null) return null;
        return createdAtPath.lt(cursor.at())
                .or(createdAtPath.eq(cursor.at()).and(idPath.lt(cursor.id())));
    }

    public static BooleanExpression beforeAsc(
            DateTimePath<LocalDateTime> createdAtPath,
            NumberPath<Long> idPath,
            Cursor cursor
    ){
        if (cursor == null || cursor.at() == null || cursor.id() == null) return null;
        return createdAtPath.gt(cursor.at())
                .or(createdAtPath.eq(cursor.at()).and(idPath.gt(cursor.id())));
    }
}
