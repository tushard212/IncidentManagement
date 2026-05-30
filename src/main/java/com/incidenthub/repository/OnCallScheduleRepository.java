package com.incidenthub.repository;

import com.incidenthub.model.OnCallSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OnCallScheduleRepository extends JpaRepository<OnCallSchedule, Long> {

  List<OnCallSchedule> findByTeamIdAndActiveTrue(Long teamId);

  @Query("SELECT o FROM OnCallSchedule o WHERE o.team.id = :teamId AND o.active = true " +
      "AND o.startTime <= :now AND o.endTime >= :now")
  Optional<OnCallSchedule> findCurrentOnCall(@Param("teamId") Long teamId, @Param("now") LocalDateTime now);

  List<OnCallSchedule> findByUserId(Long userId);
}
