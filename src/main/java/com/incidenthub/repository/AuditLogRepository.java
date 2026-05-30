package com.incidenthub.repository;

import com.incidenthub.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId, Pageable pageable);

  Page<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy, Pageable pageable);

  Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

  List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

  long countByEntityType(String entityType);
}
