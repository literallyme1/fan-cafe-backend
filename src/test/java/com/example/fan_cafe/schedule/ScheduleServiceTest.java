package com.example.fan_cafe.schedule;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.schedule.application.ScheduleService;
import com.example.fan_cafe.schedule.domain.Schedule;
import com.example.fan_cafe.schedule.infrastructure.ScheduleRepository;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    ScheduleService scheduleService;

    @Test
    void create_success() {
        //given
        ScheduleRequest request = ScheduleRequest.builder()
                                    .title("인사동 치맥 축제")
                                    .location("인사동 중앙공원")
                                    .startAt(LocalDateTime.parse("2025-07-10T19:30:00"))
                                    .endAt(LocalDateTime.parse("2025-07-10T20:30:00"))
                                    .build();

        //when
        var response = scheduleService.create(request);

        //then
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

}
