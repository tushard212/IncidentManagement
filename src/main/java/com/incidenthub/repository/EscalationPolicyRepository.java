package com.incidenthub.repository;

import com.incidenthub.model.EscalationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EscalationPolicyRepository extends JpaRepository<EscalationPolicy, Long> {
  List<EscalationPolicy> findByTeamIdOrderByLevelAsc(Long teamId);

  Optional<EscalationPolicy> findByTeamIdAndLevel(Long teamId, int level);
}
