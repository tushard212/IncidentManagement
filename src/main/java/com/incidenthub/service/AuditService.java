package com.incidenthub.service;

import com.incidenthub.model.AuditLog;
import com.incidenthub.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  /**
   * Log an audit event asynchronously with retry on failure.
   * Demonstrates: @Async + @Retryable + Exponential Backoff
   */
  @Async
  @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logAction(String entityType, Long entityId, String action,
      String performedBy, String oldValue, String newValue, String changes) {
    String ipAddress = getClientIp();

    AuditLog auditLog = AuditLog.builder()
        .entityType(entityType)
        .entityId(entityId)
        .action(action)
        .performedBy(performedBy)
        .oldValue(oldValue)
        .newValue(newValue)
        .changes(changes)
        .ipAddress(ipAddress)
        .timestamp(LocalDateTime.now())
        .build();

    auditLogRepository.save(auditLog);
    log.debug("Audit logged: {} {} {} by {}", action, entityType, entityId, performedBy);
  }

  @Transactional(readOnly = true)
  public Page<AuditLog> getAuditsByEntity(String entityType, Long entityId, Pageable pageable) {
    return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId, pageable);
  }

  @Transactional(readOnly = true)
  public Page<AuditLog> getAuditsByUser(String username, Pageable pageable) {
    return auditLogRepository.findByPerformedByOrderByTimestampDesc(username, pageable);
  }

  @Transactional(readOnly = true)
  public Page<AuditLog> getAllAudits(Pageable pageable) {
    return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
  }

  private String getClientIp() {
    try {
      ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        HttpServletRequest request = attrs.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
          return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
      }
    } catch (Exception e) {
      log.debug("Could not resolve client IP");
    }
    return "system";
  }
}
