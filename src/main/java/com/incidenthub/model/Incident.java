package com.incidenthub.model;

import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.model.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

  @Id
  @GeneratedValue(generator = "snowflake")
  @GenericGenerator(name = "snowflake", type = com.incidenthub.util.SnowflakeIdGenerator.class)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Severity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IncidentStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  private String service; // affected service name

  private LocalDateTime createdAt;
  private LocalDateTime acknowledgedAt;
  private LocalDateTime resolvedAt;
  private LocalDateTime closedAt;
  private LocalDateTime slaDeadline;

  private boolean slaBreached;
  private int escalationLevel;

  @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @OrderBy("createdAt ASC")
  private List<IncidentTimeline> timeline = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    status = IncidentStatus.OPEN;
    escalationLevel = 0;
    slaBreached = false;
    calculateSlaDeadline();
  }

  public void calculateSlaDeadline() {
    if (createdAt == null)
      createdAt = LocalDateTime.now();
    switch (severity) {
      case CRITICAL -> slaDeadline = createdAt.plusMinutes(15);
      case HIGH -> slaDeadline = createdAt.plusMinutes(30);
      case MEDIUM -> slaDeadline = createdAt.plusHours(2);
      case LOW -> slaDeadline = createdAt.plusHours(24);
    }
  }
}
