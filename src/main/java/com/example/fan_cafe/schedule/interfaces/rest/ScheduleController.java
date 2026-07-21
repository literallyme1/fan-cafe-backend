package com.example.fan_cafe.schedule.interfaces.rest;

import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.schedule.application.ScheduleService;
import com.example.fan_cafe.schedule.interfaces.dto.MonthlyScheduleResponse;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleListResponse;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleRequest;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Tag(name = "일정", description = "아티스트 일정 등록과 월별 조회")
public class ScheduleController {

    private final ScheduleService scheduleService;

//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "일정 등록", description = "아티스트 일정과 기간을 등록함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "일정 정보 오류")
    })
    public ApiResponse<ScheduleResponse> create(@RequestBody @Valid ScheduleRequest request) {
        return ApiResponse.success(ApiResponseStatus.CREATED, scheduleService.create(request));
    }

    @GetMapping
    @Operation(summary = "월별 일정 조회", description = "연도와 월을 기준으로 일정을 조회함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "연월 값 오류")
    })
    public ApiResponse<MonthlyScheduleResponse> get(@RequestParam int year,
                                                    @RequestParam int month){
        return ApiResponse.success(ApiResponseStatus.SUCCESS, scheduleService.get(year, month));
    }

    //    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "일정 수정", description = "일정 제목과 기간을 수정함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "일정 정보 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정 없음")
    })
    public ApiResponse<ScheduleResponse> update(@PathVariable Long id,
                                                @RequestBody @Valid ScheduleRequest request) {
        return ApiResponse.success(ApiResponseStatus.SUCCESS, scheduleService.update(id, request));
    }

    //    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "일정 삭제", description = "일정을 삭제 처리함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정 없음")
    })
    public void delete(@PathVariable Long id){
        scheduleService.delete(id);
    }
}
