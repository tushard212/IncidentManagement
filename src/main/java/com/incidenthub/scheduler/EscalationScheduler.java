package com.incidenthub.scheduler;

import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.EscalationPolicy;
import com.incidenthub.model.Incident;
import com.incidenthub.model.IncidentTimeline;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.repository.EscalationPolicyRepository;
import com.incidenthub.repository.IncidentRepository;
import com.incidenthub.repository.IncidentTimelineRepository;
import com.incidenthub.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

  private final IncidentRepository incidentRepository;
  private final EscalationPolicyRepository escalationPolicyRepository;
  private final IncidentTimelineRepository timelineRepository;
  private final WebSocketNotificationService notificationService;

  private final ExecutorService executorService = Executors.newFixedThreadPool(4);

  /**
   * Runs every 60 seconds to check for SLA breaches and escalate incidents.
   * Demonstrates: Batch processing + Multithreading + Scheduled jobs
   */
  @Scheduled(fixedRate = 60000)
  @Transactional
  public void checkAndEscalateIncidents() {
    log.info("Running escalation check at {}", LocalDateTime.now());

    // Find incidents that have breached SLA
    List<Incident> breachedIncidents = incidentRepository.findBreachedIncidents(
        List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING),
        LocalDateTime.now());

    if (breachedIncidents.isEmpty()) {
      log.info("No SLA breaches detected");
      return;
    }

    log.info("Found {} incidents with SLA breach", breachedIncidents.size());

    // Process escalations in parallel using CompletableFuture (multithreading)
    List<CompletableFuture<Void>> futures = breachedIncidents.stream()
        .map(incident -> CompletableFuture.runAsync(() -> escalateIncident(incident), executorService))
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    log.info("Escalation batch completed");
  }

  private void escalateIncident(Incident incident) {
    try {
      // Mark as SLA breached
      if (!incident.isSlaBreached()) {
        incident.setSlaBreached(true);
      }

      int nextLevel = incident.getEscalationLevel() + 1;

      if (incident.getTeam() != null) {
        Optional<EscalationPolicy> policy = escalationPolicyRepository
            .findByTeamIdAndLevel(incident.getTeam().getId(), nextLevel);

        if (policy.isPresent()) {
          EscalationPolicy ep = policy.get();
          incident.setAssignee(ep.getTargetUser());
          incident.setEscalationLevel(nextLevel);

          // Add timeline entry
          IncidentTimeline entry = IncidentTimeline.builder()
              .incident(incident)
              .action("ESCALATED")
              .message("Auto-escalated to level " + nextLevel + " - assigned to " + ep.getTargetUser().getFullName())
              .build();
          timelineRepository.save(entry);

          log.info("Incident {} escalated to level {} -> {}", incident.getId(), nextLevel,
              ep.getTargetUser().getFullName());
        }
      }

      incidentRepository.save(incident);

      // Notify via WebSocket
      notificationService.notifySlaBreached(
          IncidentDto.Response.builder()
              .id(incident.getId())
              .title(incident.getTitle())
              .severity(incident.getSeverity())
              .status(incident.getStatus())
              .slaBreached(true)
              .escalationLevel(incident.getEscalationLevel())
              .build());
    } catch (Exception e) {
      log.error("Error escalating incident {}: {}", incident.getId(), e.getMessage());
    }
  }
}
