package com.incidenthub.controller;

import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.AuditLog;
import com.incidenthub.service.AnalyticsService;
import com.incidenthub.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API v2 - Enhanced endpoints with analytics, audit trail, and improved
 * responses.
 * Demonstrates: API Versioning via URI path strategy.
 */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Tag(name = "API v2", description = "Enhanced API with analytics and audit trail")
public class ApiV2Controller {

  private final AnalyticsService analyticsService;
  private final AuditService auditService;

  @GetMapping("/analytics")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Get incident analytics", description = "Returns MTTR, MTTA, SLA compliance, and breakdown charts")
  public ResponseEntity<IncidentDto.AnalyticsResponse> getAnalytics(
      @RequestParam(defaultValue = "30") int days) {
    return ResponseEntity.ok(analyticsService.getAnalytics(days));
  }

  @GetMapping("/audit")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all audit logs")
  public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(auditService.getAllAudits(pageable));
  }

  @GetMapping("/audit/entity/{entityType}/{entityId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Get audit logs for a specific entity")
  public ResponseEntity<Page<AuditLog>> getAuditByEntity(
      @PathVariable String entityType,
      @PathVariable Long entityId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(auditService.getAuditsByEntity(entityType, entityId, pageable));
  }

  @GetMapping("/audit/user/{username}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @Operation(summary = "Get audit logs for a specific user")
  public ResponseEntity<Page<AuditLog>> getAuditByUser(
      @PathVariable String username,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(auditService.getAuditsByUser(username, pageable));
  }
}
