package com.incidenthub.repository;

import com.incidenthub.model.Incident;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.model.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

  Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

  Page<Incident> findBySeverity(Severity severity, Pageable pageable);

  Page<Incident> findByAssigneeId(Long assigneeId, Pageable pageable);

  Page<Incident> findByTeamId(Long teamId, Pageable pageable);

  List<Incident> findByStatusInAndSlaDeadlineBefore(List<IncidentStatus> statuses, LocalDateTime deadline);

  @Query("SELECT i FROM Incident i WHERE i.status IN :statuses AND i.slaBreached = false AND i.slaDeadline < :now")
  List<Incident> findBreachedIncidents(@Param("statuses") List<IncidentStatus> statuses,
      @Param("now") LocalDateTime now);

  @Query("SELECT i FROM Incident i WHERE i.status = :status AND i.createdAt > :since AND i.assignee IS NULL")
  List<Incident> findUnassignedIncidentsSince(@Param("status") IncidentStatus status,
      @Param("since") LocalDateTime since);

  @Query("SELECT COUNT(i) FROM Incident i WHERE i.status = :status")
  long countByStatus(@Param("status") IncidentStatus status);

  @Query("SELECT i.severity, COUNT(i) FROM Incident i GROUP BY i.severity")
  List<Object[]> countBySeverity();

  @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY " +
      "CASE i.severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END")
  List<Incident> findActiveIncidentsBySeverityPriority();
}
