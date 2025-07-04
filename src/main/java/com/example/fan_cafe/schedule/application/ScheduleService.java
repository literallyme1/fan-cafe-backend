package com.example.fan_cafe.schedule.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.schedule.domain.Schedule;
import com.example.fan_cafe.schedule.exception.ScheduleErrorCode;
import com.example.fan_cafe.schedule.infrastructure.ScheduleRepository;
import com.example.fan_cafe.schedule.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ApiResponse<ScheduleResponse> create(ScheduleRequest request){
        Schedule schedule = Schedule.of(request.getTitle(),
                                        request.getLocation(),
                                        request.getStartAt(),
                                        request.getEndAt());
        scheduleRepository.save(schedule);
        return ApiResponse.success(ApiResponseStatus.CREATED, ScheduleResponse.from(schedule));
    }

    public ApiResponse<MonthlyScheduleResponse> get(int year, int month) {

        DateRange range = DateRange.ofMonth(year, month);

        List<Schedule> schedules = scheduleRepository.findByStartAtBetween(range.start(), range.end());

        Map<LocalDate, List<ScheduleResponse>> grouped = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.groupingBy(s -> s.getStartAt().toLocalDate()));

        List<ScheduleListResponse> groupedSchedules  = grouped.entrySet().stream()
                .map(entry -> ScheduleListResponse.of(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ScheduleListResponse::getDate))
                .toList();

        return ApiResponse.success(ApiResponseStatus.SUCCESS, MonthlyScheduleResponse.of(groupedSchedules));
    }

    @Transactional
    public ApiResponse<ScheduleResponse> update(Long id, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
        schedule.update(request.getTitle(), request.getLocation(), request.getStartAt(), request.getEndAt());
        return ApiResponse.success(ApiResponseStatus.SUCCESS, ScheduleResponse.from(schedule));
    }

    @Transactional
    public ApiResponse<Void> delete(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
        schedule.delete();

        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
