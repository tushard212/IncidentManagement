package com.incidenthub.service;

import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.Incident;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private final IncidentRepository incidentRepository;

  /**
   * MTTR - Mean Time to Resolve (in minutes)
   * Cached in Redis for 5 minutes
   */
  @Cacheable(value = "analytics", key = "'mttr_' + #days")
  @Transactional(readOnly = true)
  public IncidentDto.AnalyticsResponse getAnalytics(int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    List<Incident> allIncidents = incidentRepository.findByCreatedAtAfter(since);

    // MTTR: Average time from creation to resolution
    List<Incident> resolved = allIncidents.stream()
        .filter(i -> i.getResolvedAt() != null)
        .collect(Collectors.toList());

    double mttrMinutes = resolved.stream()
        .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes())
        .average().orElse(0);

    // MTTA: Mean Time to Acknowledge
    List<Incident> acknowledged = allIncidents.stream()
        .filter(i -> i.getAcknowledgedAt() != null)
        .collect(Collectors.toList());

    double mttaMinutes = acknowledged.stream()
        .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getAcknowledgedAt()).toMinutes())
        .average().orElse(0);

    // Incidents per day
    Map<String, Long> incidentsPerDay = allIncidents.stream()
        .collect(Collectors.groupingBy(
            i -> i.getCreatedAt().toLocalDate().toString(),
            TreeMap::new,
            Collectors.counting()));

    // Incidents by severity
    Map<String, Long> bySeverity = allIncidents.stream()
        .collect(Collectors.groupingBy(
            i -> i.getSeverity().name(),
            Collectors.counting()));

    // Incidents by status
    Map<String, Long> byStatus = allIncidents.stream()
        .collect(Collectors.groupingBy(
            i -> i.getStatus().name(),
            Collectors.counting()));

    // SLA compliance rate
    long totalWithSla = allIncidents.stream()
        .filter(i -> i.getSlaDeadline() != null)
        .count();
    long breached = allIncidents.stream()
        .filter(i -> i.isSlaBreached())
        .count();
    double slaComplianceRate = totalWithSla > 0 ? ((double) (totalWithSla - breached) / totalWithSla) * 100 : 100;

    // Resolution by severity (avg minutes)
    Map<String, Double> resolutionBySeverity = resolved.stream()
        .collect(Collectors.groupingBy(
            i -> i.getSeverity().name(),
            Collectors.averagingLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes())));

    return IncidentDto.AnalyticsResponse.builder()
        .mttrMinutes(Math.round(mttrMinutes * 100.0) / 100.0)
        .mttaMinutes(Math.round(mttaMinutes * 100.0) / 100.0)
        .totalIncidents(allIncidents.size())
        .totalResolved(resolved.size())
        .slaComplianceRate(Math.round(slaComplianceRate * 100.0) / 100.0)
        .incidentsPerDay(incidentsPerDay)
        .bySeverity(bySeverity)
        .byStatus(byStatus)
        .resolutionBySeverity(resolutionBySeverity)
        .periodDays(days)
        .build();
  }
}
