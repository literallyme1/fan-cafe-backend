package com.example.fan_cafe.schedule.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.schedule.domain.Schedule;
import com.example.fan_cafe.schedule.exception.ScheduleErrorCode;
import com.example.fan_cafe.schedule.infrastructure.ScheduleRepository;
import com.example.fan_cafe.schedule.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ScheduleResponse create(ScheduleRequest request){
        Schedule schedule = Schedule.of(request.getTitle(),
                                        request.getLocation(),
                                        request.getStartAt(),
                                        request.getEndAt());
        scheduleRepository.save(schedule);
        return ScheduleResponse.from(schedule);
    }

    public MonthlyScheduleResponse get(int year, int month) {

        DateRange range = DateRange.ofMonth(year, month);
        List<ScheduleResponse> schedules = fetchSchedulesInRange(range);
        List<ScheduleListResponse> groupedSchedules = groupByDate(schedules);
        return MonthlyScheduleResponse.of(groupedSchedules);
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule schedule = findByIdOrThrow(id);
        schedule.update(request.getTitle(), request.getLocation(), request.getStartAt(), request.getEndAt());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long id) {
        Schedule schedule = findByIdOrThrow(id);
        schedule.delete();
    }

    private Schedule findByIdOrThrow(Long id){
        return scheduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
    }

    private List<ScheduleResponse> fetchSchedulesInRange(DateRange range) {
        return scheduleRepository.findByStartAtBetween(range.start(), range.end());
    }

    private List<ScheduleListResponse> groupByDate(List<ScheduleResponse> schedules) {
        return schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getStartAt().toLocalDate()))
                .entrySet().stream()
                .map(entry -> ScheduleListResponse.of(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ScheduleListResponse::getDate))
                .toList();
    }

}
