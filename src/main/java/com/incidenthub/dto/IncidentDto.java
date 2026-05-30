package com.incidenthub.dto;

import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.model.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class IncidentDto {

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Severity severity;

    private Long assigneeId;
    private Long teamId;
    private String service;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UpdateRequest {
    private String title;
    private String description;
    private Severity severity;
    private IncidentStatus status;
    private Long assigneeId;
    private String service;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long id;
    private String title;
    private String description;
    private Severity severity;
    private IncidentStatus status;
    private String assigneeName;
    private Long assigneeId;
    private String reporterName;
    private Long reporterId;
    private String teamName;
    private Long teamId;
    private String service;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime slaDeadline;
    private boolean slaBreached;
    private int escalationLevel;
    private List<TimelineResponse> timeline;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TimelineResponse {
    private Long id;
    private String action;
    private String message;
    private String performedByName;
    private LocalDateTime createdAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AddNoteRequest {
    @NotBlank
    private String message;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DashboardStats {
    private long totalOpen;
    private long totalAcknowledged;
    private long totalInvestigating;
    private long totalResolved;
    private long totalClosed;
    private long slaBreachedCount;
    private List<SeverityCount> severityCounts;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SeverityCount {
    private String severity;
    private long count;
  }
}
