package com.example.fan_cafe.schedule;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.schedule.application.ScheduleService;
import com.example.fan_cafe.schedule.domain.Schedule;
import com.example.fan_cafe.schedule.infrastructure.ScheduleRepository;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleListResponse;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleRequest;
import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());

        Schedule saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo(request.getTitle());
        assertThat(saved.getLocation()).isEqualTo(request.getLocation());
    }

    @Test
    void shouldThrowException_WhenStartAtIsAfterEndAt() {
        // given
        ScheduleRequest request = ScheduleRequest.builder()
                .title("시간 오류")
                .location("오류 장소")
                .startAt(LocalDateTime.of(2025, 7, 10, 20, 0))
                .endAt(LocalDateTime.of(2025, 7, 10, 19, 0))
                .build();

        // when & then
        assertThrows(CustomException.class, () -> scheduleService.create(request));
    }

    @Test
    void shouldCreateSchedule_WhenEndAtIsNull() {
        // given
        ScheduleRequest request = ScheduleRequest.builder()
                .title("종료 시간 없음")
                .location("장소")
                .startAt(LocalDateTime.of(2025, 7, 20, 14, 0))
                .build(); // endAt 생략

        // when
        var response = scheduleService.create(request);

        // then
        assertThat(response.getEndAt()).isNull();
    }

    private List<ScheduleResponse> mockGroupedSchedules() {
        LocalDateTime start1 = LocalDateTime.of(2025, 7, 5, 10, 0);
        LocalDateTime start2 = LocalDateTime.of(2025, 7, 5, 15, 0);
        LocalDateTime start3 = LocalDateTime.of(2025, 7, 10, 12, 0);

        return List.of(
                ScheduleResponse.from(Schedule.of("title1", "loc1", start1, start1.plusHours(1))),
                ScheduleResponse.from(Schedule.of("title2", "loc2", start2, start2.plusHours(2))),
                ScheduleResponse.from(Schedule.of("title3", "loc3", start3, start3.plusHours(1)))
        );
    }

    @Test
    void shouldReturnGroupedSchedules_WhenSchedulesExistForMonth() {
        //given
        int year = 2025;
        int month = 7;

        List<ScheduleResponse> schedules = mockGroupedSchedules();
        when(scheduleRepository.findByStartAtBetween(any(), any())).thenReturn(schedules);

        //when
        var response = scheduleService.get(year, month);

        List<ScheduleListResponse> grouped = response.getSchedules();

        assertThat(response.getSchedules()).hasSize(2);
        assertThat(grouped.getFirst().getDate()).isEqualTo(LocalDate.of(2025, 7, 5));

        assertThat(grouped.get(0).getSchedules()).hasSize(2); // 7월 5일 2건
        assertThat(grouped.get(1).getSchedules()).hasSize(1); // 7월 10일 1건

        //정렬 확인
        assertThat(grouped.get(0).getDate()).isBefore(grouped.get(1).getDate());
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
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getStartAt()).isEqualTo(request.getStartAt());
    }

    @Test
    void shouldThrowException_WhenUpdateScheduleWithInvalidId() {
        // given
        Long invalidId = 999L;
        ScheduleRequest request = ScheduleRequest.builder()
                .title("수정 시도")
                .startAt(LocalDateTime.of(2025, 7, 1, 12, 0))
                .build();

        when(scheduleRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CustomException.class, () -> scheduleService.update(invalidId, request));
    }

    @Test
    void shouldDeleteSchedule_WhenValidIdGiven() {

        //given
        Long id = 1L;
        when(scheduleRepository.findById(id)).thenReturn(Optional.of(mockSchedule));

        //when
        scheduleService.delete(id);

        //then
        assertThat(mockSchedule.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldThrowException_WhenDeleteScheduleWithInvalidId() {
        // given
        Long invalidId = 404L;
        when(scheduleRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CustomException.class, () -> scheduleService.delete(invalidId));
    }


}
