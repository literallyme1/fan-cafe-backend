package com.example.fan_cafe.schedule.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.schedule.exception.ScheduleErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "schedules")
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String location;

    @Column(updatable = false)
    private LocalDateTime startAt;

    @Column
    private LocalDateTime endAt;

    public static Schedule of(String title, String location, LocalDateTime startAt, LocalDateTime endAt) {
        Schedule schedule = Schedule.builder()
                .title(title)
                .location(location)
                .startAt(startAt)
                .endAt(endAt)
                .build();
        schedule.validateTime();
        return schedule;
    }

    public void validateTime() {
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new CustomException(ScheduleErrorCode.SCHEDULE_INVALID_TIME);
        }
    }


}
