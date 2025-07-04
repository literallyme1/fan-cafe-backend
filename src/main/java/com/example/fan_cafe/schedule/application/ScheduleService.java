package com.example.fan_cafe.schedule.application;


import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.schedule.domain.Schedule;
import com.example.fan_cafe.schedule.infrastructure.ScheduleRepository;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleRequest;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ApiResponse<ScheduleResponse> create(ScheduleRequest request){
        Schedule schedule = Schedule.of(request.getTitle(),
                                        request.getLocation(),
                                        request.getStartAt(),
                                        request.getEndAt());
        scheduleRepository.save(schedule);
        return ApiResponse.success(ApiResponseStatus.CREATED, ScheduleResponse.from(schedule));
    }
}
