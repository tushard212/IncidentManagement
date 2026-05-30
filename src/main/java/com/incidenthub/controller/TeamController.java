package com.incidenthub.controller;

import com.incidenthub.dto.TeamDto;
import com.incidenthub.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<TeamDto.Response> createTeam(@RequestBody TeamDto.CreateRequest request) {
    return ResponseEntity.ok(teamService.createTeam(request));
  }

  @GetMapping
  public ResponseEntity<List<TeamDto.Response>> getAllTeams() {
    return ResponseEntity.ok(teamService.getAllTeams());
  }

  @GetMapping("/{id}")
  public ResponseEntity<TeamDto.Response> getTeam(@PathVariable Long id) {
    return ResponseEntity.ok(teamService.getTeam(id));
  }

  @PostMapping("/{teamId}/members/{userId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<TeamDto.Response> addMember(@PathVariable Long teamId, @PathVariable Long userId) {
    return ResponseEntity.ok(teamService.addMember(teamId, userId));
  }
}
