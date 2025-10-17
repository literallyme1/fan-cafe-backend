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

    public static <T extends HasCreatedAt> Cursor fromLast(List<T> list){
        if(list == null ||list.isEmpty()){
            return null;
        }
        T last = list.getLast();
        return new Cursor(last.getId(), last.getCreatedAt());
    }
    //cursor 기준 이전 정보를 불러옴.
    public static <T extends Comparable<?>> BooleanExpression beforeDesc(
            DateTimePath<LocalDateTime> createdAtPath,
            NumberPath<Long> idPath,
            Cursor cursor
    ){
        if (cursor == null || cursor.at() == null || cursor.id() == null) return null;
        return createdAtPath.lt(cursor.at())
                .or(createdAtPath.eq(cursor.at()).and(idPath.lt(cursor.id())));
    }
    //cursor 기준 이전 정보를 오래된 순으로 보여줌.
    public static <T extends Comparable<?>> BooleanExpression beforeAsc(
            DateTimePath<LocalDateTime> createdAtPath,
            NumberPath<Long> idPath,
            Cursor cursor
    ){
        if (cursor == null || cursor.at() == null || cursor.id() == null) return null;
        return createdAtPath.gt(cursor.at())
                .or(createdAtPath.eq(cursor.at()).and(idPath.gt(cursor.id())));
    }

    //cursor 기준 최신 정보를 최신순(Desc)으로 보여줌.
    public static <T extends Comparable<?>> BooleanExpression afterDesc(
            DateTimePath<LocalDateTime> createdAtPath,
            NumberPath<Long> idPath,
            Cursor cursor
    ){
        if (cursor == null || cursor.id() == null || cursor.at() == null) return null;
        return createdAtPath.gt(cursor.at())
                .or(createdAtPath.eq(cursor.at()).and(idPath.gt(cursor.id())));
    }
}
