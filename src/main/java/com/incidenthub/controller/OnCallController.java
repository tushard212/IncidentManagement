package com.incidenthub.controller;

import com.incidenthub.dto.OnCallDto;
import com.incidenthub.service.OnCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oncall")
@RequiredArgsConstructor
public class OnCallController {

  private final OnCallService onCallService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<OnCallDto.Response> createSchedule(@RequestBody OnCallDto.CreateRequest request) {
    return ResponseEntity.ok(onCallService.createSchedule(request));
  }

  @GetMapping("/team/{teamId}")
  public ResponseEntity<List<OnCallDto.Response>> getTeamSchedules(@PathVariable Long teamId) {
    return ResponseEntity.ok(onCallService.getTeamSchedules(teamId));
  }

  @GetMapping("/team/{teamId}/current")
  public ResponseEntity<OnCallDto.Response> getCurrentOnCall(@PathVariable Long teamId) {
    return ResponseEntity.ok(onCallService.getCurrentOnCall(teamId));
  }
}
