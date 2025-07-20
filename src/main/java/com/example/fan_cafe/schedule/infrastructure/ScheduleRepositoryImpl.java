package com.example.fan_cafe.schedule.infrastructure;


import com.example.fan_cafe.global.common.SoftDeleteCondition;
import com.example.fan_cafe.schedule.domain.QSchedule;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ScheduleRepositoryImpl implements ScheduleRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    QSchedule schedule = QSchedule.schedule;

    @Override
    public List<ScheduleResponse> findByStartAtBetween(LocalDateTime start, LocalDateTime end){
        return queryFactory.select(Projections.constructor(ScheduleResponse.class,
                schedule.id,
                schedule.title,
                schedule.location,
                schedule.startAt,
                schedule.endAt
                ))
                .from(schedule)
                .where(SoftDeleteCondition.isNotDeleted(schedule.deletedAt),
                        schedule.startAt.between(start, end))
                .orderBy(schedule.startAt.asc())
                .fetch();
    }
}
