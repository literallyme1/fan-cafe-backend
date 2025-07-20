package com.example.fan_cafe.schedule.infrastructure;

import com.example.fan_cafe.schedule.domain.Schedule;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, ScheduleRepositoryCustom {

    @NonNull
    Optional<Schedule> findByIdAndDeletedAtIsNull(Long id);

}
