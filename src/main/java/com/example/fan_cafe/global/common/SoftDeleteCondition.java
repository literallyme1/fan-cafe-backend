package com.example.fan_cafe.global.common;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;

import java.time.LocalDateTime;

public class SoftDeleteCondition {

    public static BooleanExpression isNotDeleted(DateTimePath<LocalDateTime> deletedAt) {
        return deletedAt.isNull();
    }
}
