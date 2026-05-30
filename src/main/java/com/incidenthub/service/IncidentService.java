package com.incidenthub.service;

import com.incidenthub.config.IncidentMetrics;
import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.*;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.repository.*;
import com.incidenthub.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentService {

  private final IncidentRepository incidentRepository;
  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final IncidentTimelineRepository timelineRepository;
  private final WebSocketNotificationService notificationService;
  private final AuditService auditService;
  private final IncidentMetrics incidentMetrics;
  private final EmailNotificationService emailNotificationService;

  @Transactional
  @CacheEvict(value = { "dashboardStats", "analytics" }, allEntries = true)
  public IncidentDto.Response createIncident(IncidentDto.CreateRequest request, String reporterUsername) {
    User reporter = userRepository.findByUsername(reporterUsername)
        .orElseThrow(() -> new RuntimeException("Reporter not found"));

    Incident incident = Incident.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .severity(request.getSeverity())
        .service(request.getService())
        .reporter(reporter)
        .build();

    if (request.getAssigneeId() != null) {
      User assignee = userRepository.findById(request.getAssigneeId())
          .orElseThrow(() -> new RuntimeException("Assignee not found"));
      incident.setAssignee(assignee);
    }

    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(() -> new RuntimeException("Team not found"));
      incident.setTeam(team);
    }

    incident = incidentRepository.save(incident);

    // Add timeline entry
    addTimelineEntry(incident, reporter, "CREATED", "Incident created with severity: " + request.getSeverity());

    // Notify via WebSocket
    IncidentDto.Response response = mapToResponse(incident);
    notificationService.notifyIncidentCreated(response);

    // Send email notification to assignee
    if (incident.getAssignee() != null) {
      emailNotificationService.sendIncidentCreatedNotification(incident, incident.getAssignee());
    }

    // Metrics + Audit
    incidentMetrics.recordIncidentCreated();
    auditService.logAction("INCIDENT", incident.getId(), "CREATE",
        reporterUsername, null, incident.getTitle(), "severity=" + request.getSeverity());

    return response;
  }

  @Transactional
  public IncidentDto.Response acknowledgeIncident(Long incidentId, String username) {
    Incident incident = incidentRepository.findById(incidentId)
        .orElseThrow(() -> new RuntimeException("Incident not found"));
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (incident.getStatus() != IncidentStatus.OPEN) {
      throw new RuntimeException("Incident can only be acknowledged from OPEN status");
    }

    incident.setStatus(IncidentStatus.ACKNOWLEDGED);
    incident.setAcknowledgedAt(LocalDateTime.now());
    if (incident.getAssignee() == null) {
      incident.setAssignee(user);
    }

    incident = incidentRepository.save(incident);
    addTimelineEntry(incident, user, "ACKNOWLEDGED", "Incident acknowledged by " + user.getFullName());

    IncidentDto.Response response = mapToResponse(incident);
    notificationService.notifyIncidentUpdated(response);
    return response;
  }

  @Transactional
  @CacheEvict(value = { "dashboardStats", "analytics" }, allEntries = true)
  public IncidentDto.Response updateIncidentStatus(Long incidentId, IncidentStatus newStatus, String username) {
    Incident incident = incidentRepository.findById(incidentId)
        .orElseThrow(() -> new RuntimeException("Incident not found"));
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    IncidentStatus oldStatus = incident.getStatus();
    validateStatusTransition(incident.getStatus(), newStatus);

    incident.setStatus(newStatus);
    if (newStatus == IncidentStatus.RESOLVED) {
      incident.setResolvedAt(LocalDateTime.now());
      incidentMetrics.recordIncidentResolved(Duration.between(incident.getCreatedAt(), LocalDateTime.now()));
      // Send resolved notification to reporter
      emailNotificationService.sendIncidentResolvedNotification(incident, incident.getReporter());
    } else if (newStatus == IncidentStatus.CLOSED) {
      incident.setClosedAt(LocalDateTime.now());
    }

    incident = incidentRepository.save(incident);
    addTimelineEntry(incident, user, newStatus.name(), "Status changed to " + newStatus);

    auditService.logAction("INCIDENT", incidentId, "STATUS_CHANGE",
        username, oldStatus.name(), newStatus.name(), "status_transition");

    IncidentDto.Response response = mapToResponse(incident);
    notificationService.notifyIncidentUpdated(response);
    return response;
  }

  @Transactional
  public IncidentDto.Response updateIncident(Long incidentId, IncidentDto.UpdateRequest request, String username) {
    Incident incident = incidentRepository.findById(incidentId)
        .orElseThrow(() -> new RuntimeException("Incident not found"));
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (request.getTitle() != null)
      incident.setTitle(request.getTitle());
    if (request.getDescription() != null)
      incident.setDescription(request.getDescription());
    if (request.getSeverity() != null) {
      incident.setSeverity(request.getSeverity());
      incident.calculateSlaDeadline();
    }
    if (request.getService() != null)
      incident.setService(request.getService());
    if (request.getAssigneeId() != null) {
      User assignee = userRepository.findById(request.getAssigneeId())
          .orElseThrow(() -> new RuntimeException("Assignee not found"));
      incident.setAssignee(assignee);
      addTimelineEntry(incident, user, "REASSIGNED", "Reassigned to " + assignee.getFullName());
    }
    if (request.getStatus() != null) {
      validateStatusTransition(incident.getStatus(), request.getStatus());
      incident.setStatus(request.getStatus());
    }

    incident = incidentRepository.save(incident);
    addTimelineEntry(incident, user, "UPDATED", "Incident updated");

    IncidentDto.Response response = mapToResponse(incident);
    notificationService.notifyIncidentUpdated(response);
    return response;
  }

  @Transactional
  public IncidentDto.Response addNote(Long incidentId, String message, String username) {
    Incident incident = incidentRepository.findById(incidentId)
        .orElseThrow(() -> new RuntimeException("Incident not found"));
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    addTimelineEntry(incident, user, "NOTE", message);

    return mapToResponse(incident);
  }

  @Transactional
  public void deleteIncident(Long incidentId, String username) {
    Incident incident = incidentRepository.findById(incidentId)
        .orElseThrow(() -> new RuntimeException("Incident not found"));
    timelineRepository.deleteByIncidentId(incidentId);
    incidentRepository.delete(incident);
  }

  @Transactional(readOnly = true)
  public IncidentDto.Response getIncident(Long id) {
    Incident incident = incidentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Incident not found"));
    return mapToResponse(incident);
  }

  @Transactional(readOnly = true)
  public Page<IncidentDto.Response> getAllIncidents(Pageable pageable) {
    return incidentRepository.findAll(pageable).map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public Page<IncidentDto.Response> getIncidentsByStatus(IncidentStatus status, Pageable pageable) {
    return incidentRepository.findByStatus(status, pageable).map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public Page<IncidentDto.Response> getIncidentsByAssignee(Long assigneeId, Pageable pageable) {
    return incidentRepository.findByAssigneeId(assigneeId, pageable).map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public Page<IncidentDto.Response> getIncidentsByTeam(Long teamId, Pageable pageable) {
    return incidentRepository.findByTeamId(teamId, pageable).map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "dashboardStats")
  public IncidentDto.DashboardStats getDashboardStats() {
    long open = incidentRepository.countByStatus(IncidentStatus.OPEN);
    long ack = incidentRepository.countByStatus(IncidentStatus.ACKNOWLEDGED);
    long investigating = incidentRepository.countByStatus(IncidentStatus.INVESTIGATING);
    long resolved = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
    long closed = incidentRepository.countByStatus(IncidentStatus.CLOSED);

    List<Object[]> severityCounts = incidentRepository.countBySeverity();
    List<IncidentDto.SeverityCount> severityCountList = severityCounts.stream()
        .map(row -> IncidentDto.SeverityCount.builder()
            .severity(row[0].toString())
            .count((Long) row[1])
            .build())
        .collect(Collectors.toList());

    List<Incident> breached = incidentRepository.findBreachedIncidents(
        List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING),
        LocalDateTime.now());

    return IncidentDto.DashboardStats.builder()
        .totalOpen(open)
        .totalAcknowledged(ack)
        .totalInvestigating(investigating)
        .totalResolved(resolved)
        .totalClosed(closed)
        .slaBreachedCount(breached.size())
        .severityCounts(severityCountList)
        .build();
  }

  private void validateStatusTransition(IncidentStatus current, IncidentStatus next) {
    boolean valid = switch (current) {
      case OPEN -> next == IncidentStatus.ACKNOWLEDGED || next == IncidentStatus.INVESTIGATING;
      case ACKNOWLEDGED -> next == IncidentStatus.INVESTIGATING || next == IncidentStatus.RESOLVED;
      case INVESTIGATING -> next == IncidentStatus.RESOLVED;
      case RESOLVED -> next == IncidentStatus.CLOSED || next == IncidentStatus.INVESTIGATING; // reopen
      case CLOSED -> false;
    };
    if (!valid) {
      throw new RuntimeException("Invalid status transition from " + current + " to " + next);
    }
  }

  private void addTimelineEntry(Incident incident, User user, String action, String message) {
    IncidentTimeline entry = IncidentTimeline.builder()
        .incident(incident)
        .performedBy(user)
        .action(action)
        .message(message)
        .build();
    timelineRepository.save(entry);
  }

  private IncidentDto.Response mapToResponse(Incident incident) {
    List<IncidentDto.TimelineResponse> timeline = timelineRepository
        .findByIncidentIdOrderByCreatedAtAsc(incident.getId())
        .stream()
        .map(t -> IncidentDto.TimelineResponse.builder()
            .id(t.getId())
            .action(t.getAction())
            .message(t.getMessage())
            .performedByName(t.getPerformedBy() != null ? t.getPerformedBy().getFullName() : "System")
            .createdAt(t.getCreatedAt())
            .build())
        .collect(Collectors.toList());

    return IncidentDto.Response.builder()
        .id(incident.getId())
        .title(incident.getTitle())
        .description(incident.getDescription())
        .severity(incident.getSeverity())
        .status(incident.getStatus())
        .assigneeName(incident.getAssignee() != null ? incident.getAssignee().getFullName() : null)
        .assigneeId(incident.getAssignee() != null ? incident.getAssignee().getId() : null)
        .reporterName(incident.getReporter().getFullName())
        .reporterId(incident.getReporter().getId())
        .teamName(incident.getTeam() != null ? incident.getTeam().getName() : null)
        .teamId(incident.getTeam() != null ? incident.getTeam().getId() : null)
        .service(incident.getService())
        .createdAt(incident.getCreatedAt())
        .acknowledgedAt(incident.getAcknowledgedAt())
        .resolvedAt(incident.getResolvedAt())
        .closedAt(incident.getClosedAt())
        .slaDeadline(incident.getSlaDeadline())
        .slaBreached(incident.isSlaBreached())
        .escalationLevel(incident.getEscalationLevel())
        .timeline(timeline)
        .build();
  }
}
