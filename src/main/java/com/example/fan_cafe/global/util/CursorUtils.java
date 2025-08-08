package com.example.fan_cafe.global.util;

import com.example.fan_cafe.global.common.Cursor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
public class CursorUtils {

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
