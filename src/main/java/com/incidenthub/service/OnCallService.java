package com.incidenthub.service;

import com.incidenthub.dto.OnCallDto;
import com.incidenthub.model.OnCallSchedule;
import com.incidenthub.model.Team;
import com.incidenthub.model.User;
import com.incidenthub.repository.OnCallScheduleRepository;
import com.incidenthub.repository.TeamRepository;
import com.incidenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnCallService {

  private final OnCallScheduleRepository scheduleRepository;
  private final UserRepository userRepository;
  private final TeamRepository teamRepository;

  @Transactional
  public OnCallDto.Response createSchedule(OnCallDto.CreateRequest request) {
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> new RuntimeException("User not found"));
    Team team = teamRepository.findById(request.getTeamId())
        .orElseThrow(() -> new RuntimeException("Team not found"));

    OnCallSchedule schedule = OnCallSchedule.builder()
        .user(user)
        .team(team)
        .startTime(request.getStartTime())
        .endTime(request.getEndTime())
        .active(true)
        .build();

    schedule = scheduleRepository.save(schedule);

    // Update user's on-call status
    user.setOnCall(true);
    userRepository.save(user);

    return mapToResponse(schedule);
  }

  public List<OnCallDto.Response> getTeamSchedules(Long teamId) {
    return scheduleRepository.findByTeamIdAndActiveTrue(teamId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public OnCallDto.Response getCurrentOnCall(Long teamId) {
    OnCallSchedule schedule = scheduleRepository.findCurrentOnCall(teamId, LocalDateTime.now())
        .orElseThrow(() -> new RuntimeException("No one is currently on-call for this team"));
    return mapToResponse(schedule);
  }

  private OnCallDto.Response mapToResponse(OnCallSchedule schedule) {
    return OnCallDto.Response.builder()
        .id(schedule.getId())
        .userName(schedule.getUser().getFullName())
        .userId(schedule.getUser().getId())
        .teamName(schedule.getTeam().getName())
        .teamId(schedule.getTeam().getId())
        .startTime(schedule.getStartTime())
        .endTime(schedule.getEndTime())
        .active(schedule.isActive())
        .build();
  }
}
