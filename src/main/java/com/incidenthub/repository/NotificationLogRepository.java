package com.incidenthub.repository;

import com.incidenthub.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

  Page<NotificationLog> findAllByOrderBySentAtDesc(Pageable pageable);

  long countByStatus(NotificationLog.NotificationStatus status);

  @Query("SELECT COUNT(n) FROM NotificationLog n WHERE n.sentAt >= :since")
  long countSince(java.time.LocalDateTime since);

  @Query("SELECT COUNT(n) FROM NotificationLog n WHERE n.status = 'FAILED' AND n.sentAt >= :since")
  long countFailedSince(java.time.LocalDateTime since);
}
