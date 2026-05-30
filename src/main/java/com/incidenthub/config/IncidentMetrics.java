package com.incidenthub.config;

import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.repository.IncidentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom business metrics exposed via Micrometer/Prometheus.
 * Demonstrates: Observable metrics, gauges, counters, timers.
 */
@Slf4j
@Component
public class IncidentMetrics {

  private final MeterRegistry meterRegistry;
  private final IncidentRepository incidentRepository;

  private final AtomicLong openIncidents = new AtomicLong(0);
  private final AtomicLong criticalIncidents = new AtomicLong(0);
  private final AtomicLong slaBreachedCount = new AtomicLong(0);
  private final AtomicLong avgResolutionMinutes = new AtomicLong(0);

  private final Counter incidentCreatedCounter;
  private final Counter incidentResolvedCounter;
  private final Counter slaBreachCounter;
  private final Timer incidentResolutionTimer;

  public IncidentMetrics(MeterRegistry meterRegistry, IncidentRepository incidentRepository) {
    this.meterRegistry = meterRegistry;
    this.incidentRepository = incidentRepository;

    // Gauges - current state metrics
    Gauge.builder("incidents.open.total", openIncidents, AtomicLong::get)
        .description("Current number of open incidents")
        .register(meterRegistry);

    Gauge.builder("incidents.critical.total", criticalIncidents, AtomicLong::get)
        .description("Current number of critical severity incidents (non-closed)")
        .register(meterRegistry);

    Gauge.builder("incidents.sla_breached.total", slaBreachedCount, AtomicLong::get)
        .description("Current number of SLA-breached incidents")
        .register(meterRegistry);

    Gauge.builder("incidents.resolution.avg_minutes", avgResolutionMinutes, AtomicLong::get)
        .description("Average resolution time in minutes")
        .register(meterRegistry);

    // Counters - cumulative event metrics
    incidentCreatedCounter = Counter.builder("incidents.created.total")
        .description("Total incidents created")
        .register(meterRegistry);

    incidentResolvedCounter = Counter.builder("incidents.resolved.total")
        .description("Total incidents resolved")
        .register(meterRegistry);

    slaBreachCounter = Counter.builder("incidents.sla_breach.events")
        .description("Total SLA breach events")
        .register(meterRegistry);

    // Timer - resolution time distribution
    incidentResolutionTimer = Timer.builder("incidents.resolution.time")
        .description("Incident resolution time distribution")
        .register(meterRegistry);
  }

  public void recordIncidentCreated() {
    incidentCreatedCounter.increment();
  }

  public void recordIncidentResolved(Duration resolutionTime) {
    incidentResolvedCounter.increment();
    incidentResolutionTimer.record(resolutionTime);
  }

  public void recordSlaBreached() {
    slaBreachCounter.increment();
  }

  /**
   * Refresh gauge values every 30 seconds
   */
  @Scheduled(fixedRate = 30000)
  public void refreshMetrics() {
    try {
      openIncidents.set(incidentRepository.countByStatus(IncidentStatus.OPEN));
      criticalIncidents.set(incidentRepository.countByStatusNotAndSeverity(
          IncidentStatus.CLOSED, "CRITICAL"));
      slaBreachedCount.set(incidentRepository.findBreachedIncidents(
          java.util.List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING),
          LocalDateTime.now()).size());

      // Calculate average resolution time
      Double avgMinutes = incidentRepository.findAverageResolutionTimeMinutes();
      avgResolutionMinutes.set(avgMinutes != null ? avgMinutes.longValue() : 0);
    } catch (Exception e) {
      log.debug("Failed to refresh metrics: {}", e.getMessage());
    }
  }
}
