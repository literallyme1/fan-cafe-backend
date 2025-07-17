package com.example.fan_cafe.schedule.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
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

//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<ScheduleResponse> create(@RequestBody @Valid ScheduleRequest request) {
        return ApiResponse.success(ApiResponseStatus.CREATED, scheduleService.create(request));
    }

    @GetMapping
    public ApiResponse<MonthlyScheduleResponse> get(@RequestParam int year,
                                                    @RequestParam int month){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, scheduleService.get(year, month));
    }

    //    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<ScheduleResponse> update(@PathVariable Long id,
                                                @RequestBody @Valid ScheduleRequest request) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, scheduleService.update(id, request));
    }

    //    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        scheduleService.delete(id);
    }
}
