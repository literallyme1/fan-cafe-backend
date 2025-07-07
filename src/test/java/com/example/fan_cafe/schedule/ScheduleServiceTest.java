package com.example.fan_cafe.schedule;

import com.example.fan_cafe.comment.domain.Comment;
import com.example.fan_cafe.schedule.application.ScheduleService;
import com.example.fan_cafe.schedule.domain.Schedule;
import com.example.fan_cafe.schedule.infrastructure.ScheduleRepository;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleListResponse;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleRequest;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    ScheduleService scheduleService;

    Schedule mockSchedule;

    @BeforeEach
    void setUp() {
        LocalDateTime start1 = LocalDateTime.of(2025, 7, 5, 10, 0);
        mockSchedule = Schedule.of("title1", "loc1", start1, start1.plusHours(1));
    }

    @Test
    void shouldCreateSchedule_WhenValidRequest() {
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

    @Test
    void shouldReturnGroupedSchedules_WhenSchedulesExistForMonth() {
        //given
        int year = 2025;
        int month = 7;

        LocalDateTime start1 = LocalDateTime.of(2025, 7, 5, 10, 0);
        LocalDateTime start2 = LocalDateTime.of(2025, 7, 5, 15, 0);
        LocalDateTime start3 = LocalDateTime.of(2025, 7, 10, 12, 0);

        Schedule s1 = Schedule.of("title1", "loc1", start1, start1.plusHours(1));
        Schedule s2 = Schedule.of("title2", "loc2", start2, start2.plusHours(2));
        Schedule s3 = Schedule.of("title3", "loc3", start3, start3.plusHours(1));

        List<Schedule> schedules = List.of(s1, s2, s3);
        when(scheduleRepository.findByStartAtBetween(any(), any())).thenReturn(schedules);

        //when
        var response = scheduleService.get(year, month);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        List<ScheduleListResponse> grouped = response.getData().getSchedules();

        assertEquals(2, grouped.size()); //2개의 그룹
        assertEquals(LocalDate.of(2025, 7, 5), grouped.getFirst().getDate());//날짜 맞는지 확인
        assertEquals(2, grouped.getFirst().getSchedules().size());

        //정렬 확인
        assertTrue(grouped.get(0).getDate().isBefore(grouped.get(1).getDate()));
    }

    @Test
    void shouldUpdateSchedule_WhenValidRequestIsGiven() {
        //given
        Long id = 1L;
        ScheduleRequest request = ScheduleRequest.builder()
                .title("수정된 스케줄")
                .location("수정된 장소")
                .startAt(LocalDateTime.of(2025, 7, 5, 11, 0))
                .build();
        when(scheduleRepository.findById(id)).thenReturn(Optional.of(mockSchedule));

        //when
        var response = scheduleService.update(id, request);

        //then
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals(mockSchedule.getTitle(), response.getData().getTitle());
    }

    @Test
    void shouldDeleteSchedule_WhenValidIdGiven() {

        //given
        Long id = 1L;
        when(scheduleRepository.findById(id)).thenReturn(Optional.of(mockSchedule));

        //when
        var response = scheduleService.delete(id);

        //then
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertNotNull(mockSchedule.getDeletedAt());
    }

}
