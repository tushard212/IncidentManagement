package com.incidenthub.repository;

import com.incidenthub.model.IncidentTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentTimelineRepository extends JpaRepository<IncidentTimeline, Long> {
  List<IncidentTimeline> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

  void deleteByIncidentId(Long incidentId);
}
