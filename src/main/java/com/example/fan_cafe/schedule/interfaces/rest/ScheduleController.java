package com.example.fan_cafe.schedule.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.schedule.application.ScheduleService;
import com.example.fan_cafe.schedule.interfaces.dto.MonthlyScheduleResponse;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleListResponse;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleRequest;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<ScheduleResponse> create(@RequestBody @Valid ScheduleRequest request) {
        return scheduleService.create(request);
    }

    public ApiResponse<MonthlyScheduleResponse> get(@RequestParam int year,
                                                    @RequestParam int month){
        return scheduleService.get(year, month);
    }
}
