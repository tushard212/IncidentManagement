package com.incidenthub.controller;

import com.incidenthub.dto.OnCallDto;
import com.incidenthub.service.OnCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oncall")
@RequiredArgsConstructor
@Tag(name = "On-Call", description = "On-call rotation schedules per team")
public class OnCallController {

  private final OnCallService onCallService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Create on-call schedule", description = "Create a new on-call rotation for a team")
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
