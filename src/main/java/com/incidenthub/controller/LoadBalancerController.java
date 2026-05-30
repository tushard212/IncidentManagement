package com.incidenthub.controller;

import com.incidenthub.config.LoadBalancerHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for managing load balancer behavior.
 * Supports graceful drain/resume for rolling deployments.
 */
@RestController
@RequestMapping("/api/admin/lb")
@RequiredArgsConstructor
public class LoadBalancerController {

  private final LoadBalancerHealthIndicator healthIndicator;

  @PostMapping("/drain")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> drain() {
    healthIndicator.drain();
    return ResponseEntity.ok(Map.of("status", "draining", "message", "Instance will stop accepting new traffic"));
  }

  @PostMapping("/resume")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> resume() {
    healthIndicator.resume();
    return ResponseEntity.ok(Map.of("status", "active", "message", "Instance is now accepting traffic"));
  }
}
