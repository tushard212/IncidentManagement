package com.incidenthub.controller;

import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.ratelimiter.RateLimit;
import com.incidenthub.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

  private final IncidentService incidentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
  @RateLimit(maxRequests = 10, windowSeconds = 60, keyType = RateLimit.KeyType.USER)
  public ResponseEntity<IncidentDto.Response> createIncident(
      @Valid @RequestBody IncidentDto.CreateRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(incidentService.createIncident(request, authentication.getName()));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
  @RateLimit(maxRequests = 100, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
  public ResponseEntity<Page<IncidentDto.Response>> getAllIncidents(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(incidentService.getAllIncidents(pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
  public ResponseEntity<IncidentDto.Response> getIncident(@PathVariable Long id) {
    return ResponseEntity.ok(incidentService.getIncident(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @incidentAccessService.isAssignedOrReporter(#id, authentication.name)")
  public ResponseEntity<IncidentDto.Response> updateIncident(
      @PathVariable Long id,
      @Valid @RequestBody IncidentDto.UpdateRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(incidentService.updateIncident(id, request, authentication.getName()));
  }

  @PostMapping("/{id}/acknowledge")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
  public ResponseEntity<IncidentDto.Response> acknowledgeIncident(
      @PathVariable Long id,
      Authentication authentication) {
    return ResponseEntity.ok(incidentService.acknowledgeIncident(id, authentication.getName()));
  }

  @PostMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @incidentAccessService.isAssignedOrReporter(#id, authentication.name)")
  public ResponseEntity<IncidentDto.Response> updateStatus(
      @PathVariable Long id,
      @RequestParam IncidentStatus status,
      Authentication authentication) {
    return ResponseEntity.ok(incidentService.updateIncidentStatus(id, status, authentication.getName()));
  }

  @PostMapping("/{id}/notes")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @incidentAccessService.isAssignedOrReporter(#id, authentication.name)")
  public ResponseEntity<IncidentDto.Response> addNote(
      @PathVariable Long id,
      @Valid @RequestBody IncidentDto.AddNoteRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(incidentService.addNote(id, request.getMessage(), authentication.getName()));
  }

  @GetMapping("/status/{status}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
  public ResponseEntity<Page<IncidentDto.Response>> getByStatus(
      @PathVariable IncidentStatus status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(incidentService.getIncidentsByStatus(status, pageable));
  }

  @GetMapping("/assignee/{assigneeId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @incidentAccessService.isSameUser(#assigneeId, authentication.name)")
  public ResponseEntity<Page<IncidentDto.Response>> getByAssignee(
      @PathVariable Long assigneeId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(incidentService.getIncidentsByAssignee(assigneeId, pageable));
  }

  @GetMapping("/team/{teamId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @incidentAccessService.isTeamMember(#teamId, authentication.name)")
  public ResponseEntity<Page<IncidentDto.Response>> getByTeam(
      @PathVariable Long teamId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(incidentService.getIncidentsByTeam(teamId, pageable));
  }

  @GetMapping("/dashboard/stats")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
  @RateLimit(maxRequests = 60, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
  public ResponseEntity<IncidentDto.DashboardStats> getDashboardStats() {
    return ResponseEntity.ok(incidentService.getDashboardStats());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Map<String, String>> deleteIncident(
      @PathVariable Long id,
      Authentication authentication) {
    incidentService.deleteIncident(id, authentication.getName());
    return ResponseEntity.ok(Map.of("message", "Incident deleted successfully"));
  }
}
