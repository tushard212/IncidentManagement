package com.incidenthub.controller;

import com.incidenthub.model.NotificationLog;
import com.incidenthub.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationLogRepository notificationLogRepository;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Page<NotificationLog>> getNotifications(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Page<NotificationLog> notifications = notificationLogRepository
        .findAllByOrderBySentAtDesc(PageRequest.of(page, size));
    return ResponseEntity.ok(notifications);
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Map<String, Object>> getNotificationStats() {
    LocalDateTime last24h = LocalDateTime.now().minusHours(24);
    LocalDateTime last7d = LocalDateTime.now().minusDays(7);

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalSent", notificationLogRepository.countByStatus(NotificationLog.NotificationStatus.SENT));
    stats.put("totalFailed", notificationLogRepository.countByStatus(NotificationLog.NotificationStatus.FAILED));
    stats.put("last24h", notificationLogRepository.countSince(last24h));
    stats.put("failedLast7d", notificationLogRepository.countFailedSince(last7d));

    return ResponseEntity.ok(stats);
  }
}
